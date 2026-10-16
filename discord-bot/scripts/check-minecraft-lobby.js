// Explicit lobby-only test: no selector actions or automated verification.
import { loadConfig } from "../src/config.js";
import { makeMinecraftConfig } from "../src/minecraft-sessions.js";
import { createMineflayerBot } from "../vendor/bot-invsee/src/mineflayerBot.js";

const nick = process.argv[2];
if (!/^[A-Za-z0-9_]{3,16}$/.test(nick ?? "")) throw new Error("Uso: node scripts/check-minecraft-lobby.js NomeDaConta");
const config = loadConfig({ requireToken: false });
const api = createMineflayerBot(makeMinecraftConfig(config.minecraft, nick, "lobby"));
try {
  const ready = await api.waitForSession();
  console.log(JSON.stringify({ nick, ready, ...api.getStatus() }));
} finally { api.stop(); }
