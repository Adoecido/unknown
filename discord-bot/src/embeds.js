import { EmbedBuilder } from "discord.js";
import { clipText } from "./text.js";

export const EMBED_COLOR = 0xffffff;

// All visible text belongs in white embeds. Only explicit notification mentions
// stay outside: Discord does not deliver pings from embed descriptions.
export function embedMessage(payload) {
  const source = typeof payload === "string" ? { content: payload } : payload;
  const { content, mentionContent = "", embeds = [], ...options } = source;
  const formatted = embeds.map((embed) => new EmbedBuilder(
    typeof embed.toJSON === "function" ? embed.toJSON() : embed,
  ).setColor(EMBED_COLOR));
  if (content?.trim()) {
    formatted.unshift(new EmbedBuilder().setColor(EMBED_COLOR).setDescription(clipText(content, 4096)));
  }
  if (!formatted.length) {
    formatted.push(new EmbedBuilder().setColor(EMBED_COLOR)
      .setDescription(options.files?.length ? "Arquivo anexado." : "Mensagem do bot."));
  }
  return { ...options, content: mentionContent, embeds: formatted,
    allowedMentions: options.allowedMentions ?? { parse: [] } };
}

export function sendEmbed(channel, payload) {
  return channel.send(embedMessage(payload));
}

export function replyEmbed(message, payload) {
  return message.reply(embedMessage(payload));
}

export function editEmbedReply(interaction, payload) {
  return interaction.editReply(embedMessage(payload));
}

export function editEmbedMessage(message, payload) {
  return message.edit(embedMessage(payload));
}
