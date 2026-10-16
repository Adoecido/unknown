import { REST, Routes } from "discord.js";
import { loadConfig } from "../src/config.js";
import { commandDefinitions } from "../src/commands.js";

const config = loadConfig();
const rest = new REST({ version: "10" }).setToken(config.token);

const route = config.guildId
  ? Routes.applicationGuildCommands(config.clientId, config.guildId)
  : Routes.applicationCommands(config.clientId);

console.log(`Publicando ${commandDefinitions.length} comando(s) ${config.guildId ? "no servidor de desenvolvimento" : "globalmente"}...`);
await rest.put(route, { body: commandDefinitions });
console.log("Comandos publicados com sucesso.");
