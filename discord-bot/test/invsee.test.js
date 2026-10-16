import test from "node:test";
import assert from "node:assert/strict";
import { Collection, PermissionsBitField, PermissionFlagsBits } from "discord.js";
import { commandDefinitions } from "../src/commands.js";
import { describeMinecraftFailure, handleInvseeCommand } from "../src/invsee.js";
import { EMBED_COLOR } from "../src/embeds.js";

function fixture({ ready = true, status = {}, mode = "lobby" } = {}) {
  const replies = [];
  const starts = [];
  const session = { nick: "BotTicket", mode, closed: false, api: { waitForSession: async () => ready, getStatus: () => status } };
  const context = {
    config: { ownerRoleId: "owner", staffRoleIds: [] },
    store: { getTicketByChannel: async () => ({ status: "open" }) },
    minecraft: {
      start: (...args) => { starts.push(args); return session; },
      stop: () => { session.closed = true; },
      run: async (_, callback) => callback(session),
    },
  };
  const interaction = {
    commandName: "botinvsee", channelId: "ticket", channel: { permissionsFor: () => new PermissionsBitField(PermissionFlagsBits.Administrator) },
    member: { permissions: new PermissionsBitField(PermissionFlagsBits.Administrator), roles: { cache: new Collection() } },
    options: { getString: (key) => ({ nick: "BotTicket", modo: mode, tag: "CLAN" })[key] },
    deferReply: async () => {}, editReply: async (reply) => { replies.push(reply); }, reply: async (reply) => { replies.push(reply); },
  };
  return { interaction, context, replies, starts, session };
}

test("modo lobby é opcional no slash command e chega à sessão correta", async () => {
  const definition = commandDefinitions.find((command) => command.name === "botinvsee");
  const mode = definition.options.find((option) => option.name === "modo");
  assert.equal(mode.required, false);
  assert.deepEqual(mode.choices.map((choice) => choice.value), ["p4free", "lobby"]);
  const { interaction, context, replies, starts, session } = fixture();
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(starts, [["ticket", "BotTicket", "lobby"]]);
  assert.match(replies[0].embeds[0].toJSON().description, /login confirmado no lobby/);
  assert.match(replies[0].embeds[0].toJSON().description, /Nenhum acesso ao P4Free/);
  assert.equal(replies[0].content, "");
  assert.equal(replies[0].embeds[0].toJSON().color, EMBED_COLOR);
  assert.equal(session.closed, false);
});

test("verificação pendente gera diagnóstico e encerra a sessão, sem alegar sucesso", async () => {
  const { interaction, context, replies, session } = fixture({ ready: false,
    status: { mode: "lobby", phase: "login-captcha", connected: true, authenticated: false, verificationRequired: true } });
  await handleInvseeCommand(interaction, context);
  assert.equal(session.closed, true);
  assert.match(replies[0].embeds[0].toJSON().description, /login ainda não foi confirmado/);
  assert.match(replies[0].embeds[0].toJSON().description, /não automatiza/);
  assert.equal(replies[0].embeds[0].toJSON().color, EMBED_COLOR);
  assert.deepEqual(replies[0].allowedMentions.parse, []);
});

test("consultas Invsee não são executadas em modo lobby", async () => {
  const { interaction, context, replies } = fixture();
  interaction.commandName = "invsee";
  await handleInvseeCommand(interaction, context);
  assert.match(replies[0].embeds[0].toJSON().description, /modo de teste do lobby/);
});

test("diagnóstico diferencia rede, senha, login pendente e P4Free indisponível", () => {
  assert.match(describeMinecraftFailure({ errorCode: "ECONNRESET" }), /Falha de rede \(ECONNRESET\)/);
  assert.match(describeMinecraftFailure({ errorCode: "INVALID_PASSWORD" }), /recusou a senha/);
  assert.match(describeMinecraftFailure({ connected: true }), /não confirmou o login/);
  assert.match(describeMinecraftFailure({ authenticated: true }), /Login confirmado no lobby, mas/);
  assert.match(describeMinecraftFailure({ authenticated: true, waitingInQueue: true }), /ainda está na fila/);
});

test("botinvsee anuncia explicitamente a entrada confirmada no P4Free", async () => {
  const { interaction, context, replies } = fixture({ mode: "p4free" });
  await handleInvseeCommand(interaction, context);
  assert.match(replies[0].embeds[0].toJSON().description, /entrada confirmada no \*\*P4Free\*\*/);
});

test("participante comum não inicia a conta nem escolhe o nick, mesmo sendo autor", async () => {
  const { interaction, context, replies, starts } = fixture({ mode: "p4free" });
  interaction.member = { id: "guest", permissions: new PermissionsBitField(), roles: { cache: new Collection() } };
  context.store.getTicketByChannel = async () => ({ status: "open", ownerId: "guest" });
  await handleInvseeCommand(interaction, context);
  assert.deepEqual(starts, []);
  assert.match(JSON.stringify(replies), /Somente a administração/);
});

test("qualquer participante do ticket pode consultar, reconectar e parar a conta", async (t) => {
  for (const command of ["invsee", "invsee-player", "invseeauto", "invseeauto-stop", "clanwarn", "reconnect", "botinvsee-stop"]) {
    await t.test(command, async (t) => {
      const { interaction, context, replies, session } = fixture({ mode: "p4free" });
      t.after(() => { if (session.auto) clearTimeout(session.auto.timer); });
      const calls = [];
      interaction.commandName = command;
      interaction.guildId = "guild";
      interaction.member = { id: "guest", permissions: new PermissionsBitField(), roles: { cache: new Collection() } };
      interaction.channel.permissionsFor = () => new PermissionsBitField([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages]);
      interaction.options = { getString: (key) => ({ tag: "CLAN", nick: "Player", estado: "on" })[key], getInteger: () => 60 };
      context.store.getTicketByChannel = async () => ({ status: "open", ownerId: "another-user" });
      context.minecraft.get = () => session;
      context.minecraft.stop = () => { calls.push("stop"); return true; };
      context.unknownSync = { publish: async () => {}, clear: async () => calls.push("clear") };
      session.api.getClanRoster = async () => { calls.push("clan"); return { ok: true, online: [], offline: [] }; };
      session.api.invseeMany = async () => [];
      session.api.invseePlayer = async () => { calls.push("player"); return { ok: true, player: "Player", sets: 1,
        analysis: { totals: { helmet: 1, chestplate: 1, leggings: 1, boots: 1 }, heads: 0 } }; };
      session.api.reconnect = async () => { calls.push("reconnect"); return { ok: true }; };
      session.api.sendChatCommand = async () => { calls.push("chat"); return { ok: true }; };
      await handleInvseeCommand(interaction, context);
      assert.ok(replies.length);
      assert.equal(/Somente|administração|não está/.test(JSON.stringify(replies)), false);
      const expected = { invsee: "clan", "invsee-player": "player", invseeauto: "clan", "invseeauto-stop": "clear", clanwarn: "chat", reconnect: "reconnect", "botinvsee-stop": "stop" };
      assert.ok(calls.includes(expected[command]));
    });
  }
});

test("usuário sem acesso ao ticket não executa nem os comandos liberados", async () => {
  const { interaction, context, replies, session } = fixture({ mode: "p4free" });
  interaction.commandName = "botinvsee-stop";
  interaction.channel.permissionsFor = () => new PermissionsBitField();
  await handleInvseeCommand(interaction, context);
  assert.equal(session.closed, false);
  assert.match(JSON.stringify(replies), /participantes de um ticket aberto/);
});
