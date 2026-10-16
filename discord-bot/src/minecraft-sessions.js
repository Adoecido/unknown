import { createMineflayerBot } from "../vendor/bot-invsee/src/mineflayerBot.js";

export function makeMinecraftConfig(config, nick, mode = "p4free") {
  return {
    mode,
    minecraft: { host: config.host, port: config.port, version: config.version, auth: config.auth, username: nick },
    login: { password: config.passwordTemplate.replaceAll("{nick}", nick), commandTemplate: "/login {password}",
      sessionTimeoutMs: config.sessionTimeoutMs, compassHotbarSlot: 2 },
    server: { profile: "drazyh", drazyh: { requiresLoginCaptcha: true, requiresServerCaptcha: true, selectorItemName: "p4free" } },
    invsee: { commandTemplate: "/invsee {player}", windowTimeoutMs: 4000, delayBetweenPlayersMs: 1500 },
    clan: { commandTemplate: "/clan roster {tag}", rosterTimeoutMs: 2000 },
  };
}

export class MinecraftSessions {
  sessions = new Map();
  nextConnectionAt = 0;
  constructor(config, factory = createMineflayerBot) {
    this.config = config;
    this.factory = factory;
  }

  reserveConnectionDelay() {
    const now = Date.now();
    const connectAt = Math.max(now, this.nextConnectionAt);
    this.nextConnectionAt = connectAt + (this.config.connectionIntervalMs ?? 5000);
    return connectAt - now;
  }

  start(channelId, nick, mode = "p4free") {
    if (!["p4free", "lobby"].includes(mode)) throw new Error("Modo inválido. Escolha lobby ou p4free.");
    if (!/^[A-Za-z0-9_]{3,16}$/.test(nick)) throw new Error("O nick deve ter entre 3 e 16 letras, números ou sublinhados.");
    if (this.sessions.has(channelId)) throw new Error("Este ticket já tem uma conta. Use /botinvsee-stop antes de trocar.");
    if ([...this.sessions.values()].some((session) => session.nick.toLowerCase() === nick.toLowerCase())) {
      throw new Error("Esta conta já está em uso em outro ticket. Escolha outro nick.");
    }
    if (this.sessions.size >= this.config.maximumSessions) throw new Error("O limite de sessões Minecraft foi atingido.");
    const api = this.factory({ ...makeMinecraftConfig(this.config, nick, mode),
      reserveConnectionDelay: () => this.reserveConnectionDelay() });
    const session = { api, nick, mode, channelId, busy: false, closed: false, auto: null, warnEnabled: false, cleanups: new Set() };
    this.sessions.set(channelId, session);
    return session;
  }

  get(channelId) { return this.sessions.get(channelId); }

  async run(channelId, callback) {
    const session = this.get(channelId);
    if (!session || session.closed) throw new Error("Nenhuma conta ativa neste ticket. Peça à administração para usar /botinvsee nick:... primeiro.");
    if (session.busy) throw new Error("A conta já está executando uma consulta neste ticket. Aguarde terminar.");
    session.busy = true;
    try { return await callback(session); } finally { session.busy = false; }
  }

  stop(channelId) {
    const session = this.get(channelId);
    if (!session) return false;
    session.closed = true;
    if (session.auto) clearTimeout(session.auto.timer);
    for (const cleanup of session.cleanups) {
      try { cleanup(); } catch (error) { console.error("Erro ao limpar sessão Minecraft:", error.message); }
    }
    try { session.api.stop(); }
    catch (error) { console.error("Erro ao desligar conta Minecraft:", error.message); }
    finally { this.sessions.delete(channelId); }
    return true;
  }

  stopAll() { for (const channelId of [...this.sessions.keys()]) this.stop(channelId); }
}
