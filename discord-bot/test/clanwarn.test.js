import test from "node:test";
import assert from "node:assert/strict";
import { Collection, PermissionsBitField, PermissionFlagsBits } from "discord.js";
import { handleInvseeCommand } from "../src/invsee.js";
import { commandDefinitions } from "../src/commands.js";

function armor(player, sets, helmet, chestplate, leggings, boots, overrides = {}) {
  return { ok: true, player, sets, analysis: { totals: { helmet, chestplate, leggings, boots } }, ...overrides };
}

function fixture(results, warnEnabled = true) {
  const commands = [], replies = [];
  const session = { channelId: "ticket", nick: "Bot", mode: "p4free", closed: false, warnEnabled,
    api: { getClanRoster: async () => ({ ok: true, online: results.map(result => result.player), offline: [] }),
      invseeMany: async () => results,
      sendChatCommand: async command => { commands.push(command); return { ok: true }; } } };
  const context = { config: { staffRoleIds: [] },
    store: { getTicketByChannel: async () => ({ status: "open", ownerId: "another-user" }) },
    minecraft: { get: () => session, run: async (_, callback) => callback(session) } };
  const interaction = { commandName: "invsee", channelId: "ticket", guildId: "guild",
    channel: { permissionsFor: () => new PermissionsBitField([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages]) },
    member: { id: "participant", permissions: new PermissionsBitField(), roles: { cache: new Collection() } },
    options: { getString: key => ({ tag: "CLAN", estado: "on" })[key] ?? null },
    deferReply: async () => {}, editReply: async reply => replies.push(reply), reply: async reply => replies.push(reply) };
  return { context, interaction, session, commands, replies };
}

test("clanwarn anuncia o menor número de sets mesmo quando todos possuem sets completos", async () => {
  const { context, interaction, commands } = fixture([
    armor("More", 8, 9, 8, 10, 8), armor("Least", 2, 3, 2, 5, 4), armor("Middle", 4, 4, 6, 7, 5),
  ]);
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(commands, ["/. Least com menos sets: 2 set(s) - 3 capa 2 peito 5 calça 4 bota"]);
});

test("clanwarn preserva a contagem individual de armaduras incompletas", async () => {
  const { context, interaction, commands } = fixture([
    armor("Full", 5, 5, 5, 5, 5), armor("Partial", 0, 1, 0, 2, 0),
  ]);
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(commands, ["/. Partial com menos sets: 0 set(s) - 1 capa 0 peito 2 calça 0 bota"]);
});

test("clanwarn anuncia todos os empatados no mínimo, na ordem do relatório", async () => {
  const { context, interaction, commands } = fixture([
    armor("First", 1, 2, 1, 3, 4), armor("More", 6, 6, 6, 6, 6), armor("Second", 1, 1, 4, 2, 1),
  ]);
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(commands, [
    "/. First com menos sets: 1 set(s) - 2 capa 1 peito 3 calça 4 bota",
    "/. Second com menos sets: 1 set(s) - 1 capa 4 peito 2 calça 1 bota",
  ]);
});

test("clanwarn exclui jogadores offline, possíveis mortes e consultas sem resposta", async () => {
  const { context, interaction, commands } = fixture([
    { ok: false, player: "Offline", sets: 0 }, armor("DeadMaybe", 0, 0, 0, 0, 0, { dead: true }),
    armor("Alive", 2, 2, 3, 4, 5),
  ]);
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(commands, ["/. Alive com menos sets: 2 set(s) - 2 capa 3 peito 4 calça 5 bota"]);
});

test("clanwarn não inventa contagens nem envia aviso quando não há candidatos", async () => {
  for (const results of [[], [armor("DeadMaybe", 0, 0, 0, 0, 0, { dead: true })], [{ ok: true, player: "MissingCounts", sets: 1 }]]) {
    const { context, interaction, commands } = fixture(results);
    await handleInvseeCommand(interaction, context);
    assert.deepEqual(commands, []);
  }
});

test("clanwarn on/off é controlável por participantes e explica quando os avisos são enviados", async () => {
  const { context, interaction, commands, replies, session } = fixture([armor("Least", 1, 1, 2, 3, 4)], false);
  interaction.commandName = "clanwarn";
  await handleInvseeCommand(interaction, context);
  assert.equal(session.warnEnabled, true);
  assert.match(JSON.stringify(replies.at(-1)), /quem tem menos sets/);
  assert.match(JSON.stringify(replies.at(-1)), /invseeauto/);
  assert.deepEqual(commands, []);
  interaction.commandName = "invsee";
  await handleInvseeCommand(interaction, context);
  assert.equal(commands.length, 1);
  interaction.commandName = "clanwarn";
  interaction.options.getString = key => key === "estado" ? "off" : null;
  await handleInvseeCommand(interaction, context);
  assert.equal(session.warnEnabled, false);
  interaction.commandName = "invsee";
  interaction.options.getString = () => "CLAN";
  await handleInvseeCommand(interaction, context);
  assert.equal(commands.length, 1);
});

test("clanwarn informa quando a conta não consegue enviar o aviso", async () => {
  const { context, interaction, replies, session } = fixture([armor("Least", 1, 1, 1, 1, 1)]);
  session.api.sendChatCommand = async () => ({ ok: false, notReady: true });
  await handleInvseeCommand(interaction, context);
  assert.match(JSON.stringify(replies.at(-1)), /Não consegui enviar o aviso de clan/);
});

test("a descrição publicada do clanwarn indica o mínimo e as quantidades", () => {
  const command = commandDefinitions.find(command => command.name === "clanwarn");
  assert.match(command.description, /menos sets/);
  assert.match(command.description, /peças/);
});
