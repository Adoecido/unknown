// Explicit account login test. Never clicks menus or solves captcha challenges.
import { createRequire } from "node:module";
const require = createRequire(`${process.cwd()}/package.json`);
const mineflayer = require("mineflayer");
const nick = process.argv.slice(2)[0];
if (!/^[A-Za-z0-9_]{3,16}$/.test(nick ?? "")) throw new Error("Informe o nick do teste.");
const password = `${nick}123`;
const bot = mineflayer.createBot({ host: "drazyh.com", port: 25565, version: "1.8.9", auth: "offline", username: nick, hideErrors: true });
let registered = false;
let loggedIn = false;
let done = false;
function report(value) { console.log(JSON.stringify(value).replaceAll(password, "[senha ocultada]")); }
const timer = setTimeout(() => finish("20s timeout"), 20000);
function finish(reason) {
  if (done) return;
  done = true;
  clearTimeout(timer);
  report({ finished: reason });
  bot.quit("Login diagnostic finished");
}
bot.on("login", () => report({ protocolLogin: true }));
bot.on("spawn", () => report({ spawned: true }));
bot.on("message", (message) => {
  const text = message.toString().trim();
  if (!text || done) return;
  report({ serverMessage: text });
  const normalized = text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
  if (!registered && /utilize o comando.*\/(register|registrar)/.test(normalized)) {
    registered = true;
    bot.chat(`/registrar ${password} ${password}`);
    report({ sent: "/registrar [senha ocultada] [senha ocultada]" });
  } else if (!loggedIn && /utilize o comando.*\/login/.test(normalized)) {
    loggedIn = true;
    bot.chat(`/login ${password}`);
    report({ sent: "/login [senha ocultada]" });
  }
});
bot.on("windowOpen", (window) => { report({ windowTitle: window.title }); finish("menu/captcha requires separate verification"); });
bot.on("kicked", (reason) => report({ kicked: reason }));
bot.on("error", (error) => { report({ error: error.code, message: error.message }); finish("network error"); });
bot.on("end", (reason) => { report({ disconnected: reason }); finish("connection ended"); });
