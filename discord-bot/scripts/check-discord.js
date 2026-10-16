import { REST, Routes } from "discord.js";
import { loadConfig } from "../src/config.js";

const config = loadConfig();
const rest = new REST({ version: "10" }).setToken(config.token);
const guilds = await rest.get(Routes.userGuilds());
console.log(JSON.stringify({ guilds: guilds.map((guild) => ({ id: guild.id, name: guild.name })) }));
for (const [label, id] of Object.entries(config.channels)) {
  const channel = await rest.get(Routes.channel(id));
  const messages = await rest.get(Routes.channelMessages(id), { query: new URLSearchParams({ limit: "5" }) });
  console.log(JSON.stringify({ channel: label, id: channel.id, name: channel.name, guildId: channel.guild_id,
    latestBotMessages: messages.filter((message) => message.author.id === config.clientId).map((message) => ({
      id: message.id,
      titles: message.embeds.map((embed) => embed.title),
      embedColors: message.embeds.map((embed) => embed.color),
      buttons: message.components.flatMap((row) => row.components.map((button) => button.label).filter(Boolean)),
      reactions: message.reactions?.map((reaction) => ({ emoji: reaction.emoji.name, count: reaction.count })),
    })),
    overwrites: channel.permission_overwrites.map((overwrite) => ({ id: overwrite.id, allow: overwrite.allow, deny: overwrite.deny })),
  }));
}
const commands = await rest.get(Routes.applicationGuildCommands(config.clientId, config.guildId));
console.log(JSON.stringify({ guildCommands: commands.map((command) => command.name),
  botInvseeOptions: commands.find((command) => command.name === "botinvsee")?.options }));
