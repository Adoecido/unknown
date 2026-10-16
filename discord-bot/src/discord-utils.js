import { EmbedBuilder, MessageFlags, PermissionFlagsBits } from "discord.js";
import { clipText } from "./text.js";
import { EMBED_COLOR, embedMessage, sendEmbed } from "./embeds.js";

export function isStaff(member, config) {
  if (!member) return false;
  return member.permissions.has(PermissionFlagsBits.Administrator)
    || config.staffRoleIds.some((roleId) => member.roles.cache.has(roleId));
}

export function isAdmin(member, config) {
  return Boolean(member && (member.permissions.has(PermissionFlagsBits.Administrator)
    || member.roles.cache.has(config.ownerRoleId)));
}

export function canManageTicket(member, ticket, config) {
  return Boolean(member && (ticket.ownerId === member.id || isStaff(member, config)));
}

export async function replyEphemeral(interaction, payload) {
  const options = embedMessage(payload);
  if (interaction.deferred || interaction.replied) {
    if (interaction.deferred && !interaction.replied) return interaction.editReply(options);
    return interaction.followUp({ ...options, flags: MessageFlags.Ephemeral });
  }
  return interaction.reply({ ...options, flags: MessageFlags.Ephemeral });
}

export async function requireUserPermission(interaction, permission, message) {
  if (interaction.memberPermissions?.has(permission)) return true;
  await replyEphemeral(interaction, message);
  return false;
}

export function makeModerationEmbed(guild, entry) {
  const target = entry.targetId ? `<@${entry.targetId}>` : entry.targetLabel;
  const fields = [
    { name: "Caso", value: `#${entry.number}`, inline: true },
    { name: "Moderador", value: `<@${entry.moderatorId}>`, inline: true },
    { name: "Alvo", value: target || entry.targetLabel || "Desconhecido", inline: true },
    { name: "Motivo", value: clipText(entry.reason, 1024), inline: false },
  ];

  if (entry.metadata.channelId) {
    fields.push({ name: "Canal", value: `<#${entry.metadata.channelId}>`, inline: true });
  }
  if (entry.metadata.deletedCount) {
    fields.push({ name: "Mensagens apagadas", value: String(entry.metadata.deletedCount), inline: true });
  }

  return new EmbedBuilder()
    .setColor(EMBED_COLOR)
    .setTitle(`Mod log — ${entry.action}`)
    .addFields(fields)
    .setTimestamp(new Date(entry.createdAt))
    .setFooter({ text: guild.name });
}

export async function writeModerationLog({ guild, config, store, action, moderatorId, targetId, targetLabel, reason, metadata }) {
  const entry = await store.addModerationCase({
    guildId: guild.id,
    action,
    moderatorId,
    targetId,
    targetLabel,
    reason,
    metadata,
    defaultFilterWords: config.defaultFilterWords,
  });

  if (!config.modLogChannelId) return entry;
  const channel = await guild.channels.fetch(config.modLogChannelId).catch(() => null);
  if (!channel?.isTextBased()) return entry;

  await sendEmbed(channel, { embeds: [makeModerationEmbed(guild, entry)] }).catch((error) => {
    console.error("Não foi possível enviar o mod log:", error.message);
  });
  return entry;
}
