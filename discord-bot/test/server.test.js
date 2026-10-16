import test from "node:test";
import assert from "node:assert/strict";
import { Collection, ChannelType, PermissionFlagsBits, PermissionsBitField } from "discord.js";
import { allowedCommandLocation, handlePublication, handleVerification, setupServer } from "../src/server.js";
import { commandDefinitions } from "../src/commands.js";
import { handleTicketUserSelect, ticketPanelPayload, enableTicketCommands } from "../src/tickets.js";
import { EMBED_COLOR } from "../src/embeds.js";

const config = { ownerRoleId: "owner-role", verifiedRoleId: "member-role", staffRoleIds: ["helper-role", "owner-role"],
  channels: { commands: "commands", home: "home", announcement: "announcements", features: "features", media: "media" } };
const member = (admin = false) => ({ id: "user", roles: { cache: new Collection() },
  permissions: new PermissionsBitField(admin ? PermissionFlagsBits.Administrator : 0n) });

test("restringe comandos gerais e mantém exceções somente onde são necessárias", () => {
  assert.equal(allowedCommandLocation("anuncio", null, "commands", config), true);
  assert.equal(allowedCommandLocation("anuncio", null, "ticket", config, { status: "open" }), false);
  assert.equal(allowedCommandLocation("mod", "ban", "chat", config), false);
  assert.equal(allowedCommandLocation("mod", "limpar", "chat", config), true);
  assert.equal(allowedCommandLocation("mod", "historico", "chat", config), true);
  assert.equal(allowedCommandLocation("botinvsee", null, "commands", config), false);
  assert.equal(allowedCommandLocation("invsee", null, "ticket", config, { status: "open" }), true);
  assert.equal(allowedCommandLocation("invsee", null, "ticket", config, { status: "closing" }), false);
});

test("a reação certa na mensagem certa dá somente o cargo de membros", async () => {
  const grants = [];
  const guild = { id: "guild", members: { fetch: async () => ({ roles: { cache: new Collection(), add: async (id) => grants.push(id) } }) } };
  const context = { config, store: { getGuildSettings: async () => ({ verificationMessageId: "verify" }) } };
  const reaction = { message: { channelId: "home", id: "verify", guild }, emoji: { name: "✅" }, partial: false };
  await handleVerification({ ...reaction, message: { ...reaction.message, id: "unrelated" } }, { id: "user", bot: false }, context);
  await handleVerification(reaction, { id: "bot", bot: true }, context);
  assert.deepEqual(grants, []);
  await handleVerification(reaction, { id: "user", bot: false }, context);
  assert.deepEqual(grants, ["member-role"]);
});

test("anúncios/features publicam embeds no destino certo e só para admins", async () => {
  const published = [];
  const interaction = { member: member(true), commandName: "features", deferReply: async () => {}, editReply: async () => {},
    guild: { channels: { fetch: async (id) => ({ send: async (payload) => published.push({ id, payload }) }) } },
    options: { getString: (name) => name === "mensagem" ? "Novos recursos **hoje**" : "Atualização", getAttachment: () => null } };
  await handlePublication(interaction, { config });
  assert.equal(published[0].id, "features");
  assert.equal(published[0].payload.embeds[0].toJSON().description, "Novos recursos **hoje**");
  assert.equal(published[0].payload.embeds[0].toJSON().color, EMBED_COLOR);
  assert.deepEqual(published[0].payload.allowedMentions.parse, []);
  let denied = false;
  await handlePublication({ ...interaction, member: member(), reply: async () => { denied = true; } }, { config });
  assert.equal(denied, true);
  assert.equal(published.length, 1);
});

test("vídeo preserva o link e o convite para curtir e se inscrever", async () => {
  let result;
  await handlePublication({ member: member(true), commandName: "video", deferReply: async () => {}, editReply: async () => {},
    guild: { channels: { fetch: async (id) => { assert.equal(id, "media"); return { send: async (payload) => { result = payload; } }; } } },
    options: { getString: () => "https://youtu.be/example" } }, { config });
  assert.match(result.embeds[0].toJSON().description, /https:\/\/youtu.be\/example/);
  assert.match(result.embeds[0].toJSON().description, /like e inscreva-se/);
  assert.equal(result.embeds[0].toJSON().url, "https://youtu.be/example");
  assert.equal(result.embeds[0].toJSON().color, EMBED_COLOR);
  assert.equal(result.content, "");
});

test("clan seleciona vários membros, mantém o autor e persiste os convidados", async () => {
  const permissions = [];
  const stored = [];
  const creator = member();
  const users = new Collection([["guest1", { id: "guest1", bot: false }], ["guest2", { id: "guest2", bot: false }], ["bot", { id: "bot", bot: true }]]);
  await handleTicketUserSelect({ customId: "ticket:clan-invite:ticket-channel", guildId: "guild", member: creator, users,
    deferReply: async () => {}, editReply: async () => {}, guild: {
      members: { fetch: async () => ({}) },
      channels: { fetch: async () => ({ permissionOverwrites: { edit: async (id, permission) => {
        assert.equal(permission.UseApplicationCommands, true);
        permissions.push(id);
      } } }) },
    } }, { config, store: { getTicketByChannel: async () => ({ id: "ticket", type: "clan", status: "open", ownerId: "user" }),
      addParticipant: async (_, id) => stored.push(id) } });
  assert.deepEqual(permissions, ["guest1", "guest2"]);
  assert.deepEqual(stored, permissions);
});

test("painel oferece Buy solo/clan e comandos serializam sem duplicatas", () => {
  const payload = ticketPanelPayload();
  assert.equal(payload.embeds[0].toJSON().color, EMBED_COLOR);
  assert.deepEqual(payload.components[0].toJSON().components.map((button) => button.label), ["Buy (solo)", "Buy (clan)"]);
  assert.equal(new Set(commandDefinitions.map((command) => command.name)).size, commandDefinitions.length);
  for (const name of ["botinvsee", "invsee-player", "invseeauto", "anuncio", "features", "video", "anuncioticket"]) {
    assert.ok(commandDefinitions.some((command) => command.name === name));
  }
});

test("configuração reutiliza painéis, sincroniza reação offline e mantém #admin privado", async () => {
  const setupConfig = { ...config, channels: { ...config.channels, tickets: "tickets", admin: "admin" } };
  const settings = { filterWords: [], verificationMessageId: "verify", ticketPanelMessageId: "panel", memberSnapshotSent: true };
  const grants = [];
  const edits = [];
  let newMessages = 0;
  const returningMember = { id: "returning", user: { tag: "returning" }, roles: { cache: new Collection(), add: async (id) => grants.push(id) } };
  const verification = { id: "verify", edit: async () => verification, react: async () => {},
    reactions: { cache: new Collection([["✅", { emoji: { name: "✅" }, users: { fetch: async () => new Collection([["returning", { id: "returning", bot: false }]]) } }]]) } };
  const panel = { id: "panel", edit: async () => panel };
  const channels = new Collection(Object.values(setupConfig.channels).map((id) => [id, {
    id, type: ChannelType.GuildText, permissionsFor: () => new PermissionsBitField(PermissionFlagsBits.ViewChannel),
    permissionOverwrites: { edit: async (roleId, permission) => edits.push({ id, roleId, permission }) },
    send: async () => { newMessages++; return panel; },
    messages: { fetch: async () => id === "home" ? verification : panel },
  }]));
  const guild = { id: "guild", roles: { everyone: { id: "everyone" }, fetch: async () => new Collection([
    ["member-role", { id: "member-role", managed: false }], ["helper-role", {}], ["owner-role", {}],
  ]) }, members: { me: { id: "bot", permissions: new PermissionsBitField(PermissionFlagsBits.Administrator),
    roles: { highest: { comparePositionTo: () => 1 } } }, fetch: async () => returningMember },
  channels: { fetch: async (id) => id ? channels.get(id) : channels } };
  await setupServer(guild, { config: setupConfig, store: { getOpenTickets: async () => [], getGuildSettings: async () => settings,
    updateGuildSettings: async (_, patch) => Object.assign(settings, patch) } });
  assert.equal(newMessages, 0);
  assert.deepEqual(grants, ["member-role"]);
  assert.equal(edits.find((entry) => entry.id === "home" && entry.roleId === "everyone").permission.ViewChannel, true);
  assert.equal(edits.find((entry) => entry.id === "commands" && entry.roleId === "everyone").permission.ViewChannel, false);
  assert.equal(edits.find((entry) => entry.id === "admin" && entry.roleId === "member-role").permission.ViewChannel, false);
  assert.equal(edits.find((entry) => entry.id === "admin" && entry.roleId === "owner-role").permission.ViewChannel, true);
});

test("tickets existentes liberam comandos preservando quem pode ver e escrever", async () => {
  const edits = [];
  const channel = { type: ChannelType.GuildText, permissionOverwrites: {
    cache: new Collection([
      ["guest", { id: "guest", type: 1, allow: new PermissionsBitField([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages]), deny: new PermissionsBitField(PermissionFlagsBits.UseApplicationCommands) }],
      ["blocked", { id: "blocked", type: 1, allow: new PermissionsBitField(PermissionFlagsBits.ViewChannel), deny: new PermissionsBitField(PermissionFlagsBits.SendMessages) }],
    ]), edit: async (id, permissions, options) => {
      assert.equal(options.type, id === "everyone" ? 0 : 1);
      edits.push({ id, permissions });
    },
  } };
  const guild = { id: "guild", roles: { everyone: { id: "everyone" } }, channels: { fetch: async () => channel } };
  assert.equal(await enableTicketCommands(guild, { getOpenTickets: async () => [{ channelId: "ticket" }] }), 1);
  assert.deepEqual(edits, [
    { id: "everyone", permissions: { UseApplicationCommands: true } },
    { id: "guest", permissions: { UseApplicationCommands: true } },
  ]);
});

test("comandos Minecraft ficam visíveis e a autorização de início é validada pelo bot", () => {
  for (const name of ["botinvsee", "botinvsee-stop", "syncunknown", "invsee", "invsee-player", "invseeauto", "invseeauto-stop", "clanwarn", "reconnect"]) {
    assert.equal(commandDefinitions.find(command => command.name === name).default_member_permissions, null);
  }
});
