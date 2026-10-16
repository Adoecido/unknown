import test from "node:test";
import assert from "node:assert/strict";
import { Collection, PermissionsBitField, PermissionFlagsBits } from "discord.js";
import { UnknownSyncClient } from "../src/unknown-sync.js";
import { handleInvseeCommand, syncReport } from "../src/invsee.js";
import { commandDefinitions } from "../src/commands.js";
import { analyzeArmor } from "../vendor/bot-invsee/src/armor.js";

test("bridge usa credencial restrita e serializa publish/clear por ticket", async () => {
  const calls = [];
  const bridge = new UnknownSyncClient({ baseUrl: "https://example.invalid", botToken: "x".repeat(40) }, async (url, options) => {
    calls.push({ url, options, body: JSON.parse(options.body) });
    await new Promise((resolve) => setTimeout(resolve, 5));
    return { ok: true, json: async () => ({ ok: true }) };
  });
  await Promise.all([bridge.publish("guild", "ticket", { players: [] }), bridge.clear("guild", "ticket")]);
  assert.deepEqual(calls.map((call) => call.body.action), ["publish", "clear"]);
  assert.equal(calls[0].url, "https://example.invalid/v1/invsee/sync");
  assert.equal(calls[0].options.redirect, "error");
  assert.equal(calls[0].options.headers.Authorization, "Bearer " + "x".repeat(40));
  assert.throws(() => new UnknownSyncClient({ baseUrl: "http://example.invalid" }), /HTTPS/);
  await assert.rejects(new UnknownSyncClient().bind("x".repeat(30), "g", "c"), /não configurada/);
});

test("relatório mantém ranking estável, não inventa mortes nem inclui falhas", () => {
  assert.deepEqual(syncReport([{ ok: true, player: "Last", sets: 8 }, { ok: false, player: "Failed" },
    { ok: true, player: "DeadMaybe", sets: 0, dead: true }, { ok: true, player: "First", sets: 1 },
    { ok: true, player: "Tied", sets: 1 }], 60), {
    players: [{ name: "First", sets: 1 }, { name: "Tied", sets: 1 }, { name: "Last", sets: 8 }],
    deadPlayers: [], ttlSeconds: 180,
  });
});

test("sync preserva as oito peças de cada tipo e não substitui contagens pelo total de sets", () => {
  const slots = ["helmet", "chestplate", "leggings", "boots"].flatMap((piece) =>
    Array.from({ length: 8 }, () => ({
      name: `diamond_${piece}`, count: 1, maxDurability: 400, durabilityUsed: 0,
    })));
  const analysis = analyzeArmor(slots);
  assert.equal(analysis.totalSets, 8);
  const report = syncReport([{ ok: true, player: "rarecrit", sets: analysis.totalSets, analysis }], 60);
  assert.deepEqual(report.players, [{ name: "rarecrit", sets: 8,
    helmet: 8, chestplate: 8, leggings: 8, boots: 8 }]);
  const extraHelmet = analyzeArmor([...slots, slots[0]]);
  assert.deepEqual(syncReport([{ ok: true, player: "rarecrit", sets: extraHelmet.totalSets,
    analysis: extraHelmet }], 60).players, [{ name: "rarecrit", sets: 8,
    helmet: 9, chestplate: 8, leggings: 8, boots: 8 }]);
});

test("syncunknown é privado, usa ticket autorizado e não repete o código na resposta", async () => {
  const definition = commandDefinitions.find((command) => command.name === "syncunknown");
  assert.equal(definition.options[0].min_length, 30);
  assert.equal(definition.options[0].max_length, 30);
  const calls = [], replies = [];
  const code = "A".repeat(30);
  const context = {
    config: { staffRoleIds: [] }, store: { getTicketByChannel: async () => ({ status: "open", ownerId: "owner" }) },
    minecraft: { get: () => undefined }, unknownSync: { bind: async (...args) => calls.push(args) },
  };
  const interaction = {
    commandName: "syncunknown", channelId: "ticket", guildId: "guild",
    channel: { permissionsFor: () => new PermissionsBitField([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages]) },
    member: { id: "owner", permissions: new PermissionsBitField(), roles: { cache: new Collection() } },
    options: { getString: () => code }, deferReply: async (options) => { assert.equal(options.flags, 64); },
    editReply: async (reply) => replies.push(reply), reply: async (reply) => replies.push(reply),
  };
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(calls, [[code, "guild", "ticket"]]);
  assert.equal(JSON.stringify(replies).includes(code), false);
  interaction.member.id = "participant";
  await handleInvseeCommand(interaction, context);
  assert.equal(calls.length, 2);
  assert.equal(JSON.stringify(replies).includes(code), false);
  interaction.channel.permissionsFor = () => new PermissionsBitField();
  await handleInvseeCommand(interaction, context);
  assert.equal(calls.length, 2);
  assert.match(JSON.stringify(replies.at(-1)), /participantes de um ticket aberto/);
});

test("erros da API não vazam respostas, segredos ou o código", async () => {
  const bridge = new UnknownSyncClient({ baseUrl: "https://example.invalid", botToken: "x".repeat(40) }, async () => ({
    ok: false, json: async () => ({ ok: false, error: "internal secret and code" }),
  }));
  await assert.rejects(bridge.bind("A".repeat(30), "g", "c"), /API recusou/);
});

test("consulta do clan publica ranking completo e auto-stop invalida o relatório", async () => {
  const published = [], cleared = [], replies = [];
  const session = { channelId: "ticket", nick: "Bot", mode: "p4free", closed: false,
    api: { getClanRoster: async () => ({ ok: true, online: ["Last", "First"], offline: [] }),
      invseeMany: async () => [{ ok: true, player: "Last", sets: 8 }, { ok: true, player: "First", sets: 2 }] } };
  const context = {
    config: { staffRoleIds: [] }, store: { getTicketByChannel: async () => ({ status: "open" }) },
    minecraft: { get: () => session, run: async (_, callback) => callback(session) },
    unknownSync: { publish: async (...args) => published.push(args), clear: async (...args) => cleared.push(args) },
  };
  const interaction = { commandName: "invsee", channelId: "ticket", guildId: "guild",
    channel: { permissionsFor: () => new PermissionsBitField(PermissionFlagsBits.Administrator) },
    options: { getString: () => "CLAN" }, deferReply: async () => {},
    editReply: async (reply) => replies.push(reply), reply: async (reply) => replies.push(reply) };
  await handleInvseeCommand(interaction, context);
  assert.equal(published.length, 1);
  assert.deepEqual(published[0].slice(0, 2), ["guild", "ticket"]);
  assert.deepEqual(published[0][2].players, [{ name: "First", sets: 2 }, { name: "Last", sets: 8 }]);
  interaction.commandName = "invseeauto-stop";
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(cleared, [["guild", "ticket"]]);
  assert.equal(session.latestInvseeReport, null);
});
