import test from "node:test";
import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { setTimeout as delay } from "node:timers/promises";
import { createMineflayerBot } from "../vendor/bot-invsee/src/mineflayerBot.js";
import { makeMinecraftConfig, MinecraftSessions } from "../src/minecraft-sessions.js";

function fakeClient(options) {
  return Object.assign(new EventEmitter(), {
    username: options.username, sent: [], clicked: [], selected: false,
    chat(command) { this.sent.push(command); },
    clickWindow(slot) { this.clicked.push(slot); return Promise.resolve(); },
    setQuickBarSlot() {},
    activateItem() { this.selected = true; },
    quit() { this.emit("end", "test-stop"); },
  });
}

function fixture(mode = "p4free") {
  const config = makeMinecraftConfig({ host: "drazyh.com", port: 25565, version: "1.8.9", auth: "offline", passwordTemplate: "{nick}123", sessionTimeoutMs: 200 }, "BotTicket", mode);
  Object.assign(config.login, { delayAfterSpawnMs: 1, loginCaptchaTimeoutMs: 150, delayBeforeCompassMs: 1, delayBeforeServerReadyMs: 1, captchaPromptGraceMs: 10, serverJoinRetryMs: 20 });
  config.auth = { reconnectDelayMs: 10, delayAfterRegisterMs: 1 };
  const clients = [];
  const factory = (options) => {
    const client = fakeClient(options);
    clients.push(client);
    return client;
  };
  return { api: createMineflayerBot(config, factory), clients };
}

async function waitUntil(predicate) {
  for (let attempt = 0; attempt < 50; attempt++) {
    if (predicate()) return;
    await delay(2);
  }
  assert.fail("A etapa de login simulada não terminou");
}

function confirmP4Free(client) {
  client.emit("scoreboardPosition", 1, { title: "§b§lP4FREE", name: "p4free" });
}

async function completeServerCaptcha(client) {
  client.emit("login");
  client.emit("message", "Você logou com sucesso.");
  client.emit("windowOpen", { title: "Clique no Sinalizador", slots: [{ name: "beacon" }] });
  await waitUntil(() => client.selected);
  client.emit("windowOpen", { title: "Servidores", slots: [{ name: "nether_star", displayName: "P4Free" }] });
  await waitUntil(() => client.clicked.length === 2);
  client.emit("windowOpen", { title: "Captcha: clique a picareta de ferro", slots: [{ name: "iron_pickaxe" }] });
  await delay(2);
}

test("resolver o captcha e continuar no lobby nunca anuncia entrada no P4Free", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  const pending = api.waitForSession();
  await completeServerCaptcha(client);
  client.emit("scoreboardPosition", 1, { title: "§b§lDRAZYH" });
  client.emit("respawn");
  client.emit("spawn");
  client.emit("message", "P4Free disponível no menu de servidores.");
  await delay(5);
  assert.equal(api.isReady(), false);
  assert.equal(api.getStatus().destinationConfirmed, false);
  assert.equal(await pending, false);
  assert.equal(api.isReady(), false);
});

test("a mensagem de transferência só confirma entrada após trocar e carregar o mundo", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  await completeServerCaptcha(client);
  client.emit("message", " * Chegou a sua vez! Conectando no P4Free!\n * Aguarde enquanto preparamos para você entrar.");
  assert.equal(api.isReady(), false);
  client.emit("spawn");
  assert.equal(api.isReady(), false);
  client.emit("respawn");
  assert.equal(api.isReady(), false);
  client.emit("spawn");
  assert.equal(await api.waitForSession(), true);
  assert.equal(api.getStatus().destinationConfirmed, true);
});

test("aguardar na fila não anuncia entrada nem clica novamente no seletor", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  await completeServerCaptcha(client);
  client.emit("message", " * Na posição #4 para entrar no P4Free.");
  await delay(40);
  assert.equal(api.isReady(), false);
  assert.equal(api.getStatus().waitingInQueue, true);
  assert.equal(api.getStatus().serverJoinAttempts, 1);
  assert.deepEqual(client.clicked, [0, 0, 0]);
});

test("captcha que apenas desbloqueia o lobby faz o bot selecionar P4Free novamente", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  let activations = 0;
  client.activateItem = function() { this.selected = true; activations++; };
  await completeServerCaptcha(client);
  await waitUntil(() => activations === 2);
  assert.equal(api.isReady(), false);
  client.emit("windowOpen", { title: "Servidores", slots: [{ name: "nether_star", displayName: "P4Free" }] });
  await waitUntil(() => api.getStatus().serverJoinAttempts === 2);
  client.emit("message", "Conectando...");
  client.emit("respawn");
  client.emit("spawn");
  assert.equal(await api.waitForSession(), true);
});

test("transferência atrasada ainda confirma o destino após reabrir o seletor", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  await completeServerCaptcha(client);
  await waitUntil(() => api.getStatus().phase === "server-selector");
  client.emit("message", " * Chegou a sua vez! Conectando no P4Free!");
  client.emit("respawn");
  client.emit("spawn");
  assert.equal(await api.waitForSession(), true);
  assert.equal(api.getStatus().serverJoinAttempts, 1);
});

test("mensagem atrasada após captcha concluído não impede a abertura do seletor", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("message", "Você logou com sucesso.");
  client.emit("windowOpen", { title: "Clique na Pá", slots: [{ name: "iron_shovel" }] });
  client.emit("message", "Selecione a resposta do captcha.");
  await waitUntil(() => client.selected);
  assert.equal(api.getStatus().verificationRequired, false);
  assert.equal(api.isReady(), false);
});

test("um novo captcha durante o período de espera mantém a verificação pendente", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("message", "Você logou com sucesso.");
  client.emit("windowOpen", { title: "Clique na Pá", slots: [{ name: "iron_shovel" }] });
  await delay(2);
  client.emit("message", "Selecione a resposta do captcha.");
  client.emit("windowOpen", { title: "Clique na Bússola", slots: [{ name: "apple" }] });
  await delay(20);
  assert.equal(api.getStatus().verificationRequired, true);
  assert.equal(api.isReady(), false);
});

test("base processa login, captcha, seletor P4Free e segundo captcha no cliente do ticket", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("spawn");
  await waitUntil(() => client.sent.length > 0);
  assert.equal(client.sent[0], "/login BotTicket123");
  client.emit("windowOpen", { title: "Clique no Sinalizador!", slots: [{ name: "beacon" }] });
  await waitUntil(() => client.selected);
  client.emit("windowOpen", { title: "Servidores", slots: [{ name: "compass", displayName: "p4free" }] });
  await waitUntil(() => client.clicked.length === 2);
  client.emit("message", "Selecione a resposta do captcha.");
  client.emit("windowOpen", { title: "Click on the gold!", slots: [{ name: "gold_ingot" }] });
  confirmP4Free(client);
  assert.equal(await api.waitForSession(), true);
  assert.equal(api.isReady(), true);
  client.emit("end", "connection-lost");
  assert.equal(api.isReady(), false);
  assert.equal(await api.sendChatCommand("/clan roster TEST" ).then((result) => result.ok), false);
});

test("fechar ticket durante reconnect impede uma nova conexão", async () => {
  const { api, clients } = fixture();
  const pending = api.reconnect();
  api.stop();
  assert.equal((await pending).ok, false);
  assert.equal(clients.length, 1);
  assert.equal(await api.waitForSession(), false);
});

test("registro funciona com mensagens de autenticação antes de spawn/login", async (t) => {
  const { api, clients } = fixture("lobby");
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("message", "Utilize o comando /registrar <senha> <senha>.");
  assert.equal(client.sent[0], "/registrar BotTicket123 BotTicket123");
  client.emit("message", "Você se registrou com sucesso.");
  assert.equal(api.getStatus().registered, true);
  assert.equal(api.getStatus().authenticated, false);
  client.emit("spawn");
  client.emit("login");
  await waitUntil(() => client.sent.includes("/login BotTicket123"));
  assert.equal(client.sent.filter((command) => command.startsWith("/login")).length, 1);
  client.emit("message", "Você se logou com sucesso.");
  assert.equal(await api.waitForSession(), true);
  assert.equal(api.getStatus().authenticated, true);
  assert.equal(client.selected, false);
  assert.deepEqual(client.clicked, []);
});

test("evento login inicia autenticação sem esperar spawn e lobby exige confirmação", async (t) => {
  const { api, clients } = fixture("lobby");
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  await waitUntil(() => client.sent.length > 0);
  assert.equal(client.sent[0], "/login BotTicket123");
  assert.equal(api.isReady(), false);
  client.emit("message", "Login efetuado com sucesso.");
  assert.equal(await api.waitForSession(), true);
  assert.equal(client.selected, false);
});

test("captcha no lobby é resolvido antes de confirmar um login autenticado", async (t) => {
  const { api, clients } = fixture("lobby");
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("message", "Login efetuado com sucesso.");
  client.emit("message", "Selecione a resposta do captcha.");
  client.emit("windowOpen", { title: "Clique no diamante!", slots: [{ name: "diamond" }] });
  assert.equal(await api.waitForSession(), true);
  assert.equal(api.getStatus().connected, true);
  assert.equal(api.getStatus().verificationRequired, false);
  assert.equal(api.getStatus().authenticated, true);
  assert.equal(client.selected, false);
  assert.deepEqual(client.clicked, [0]);
});

test("senha incorreta e reset de rede encerram a espera com diagnóstico", async (t) => {
  const { api, clients } = fixture("lobby");
  t.after(() => api.stop());
  clients[0].emit("login");
  clients[0].emit("message", "Senha incorreta.");
  assert.equal(await api.waitForSession(), false);
  assert.equal(api.getStatus().errorCode, "INVALID_PASSWORD");
  const second = fixture("lobby");
  t.after(() => second.api.stop());
  second.clients[0].emit("error", Object.assign(new Error("Connection reset"), { code: "ECONNRESET" }));
  assert.equal(await second.api.waitForSession(), false);
  assert.equal(second.api.getStatus().errorCode, "ECONNRESET");
});

test("login confirmado seguido de verificação não é tratado como sessão pronta no lobby", async (t) => {
  const { api, clients } = fixture("lobby");
  t.after(() => api.stop());
  clients[0].emit("login");
  clients[0].emit("message", "Login efetuado com sucesso.");
  clients[0].emit("message", "Selecione a resposta do captcha.");
  assert.equal(await api.waitForSession(), false);
  await delay(5);
  assert.equal(api.isReady(), false);
  assert.equal(api.getStatus().authenticated, true);
  assert.equal(api.getStatus().verificationRequired, true);
  assert.equal(clients[0].selected, false);
  assert.deepEqual(clients[0].clicked, []);
});

test("stop cancela o envio de login agendado antes da autenticação", async () => {
  const { api, clients } = fixture("lobby");
  clients[0].emit("login");
  api.stop();
  await delay(5);
  assert.deepEqual(clients[0].sent, []);
});

test("registro com o título real clique a picareta de ferro conclui os dois captchas", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("message", "Utilize o comando /registrar <senha> <senha>.");
  client.emit("message", "Você se registrou com sucesso.");
  client.emit("message", "Selecione a resposta do captcha.");
  client.emit("windowOpen", {
    title: '"Captcha: clique a picareta de ferro"', inventoryStart: 4,
    slots: [{ name: "wooden_pickaxe" }, { name: "diamond_pickaxe" }, { name: "iron_pickaxe" }, { name: "golden_pickaxe" }],
  });
  client.emit("message", "Você já está logado.");
  assert.equal(api.getStatus().authenticated, true);
  assert.equal(api.getStatus().verificationRequired, true);
  assert.equal(api.isReady(), false);
  assert.deepEqual(client.clicked, [2]);
  await waitUntil(() => client.selected);
  client.emit("windowOpen", { title: "Servidores", slots: [{ name: "compass", displayName: "P4Free" }] });
  await waitUntil(() => client.clicked.length === 2);
  client.emit("windowOpen", { title: "Captcha: clique o sinalizador", slots: [{ name: "beacon" }] });
  confirmP4Free(client);
  assert.equal(await api.waitForSession(), true);
});

test("captchas com componentes JSON e cores escolhem o material específico", async (t) => {
  const cases = [
    { title: { text: "§aCaptcha: clique na ", extra: [{ text: "espada de diamante!" }] }, names: ["iron_sword", "diamond_sword"] },
    { title: '{"text":"Clique as botas de couro!"}', names: ["iron_boots", "leather_boots"] },
    { title: "Clique no bloco de ouro!", names: ["iron_block", "gold_block"] },
    { title: "Clique na Fornalha", names: ["bucket", "furnace"] },
    { title: "Captcha: clique o Baú do Ender", names: ["chest", "ender_chest"] },
    { title: "Captcha: clique a espada dourada", names: ["iron_sword", "golden_sword"] },
    { title: "Clique na picareta dourada!", names: ["diamond_pickaxe", "golden_pickaxe"] },
  ];
  for (const entry of cases) {
    await t.test(JSON.stringify(entry.title), async (t) => {
      const { api, clients } = fixture("lobby");
      t.after(() => api.stop());
      const client = clients[0];
      client.emit("login");
      client.emit("message", "Login efetuado com sucesso.");
      client.emit("windowOpen", { title: entry.title, slots: entry.names.map((name) => ({ name })) });
      assert.equal(await api.waitForSession(), true);
      assert.deepEqual(client.clicked, [1]);
    });
  }
});

test("um item do inventário pessoal não é usado como resposta do captcha", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("windowOpen", { title: "Clique a picareta de ferro", inventoryStart: 2,
    slots: [{ name: "diamond_pickaxe" }, { name: "golden_pickaxe" }, { name: "iron_pickaxe" }] });
  assert.equal(await api.waitForSession(), false);
  assert.deepEqual(client.clicked, []);
});

test("dois tickets autenticam sem misturar captchas, inclusive com janelas abertas durante o clique", async (t) => {
  const clients = [];
  const manager = new MinecraftSessions({ host: "drazyh.com", port: 25565, version: "1.8.9", auth: "offline",
    passwordTemplate: "{nick}123", maximumSessions: 10, sessionTimeoutMs: 200, connectionIntervalMs: 10 }, (config) => {
    Object.assign(config.login, { delayAfterSpawnMs: 1, loginCaptchaTimeoutMs: 150, delayBeforeCompassMs: 1, delayBeforeServerReadyMs: 1 });
    config.auth = { delayAfterRegisterMs: 1 };
    return createMineflayerBot(config, (options) => { const client = fakeClient(options); clients.push(client); return client; });
  });
  t.after(() => manager.stopAll());
  const first = manager.start("ticket-a", "BotSolo");
  const second = manager.start("ticket-b", "BotClan");
  assert.equal(second.api.getStatus().phase, "queued");
  const secondReady = second.api.waitForSession();
  await waitUntil(() => clients.length === 2);
  const [clientA, clientB] = clients;
  clientA.emit("login");
  clientB.emit("login");
  await waitUntil(() => clientA.sent.length && clientB.sent.length);
  assert.equal(clientA.sent[0], "/login BotSolo123");
  assert.equal(clientB.sent[0], "/login BotClan123");
  clientA.emit("message", "Você logou com sucesso.");
  clientA.emit("windowOpen", { title: "Clique na Fornalha", slots: [{ name: "furnace" }] });
  clientB.emit("windowOpen", { title: "Clique na Sela", slots: [{ name: "saddle" }] });
  await waitUntil(() => clientA.selected && clientB.selected);
  clientA.emit("windowOpen", { title: "Servidores", slots: [{ name: "compass", displayName: "p4free" }] });
  await waitUntil(() => clientA.clicked.length === 2);
  clientA.emit("windowOpen", { title: "Clique na Teia", slots: [{ name: "web" }] });
  confirmP4Free(clientA);
  assert.equal(await first.api.waitForSession(), true);
  assert.equal(second.api.isReady(), false);

  let stage = "extra-login-captcha";
  clientB.clickWindow = function(slot) {
    this.clicked.push(slot);
    if (stage === "extra-login-captcha") {
      stage = "selector";
      this.emit("windowOpen", { title: "Servidores", slots: [{ name: "compass", displayName: "p4free" }] });
    } else if (stage === "selector") {
      stage = "server-captcha";
      this.emit("windowOpen", { title: "Click on the gold", slots: [{ name: "gold_ingot" }] });
    }
    return Promise.resolve();
  };
  clientB.emit("windowOpen", { title: "Captcha: clique a espada dourada", slots: [{ name: "iron_sword" }, { name: "golden_sword" }] });
  confirmP4Free(clientB);
  assert.equal(await secondReady, true);
  assert.deepEqual(clientB.clicked, [0, 1, 0, 0]);
  assert.deepEqual(clientA.clicked, [0, 0, 0]);
  assert.equal(first.api.isReady(), true);
  manager.stop("ticket-b");
  assert.equal(first.api.isReady(), true);
});

test("fechar um ticket na fila impede a criação de sua conexão Minecraft", async (t) => {
  const clients = [];
  const manager = new MinecraftSessions({ host: "drazyh.com", port: 25565, version: "1.8.9", auth: "offline",
    passwordTemplate: "{nick}123", maximumSessions: 10, sessionTimeoutMs: 200, connectionIntervalMs: 40 },
  (config) => createMineflayerBot(config, (options) => { const client = fakeClient(options); clients.push(client); return client; }));
  t.after(() => manager.stopAll());
  manager.start("a", "BotA");
  const second = manager.start("b", "BotB");
  const pending = second.api.waitForSession();
  assert.equal(second.api.bot, null);
  manager.stop("b");
  assert.equal(await pending, false);
  await delay(60);
  assert.equal(clients.length, 1);
  assert.equal(clients[0].username, "BotA");
});

test("recusa por conexões rápidas preserva o motivo do servidor", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  clients[0].emit("kicked", { text: "Você está entrando muito rápido, tente novamente mais tarde." });
  clients[0].emit("end", "socketClosed");
  assert.equal(await api.waitForSession(), false);
  assert.equal(api.getStatus().errorCode, "CONNECTION_RATE_LIMIT");
});

test("registro novo aceita o seletor após mensagem atrasada de captcha", async (t) => {
  const { api, clients } = fixture();
  t.after(() => api.stop());
  const client = clients[0];
  client.emit("login");
  client.emit("message", "Utilize o comando /registrar <senha> <senha>.");
  client.emit("message", "Você se registrou com sucesso.");
  client.emit("message", "Selecione a resposta do captcha.");
  client.emit("windowOpen", { title: "Clique no Peixe", slots: [{ name: "fish" }] });
  await waitUntil(() => client.selected);
  client.emit("windowOpen", { title: "Captcha: clique a bússola", inventoryStart: 4,
    slots: [{ name: "bow" }, { name: "bucket" }, { name: "golden_sword" }, { name: "compass" }, { name: "compass" }] });
  await waitUntil(() => api.getStatus().phase === "server-selector");
  client.emit("message", "Selecione a resposta do captcha.");
  assert.equal(api.getStatus().verificationRequired, true);
  client.emit("windowOpen", { title: "Servidores", slots: [{ name: "compass", displayName: "p4free" }] });
  await waitUntil(() => client.clicked.length === 3);
  assert.equal(api.getStatus().phase, "joining-server");
  client.emit("windowOpen", { title: "Clique na Pena", slots: [{ name: "feather" }] });
  confirmP4Free(client);
  assert.equal(await api.waitForSession(), true);
  assert.deepEqual(client.clicked, [0, 3, 0, 0]);
  assert.equal(api.getStatus().verificationRequired, false);
});
