import { ApplicationFlagsBitField, PermissionFlagsBits, PermissionsBitField, REST, Routes } from "discord.js";
import { loadConfig } from "../src/config.js";

const config = loadConfig();
const rest = new REST({ version: "10" }).setToken(config.token);
const current = await rest.get(Routes.currentApplication());
const requiredFlags = ApplicationFlagsBitField.Flags.GatewayGuildMembersLimited
  | ApplicationFlagsBitField.Flags.GatewayMessageContentLimited;
const permissions = new PermissionsBitField([
  PermissionFlagsBits.KickMembers, PermissionFlagsBits.BanMembers,
  PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ManageRoles, PermissionFlagsBits.ManageMessages,
  PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory,
  PermissionFlagsBits.EmbedLinks, PermissionFlagsBits.AttachFiles, PermissionFlagsBits.AddReactions,
  PermissionFlagsBits.MentionEveryone, PermissionFlagsBits.UseApplicationCommands,
]).bitfield.toString();
const updated = await rest.patch(Routes.currentApplication(), { body: {
  flags: (current.flags ?? 0) | requiredFlags,
  install_params: { scopes: ["bot", "applications.commands"], permissions },
} });
console.log(JSON.stringify({ applicationId: updated.id, guildMembersEnabled: Boolean(updated.flags & ApplicationFlagsBitField.Flags.GatewayGuildMembersLimited),
  messageContentEnabled: Boolean(updated.flags & ApplicationFlagsBitField.Flags.GatewayMessageContentLimited),
  inviteUrl: `https://discord.com/oauth2/authorize?client_id=${config.clientId}&scope=bot%20applications.commands&permissions=${permissions}&integration_type=0` }, null, 2));
