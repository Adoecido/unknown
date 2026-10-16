/** Ticket-scoped Invsee publisher. No client or administrator tokens belong here. */
export class UnknownSyncClient {
  constructor({ baseUrl = "", botToken = "" } = {}, fetchImpl = globalThis.fetch) {
    this.fetch = fetchImpl;
    this.token = botToken;
    this.queues = new Map();
    if (baseUrl) {
      const url = new URL(baseUrl);
      if (url.protocol !== "https:" && !(url.protocol === "http:" && ["127.0.0.1", "localhost", "[::1]"].includes(url.hostname))) {
        throw new Error("UNKNOWN_CONTROL_URL precisa usar HTTPS (HTTP somente no loopback).");
      }
      if (url.username || url.password || url.search || url.hash) throw new Error("URL de sincronização inválida.");
      this.endpoint = new URL("/v1/invsee/sync", url).href;
    }
  }

  request(action, guildId, channelId, arguments_ = {}) {
    if (!this.endpoint || this.token.length < 32) return Promise.reject(new Error("Sincronização não configurada no bot."));
    const key = `${guildId}:${channelId}`;
    const next = (this.queues.get(key) ?? Promise.resolve()).catch(() => {}).then(async () => {
      let response;
      try {
        response = await this.fetch(this.endpoint, {
          method: "POST", redirect: "error", signal: AbortSignal.timeout(10000),
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${this.token}` },
          body: JSON.stringify({ ...arguments_, action, guildId, channelId }),
        });
      } catch { throw new Error("API de sincronização indisponível. Tente novamente."); }
      let result;
      try { result = await response.json(); } catch { throw new Error("Resposta inválida da API de sincronização."); }
      if (!response.ok || result.ok !== true) {
        throw new Error(result.error === "code_expired_or_used" || result.error === "invalid_code"
          ? "Código inválido, vencido ou já utilizado. Copie um novo código no Target."
          : "API recusou a sincronização. Verifique a configuração do bot.");
      }
      return result;
    });
    this.queues.set(key, next);
    next.finally(() => { if (this.queues.get(key) === next) this.queues.delete(key); }).catch(() => {});
    return next;
  }

  bind(code, guildId, channelId) { return this.request("bind", guildId, channelId, { code }); }
  publish(guildId, channelId, report) { return this.request("publish", guildId, channelId, report); }
  clear(guildId, channelId) { return this.request("clear", guildId, channelId); }
}
