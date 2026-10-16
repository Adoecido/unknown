import test from "node:test";
import assert from "node:assert/strict";
import { MinecraftSessions } from "../src/minecraft-sessions.js";

function sessions(maximumSessions = 10) {
  const created = [];
  const manager = new MinecraftSessions({ host: "drazyh.com", port: 25565, version: "1.8.9", auth: "offline",
    passwordTemplate: "{nick}123", maximumSessions, sessionTimeoutMs: 45000 }, (config) => {
    const api = { stopped: false, stop() { this.stopped = true; } };
    created.push({ config, api });
    return api;
  });
  return { manager, created };
}

test("tickets usam conexões e senhas distintas, sem compartilhar estado", async () => {
  const { manager, created } = sessions();
  const first = manager.start("ticket-a", "BotSolo");
  const second = manager.start("ticket-b", "BotClan");
  assert.notEqual(first.api, second.api);
  assert.equal(created[0].config.login.password, "BotSolo123");
  assert.equal(created[1].config.login.password, "BotClan123");
  first.warnEnabled = true;
  assert.equal(second.warnEnabled, false);
  assert.throws(() => manager.start("ticket-c", "botsolo"), /outro ticket/);
  assert.throws(() => manager.start("ticket-a", "NovoBot"), /já tem uma conta/);
  manager.stop("ticket-a");
  assert.equal(first.api.stopped, true);
  assert.equal(second.api.stopped, false);
  assert.ok(manager.start("ticket-c", "BotSolo"));
  manager.stopAll();
});

test("consultas simultâneas no mesmo ticket não se misturam; outros tickets seguem disponíveis", async () => {
  const { manager } = sessions();
  manager.start("a", "BotA");
  manager.start("b", "BotB");
  let finish;
  const pending = manager.run("a", () => new Promise((resolve) => { finish = resolve; }));
  await assert.rejects(manager.run("a", () => {}), /já está executando/);
  assert.equal(await manager.run("b", (session) => session.nick), "BotB");
  finish();
  await pending;
  assert.equal(await manager.run("a", (session) => session.nick), "BotA");
  manager.stopAll();
});

test("encerrar ticket cancela timers, listeners e libera a conta", () => {
  const { manager } = sessions(1);
  const session = manager.start("a", "BotA");
  let cleaned = false;
  session.auto = { timer: setTimeout(() => assert.fail("timer deveria ser cancelado"), 10000) };
  session.cleanups.add(() => { cleaned = true; });
  assert.throws(() => manager.start("b", "BotB"), /limite/);
  manager.stop("a");
  assert.equal(cleaned, true);
  assert.equal(session.closed, true);
  assert.equal(manager.get("a"), undefined);
  manager.start("b", "BotB");
  manager.stopAll();
});

test("modo lobby é exclusivo do ticket e P4Free continua sendo o padrão", () => {
  const { manager, created } = sessions();
  assert.equal(manager.start("a", "LobbyBot", "lobby").mode, "lobby");
  assert.equal(created[0].config.mode, "lobby");
  assert.equal(manager.start("b", "ClanBot").mode, "p4free");
  assert.equal(created[1].config.mode, "p4free");
  assert.throws(() => manager.start("c", "AnotherBot", "invalid"), /Modo inválido/);
  manager.stopAll();
});
