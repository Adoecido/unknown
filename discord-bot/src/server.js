import { AttachmentBuilder, ChannelType, EmbedBuilder, PermissionFlagsBits } from "discord.js";
import { isAdmin, replyEphemeral } from "./discord-utils.js";
import { ticketPanelPayload, enableTicketCommands } from "./tickets.js";
import { EMBED_COLOR, sendEmbed, editEmbedMessage, editEmbedReply } from "./embeds.js";

export const minecraftCommands = new Set([
  "botinvsee", "botinvsee-stop", "invsee", "invsee-player", "invseeauto", "invseeauto-stop", "clanwarn", "reconnect", "syncunknown",
]);

export function allowedCommandLocation(commandName, subcommand, channelId, config, ticket) {
  if (commandName === "mod" && ["limpar", "historico"].includes(subcommand)) return true;
  if (minecraftCommands.has(commandName)) return Boolean(ticket?.status === "open");
  if (commandName === "ticket" && ["adicionar", "remover", "fechar", "info"].includes(subcommand)) return Boolean(ticket?.status === "open");
  return channelId === config.channels.commands;
}

export async function checkCommandLocation(interaction, { config, store }) {
  const ticket = await store.getTicketByChannel(interaction.channelId);
  const subcommand = interaction.options.getSubcommand(false);
  if (allowedCommandLocation(interaction.commandName, subcommand, interaction.channelId, config, ticket)) return true;
  await replyEphemeral(interaction, minecraftCommands.has(interaction.commandName)
    ? "Use este comando dentro de um ticket aberto."
    : `Use os comandos em <#${config.channels.commands}>. A gestão de tickets e o Minecraft funcionam dentro dos tickets; limpeza e histórico podem ser usados em qualquer canal.`);
  return false;
}

function verificationPayload() {
  return { embeds: [new EmbedBuilder().setColor(EMBED_COLOR)
    .setTitle("Bem-vindo à UNKNOWN")
    .setDescription("Para liberar seu acesso ao servidor, reaja com ✅ nesta mensagem.\n\nVocê receberá o cargo **.gg/unknown** e poderá acessar os canais e abrir um ticket de compra.")
    .setFooter({ text: "Verificação de entrada • Reaja apenas nesta mensagem" })], allowedMentions: { parse: [] } };
}

async function upsertPanel(channel, messageId, payload) {
  const existing = messageId ? await channel.messages.fetch(messageId).catch((error) => {
    if (error.code === 10008) return null;
    throw error;
  }) : null;
  return existing ? editEmbedMessage(existing, payload) : sendEmbed(channel, payload);
}

// Only public channels and the supplied channels are gated. Existing private channels keep their access policy.
async function configurePermissions(guild, config) {
  const bot = guild.members.me ?? await guild.members.fetchMe();
  if (!bot.permissions.has([PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ManageRoles])) {
    throw new Error("O bot precisa de Gerenciar canais e Gerenciar cargos para configurar o servidor.");
  }
  const roles = await guild.roles.fetch();
  const verified = roles.get(config.verifiedRoleId);
  if (!verified || verified.managed || bot.roles.highest.comparePositionTo(verified) <= 0) {
    throw new Error("O cargo do bot precisa estar acima de .gg/unknown para entregar a verificação.");
  }
  const channels = await guild.channels.fetch();
  const known = new Set(Object.values(config.channels));
  for (const id of known) {
    if (channels.get(id)?.type !== ChannelType.GuildText) throw new Error(`Canal de texto configurado não encontrado: ${id}`);
  }
  for (const id of config.staffRoleIds) {
    if (!roles.has(id)) throw new Error(`Cargo de equipe não encontrado: ${id}`);
  }
  for (const channel of channels.values()) {
    if (!channel || ![ChannelType.GuildText, ChannelType.GuildVoice, ChannelType.GuildForum, ChannelType.GuildAnnouncement].includes(channel.type)) continue;
    if (!known.has(channel.id) && !channel.permissionsFor(guild.roles.everyone)?.has(PermissionFlagsBits.ViewChannel)) continue;
    const home = channel.id === config.channels.home;
    const admin = channel.id === config.channels.admin;
    const readonly = [config.channels.features, config.channels.media, config.channels.announcement, config.channels.tickets].includes(channel.id);
    await channel.permissionOverwrites.edit(bot.id, {
      ViewChannel: true, SendMessages: true, ReadMessageHistory: true, EmbedLinks: true, AttachFiles: true,
      AddReactions: true, UseApplicationCommands: true, ManageMessages: true,
    });
    await channel.permissionOverwrites.edit(guild.roles.everyone.id, {
      ViewChannel: home, ...(home ? { SendMessages: false, AddReactions: true, ReadMessageHistory: true } : {}),
    });
    await channel.permissionOverwrites.edit(config.verifiedRoleId, {
      ViewChannel: !admin, ReadMessageHistory: true, ...(home || readonly ? { SendMessages: false } : {}),
      ...(!admin && !home && !readonly ? { SendMessages: true, UseApplicationCommands: true } : {}),
    });
    for (const id of config.staffRoleIds) {
      await channel.permissionOverwrites.edit(id, { ViewChannel: !admin || id === config.ownerRoleId,
        ...(id === config.ownerRoleId ? { SendMessages: true } : {}) });
    }
  }
}

async function synchronizeVerification(message, guild, config) {
  const reaction = message.reactions.cache.find((item) => item.emoji.name === "✅");
  if (!reaction) return;
  let after;
  for (;;) {
    const users = await reaction.users.fetch({ limit: 100, ...(after ? { after } : {}) });
    for (const user of users.values()) {
      if (user.bot) continue;
      const member = await guild.members.fetch(user.id).catch(() => null);
      if (member && !member.roles.cache.has(config.verifiedRoleId)) await member.roles.add(config.verifiedRoleId, "Verificação por reação em #home");
    }
    if (users.size < 100) break;
    after = [...users.keys()].sort((left, right) => BigInt(left) < BigInt(right) ? -1 : 1).at(-1);
  }
}

export async function setupServer(guild, { config, store }) {
  await configurePermissions(guild, config);
  const ticketCount = await enableTicketCommands(guild, store);
  console.log(`Comandos habilitados em ${ticketCount} ticket(s) aberto(s).`);
  const settings = await store.getGuildSettings(guild.id, config.defaultFilterWords);
  const home = await guild.channels.fetch(config.channels.home);
  const ticketChannel = await guild.channels.fetch(config.channels.tickets);
  const verification = await upsertPanel(home, settings.verificationMessageId, verificationPayload());
  const panel = await upsertPanel(ticketChannel, settings.ticketPanelMessageId, ticketPanelPayload());
  await store.updateGuildSettings(guild.id, { verificationMessageId: verification.id, ticketPanelMessageId: panel.id });
  await verification.react("✅");
  await synchronizeVerification(verification, guild, config);
  if (!settings.memberSnapshotSent) {
    const members = await guild.members.fetch();
    const lines = [...members.values()].sort((a, b) => (a.joinedTimestamp ?? 0) - (b.joinedTimestamp ?? 0))
      .map((member) => `${member.user.tag} | ID ${member.id} | Entrada ${member.joinedAt?.toISOString() ?? "desconhecida"}`);
    const admin = await guild.channels.fetch(config.channels.admin);
    await sendEmbed(admin, { embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle("Lista inicial de membros")
      .setDescription(`${members.size} membro(s) atual(is). As novas entradas serão registradas aqui.`)],
    files: [new AttachmentBuilder(Buffer.from(lines.join("\n")), { name: "membros-iniciais.txt" })] });
    await store.updateGuildSettings(guild.id, { memberSnapshotSent: true });
  }
}

export async function handleVerification(reaction, user, { config, store }) {
  if (user.bot || reaction.message.channelId !== config.channels.home || reaction.emoji.name !== "✅") return;
  if (reaction.partial) await reaction.fetch();
  const guild = reaction.message.guild;
  if (!guild) return;
  const settings = await store.getGuildSettings(guild.id);
  if (reaction.message.id !== settings.verificationMessageId) return;
  const member = await guild.members.fetch(user.id);
  if (!member.roles.cache.has(config.verifiedRoleId)) await member.roles.add(config.verifiedRoleId, "Verificação por reação em #home");
}

export async function handleMemberJoin(member, { config }) {
  if (config.guildId && member.guild.id !== config.guildId) return;
  if (!config.guildId && !member.guild.channels.cache.has(config.channels.admin)) return;
  const channel = await member.guild.channels.fetch(config.channels.admin);
  await sendEmbed(channel, { embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle("Novo membro no servidor")
    .setThumbnail(member.user.displayAvatarURL())
    .addFields({ name: "Membro", value: `${member.user.tag}\n<@${member.id}>` },
      { name: "ID", value: member.id, inline: true },
      { name: "Conta criada", value: `<t:${Math.floor(member.user.createdTimestamp / 1000)}:F>`, inline: true })
    .setTimestamp()], allowedMentions: { parse: [] } });
}

export async function handlePublication(interaction, { config }) {
  if (!isAdmin(interaction.member, config)) return replyEphemeral(interaction, "Somente administradores ou o cargo unknown podem publicar aqui.");
  await interaction.deferReply({ ephemeral: true });
  const name = interaction.commandName;
  const channelId = name === "video" ? config.channels.media : name === "features" ? config.channels.features : config.channels.announcement;
  const channel = await interaction.guild.channels.fetch(channelId);
  if (name === "video") {
    const raw = interaction.options.getString("link", true).trim();
    let url;
    try { url = new URL(raw); } catch { return editEmbedReply(interaction, "Informe um link de vídeo válido, começando com https://."); }
    if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) return editEmbedReply(interaction, "Informe um link público http:// ou https://.");
    await sendEmbed(channel, { embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle("🎬 Novo vídeo!")
      .setDescription(`${url.href}\n\n👍 Deixe seu like e inscreva-se no canal para acompanhar os próximos vídeos!`).setURL(url.href)], allowedMentions: { parse: [] } });
  } else {
    const title = interaction.options.getString("titulo")?.trim() || (name === "features" ? "UNKNOWN • Novidades" : "UNKNOWN • Comunicado");
    const body = interaction.options.getString("mensagem", true).trim();
    if (!body || !title) return editEmbedReply(interaction, "O título e a mensagem não podem ficar vazios.");
    const embed = new EmbedBuilder().setTitle(title).setDescription(body)
      .setColor(EMBED_COLOR).setTimestamp()
      .setFooter({ text: "UNKNOWN • .gg/unknown" });
    const image = interaction.options.getAttachment("imagem");
    if (image) {
      if (!image.contentType?.startsWith("image/")) return editEmbedReply(interaction, "O anexo precisa ser uma imagem.");
      embed.setImage(image.url);
    }
    await sendEmbed(channel, { embeds: [embed], allowedMentions: { parse: [] } });
  }
  await editEmbedReply(interaction, `Publicação enviada para <#${channelId}>.`);
}

export async function handleServerSetup(interaction, context) {
  if (!isAdmin(interaction.member, context.config)) return replyEphemeral(interaction, "Somente administradores ou o cargo unknown podem configurar o servidor.");
  await interaction.deferReply({ ephemeral: true });
  await setupServer(interaction.guild, context);
  await editEmbedReply(interaction, "Painéis, verificação e permissões atualizados.");
}
