import test from "node:test";
import assert from "node:assert/strict";
import { readFile, readdir } from "node:fs/promises";
import { Collection, EmbedBuilder, MessageFlags, PermissionFlagsBits, PermissionsBitField } from "discord.js";
import { EMBED_COLOR, embedMessage, sendEmbed, replyEmbed, editEmbedReply, editEmbedMessage } from "../src/embeds.js";
import { makeModerationEmbed, replyEphemeral } from "../src/discord-utils.js";
import { handleTicketAnnouncement } from "../src/tickets.js";
import { handleMinecraftText } from "../src/invsee.js";
import { handleMemberJoin } from "../src/server.js";

function assertWhite(payload) {
  assert.ok(payload.embeds.length > 0);
  for (const embed of payload.embeds) assert.equal(embed.toJSON().color, 0xffffff);
}

test("texto normal vira embed branco, sem texto solto ou menções automáticas", () => {
  const result = embedMessage("Mensagem **personalizada** <@123>");
  assertWhite(result);
  assert.equal(result.content, "");
  assert.equal(result.embeds[0].toJSON().description, "Mensagem **personalizada** <@123>");
  assert.deepEqual(result.allowedMentions, { parse: [] });
});

test("padronização preserva campos, imagens, anexos e botões sem alterar o original", () => {
  const original = new EmbedBuilder().setColor(0x5865f2).setTitle("Título").setDescription("Texto")
    .setImage("https://example.com/image.png").addFields({ name: "Campo", value: "Valor" });
  const files = [{ attachment: Buffer.from("anexo"), name: "anexo.txt" }];
  const components = [{ type: 1, components: [] }];
  const result = embedMessage({ embeds: [original], files, components });
  assertWhite(result);
  assert.equal(original.toJSON().color, 0x5865f2);
  assert.equal(result.embeds[0].toJSON().title, "Título");
  assert.equal(result.embeds[0].toJSON().image.url, "https://example.com/image.png");
  assert.equal(result.embeds[0].toJSON().fields[0].value, "Valor");
  assert.equal(result.files, files);
  assert.equal(result.components, components);
  assertWhite(embedMessage({ files }));
});

test("envio, resposta e edição usam embeds brancos e limpam o conteúdo antigo", async () => {
  const calls = [];
  const target = Object.fromEntries(["send", "reply", "editReply", "edit"].map((name) => [name, async (payload) => { calls.push(payload); return "message"; }]));
  for (const operation of [sendEmbed, replyEmbed, editEmbedReply, editEmbedMessage]) {
    assert.equal(await operation(target, "Mensagem"), "message");
  }
  for (const payload of calls) { assertWhite(payload); assert.equal(payload.content, ""); }
});

test("respostas efêmeras continuam privadas e em embed nos três estados da interação", async () => {
  for (const [deferred, replied, method] of [[false, false, "reply"], [true, false, "editReply"], [false, true, "followUp"]]) {
    let result;
    await replyEphemeral({ deferred, replied, [method]: async (payload) => { result = payload; } }, "Aviso privado");
    assertWhite(result);
    assert.equal(result.embeds[0].toJSON().description, "Aviso privado");
    if (method !== "editReply") assert.equal(result.flags, MessageFlags.Ephemeral);
  }
});

test("anúncio usa embed branco e mantém somente @everyone fora do embed para notificar", async () => {
  const sent = [];
  let confirmation;
  const admin = { permissions: new PermissionsBitField(PermissionFlagsBits.Administrator), roles: { cache: new Collection() } };
  await handleTicketAnnouncement({ member: admin, guildId: "guild",
    guild: { members: { me: admin }, channels: { fetch: async () => ({ isTextBased: () => true, send: async (payload) => sent.push(payload) }) } },
    options: { getString: () => "Aviso **importante** @here" }, deferReply: async () => {}, editReply: async (payload) => { confirmation = payload; } },
  { config: { ownerRoleId: "owner" }, store: { getOpenTickets: async () => [{ channelId: "a", number: 1 }, { channelId: "b", number: 2 }] } });
  assert.equal(sent.length, 2);
  for (const payload of sent) {
    assertWhite(payload);
    assert.equal(payload.content, "@everyone");
    assert.match(payload.embeds[0].toJSON().description, /Aviso \*\*importante\*\*/);
    assert.ok(payload.embeds[0].toJSON().description.includes("@\u200bhere"));
    assert.deepEqual(payload.allowedMentions, { parse: ["everyone"] });
    assert.equal(payload.mentionContent, undefined);
  }
  assertWhite(confirmation);
});

test("notificações de participantes permanecem separadas do corpo em embed", () => {
  const result = embedMessage({ mentionContent: "<@123> <@&456>", embeds: [new EmbedBuilder().setTitle("Ticket")],
    allowedMentions: { users: ["123"], roles: ["456"] } });
  assertWhite(result);
  assert.equal(result.content, "<@123> <@&456>");
  assert.deepEqual(result.allowedMentions, { users: ["123"], roles: ["456"] });
});

test("erros e instruções do comando .use também são embeds brancos", async () => {
  const replies = [];
  const message = { inGuild: () => true, author: { bot: false }, channelId: "ticket", member: {},
    channel: { permissionsFor: () => new PermissionsBitField(PermissionFlagsBits.Administrator) },
    reply: async (payload) => replies.push(payload), content: ".use" };
  const context = { store: { getTicketByChannel: async () => ({ status: "open" }) },
    minecraft: { run: async () => { throw new Error("Nenhuma conta ativa"); } } };
  assert.equal(await handleMinecraftText(message, context), true);
  message.content = ".use /clan roster TAG";
  assert.equal(await handleMinecraftText(message, context), true);
  assert.equal(replies.length, 2);
  for (const payload of replies) { assertWhite(payload); assert.equal(payload.content, ""); }
});

test("entrada de membros e mod-log seguem o branco mesmo para filtros", async () => {
  let result;
  await handleMemberJoin({ id: "user", guild: { id: "guild", channels: { fetch: async () => ({ send: async (payload) => { result = payload; } }) } },
    user: { tag: "Membro", displayAvatarURL: () => "https://example.com/avatar.png", createdTimestamp: 0 } },
  { config: { guildId: "guild", channels: { admin: "admin" } } });
  assertWhite(result);
  for (const action of ["Filtro", "Ban"]) {
    const embed = makeModerationEmbed({ name: "Guild" }, { action, number: 1, moderatorId: "admin", targetId: "user", reason: "Motivo", metadata: {}, createdAt: new Date().toISOString() });
    assert.equal(embed.toJSON().color, EMBED_COLOR);
  }
});

test("não há envios diretos nem cores alternativas fora da camada padronizada", async () => {
  const source = new URL("../src/", import.meta.url);
  for (const file of (await readdir(source)).filter((file) => file.endsWith(".js"))) {
    const text = await readFile(new URL(file, source), "utf8");
    assert.doesNotMatch(text, /\.setColor\((?!EMBED_COLOR\))/);
    if (["embeds.js", "discord-utils.js"].includes(file)) continue;
    assert.doesNotMatch(text, /\.(?:send|reply|editReply|followUp)\(/, file);
    assert.doesNotMatch(text, /(?:existing|tracker\.message)\.edit\(/, file);
  }
});
