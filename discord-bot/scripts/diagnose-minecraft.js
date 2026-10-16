// Read-only network diagnostics: no /login, /register, menu clicks or captcha solving.
import { createRequire } from "node:module";
import { resolve4, resolveSrv } from "node:dns/promises";

const require = createRequire(`${process.cwd()}/package.json`);
const { ping, createClient } = require("minecraft-protocol");
const host = "drazyh.com";
const port = 25565;
const version = "1.8.9";
const args = process.argv.slice(2);
const nick = args[args.indexOf("--nick") + 1];
const hasNick = args.includes("--nick");

console.log(JSON.stringify({ time: new Date().toISOString(), host, port, version }));
for (const [kind, lookup] of [["A", () => resolve4(host)], ["SRV", () => resolveSrv(`_minecraft._tcp.${host}`)]]) {
  try { console.log(JSON.stringify({ dns: kind, records: await lookup() })); }
  catch (error) { console.log(JSON.stringify({ dns: kind, code: error.code })); }
}

try {
  const status = await ping({ host, port, version, closeTimeout: 8000, noPongTimeout: 2000 });
  console.log(JSON.stringify({ status: "ok", version: status.version, players: { online: status.players?.online, max: status.players?.max }, latency: status.latency, description: status.description }));
} catch (error) {
  console.log(JSON.stringify({ status: "failed", code: error.code, message: error.message }));
}

if (hasNick) {
  if (!/^[A-Za-z0-9_]{3,16}$/.test(nick ?? "")) throw new Error("Nick inválido para o diagnóstico.");
  await new Promise((resolve) => {
    const client = createClient({ host, port, version, username: nick, auth: "offline", hideErrors: true });
    let ended = false;
    const finish = (reason) => {
      if (ended) return;
      ended = true;
      clearTimeout(timer);
      console.log(JSON.stringify({ diagnosticEnd: reason }));
      client.end("Network diagnostic finished");
      resolve();
    };
    const timer = setTimeout(() => finish("20s timeout"), 20000);
    client.on("connect", () => console.log(JSON.stringify({ tcp: "connected" })));
    client.on("state", (state) => console.log(JSON.stringify({ protocolState: state })));
    client.on("login", () => console.log(JSON.stringify({ loginProtocol: "accepted" })));
    client.on("chat", (packet) => console.log(JSON.stringify({ serverChat: packet.message })));
    client.on("disconnect", (packet) => { console.log(JSON.stringify({ disconnect: packet.reason })); finish("server disconnect"); });
    client.on("error", (error) => { console.log(JSON.stringify({ error: error.code, message: error.message })); finish("network error"); });
    client.on("end", (reason) => finish(reason));
  });
}
