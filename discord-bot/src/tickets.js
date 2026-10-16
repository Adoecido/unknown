import { randomUUID } from "node:crypto";
import {
  ActionRowBuilder,
  AttachmentBuilder,
  ButtonBuilder,
  ButtonStyle,
  ChannelType,
  EmbedBuilder,
  ModalBuilder,
  PermissionFlagsBits,
  TextInputBuilder,
  TextInputStyle,
  UserSelectMenuBuilder,
} from "discord.js";
import { canManageTicket, isStaff, isAdmin, replyEphemeral } from "./discord-utils.js";
import { clipText, makeTicketChannelName } from "./text.js";
import { EMBED_COLOR, sendEmbed, editEmbedReply, editEmbedMessage } from "./embeds.js";

const ticketButtons = new ActionRowBuilder().addComponents(
  new ButtonBuilder().setCustomId("ticket:add-user").setLabel("Adicionar pessoa").setEmoji("➕").setStyle(ButtonStyle.Primary),
  new ButtonBuilder().setCustomId("ticket:remove-user").setLabel("Remover pessoa").setEmoji("➖").setStyle(ButtonStyle.Secondary),
  new ButtonBuilder().setCustomId("ticket:close").setLabel("Fechar ticket").setEmoji("🔒").setStyle(ButtonStyle.Danger),
);

export async function enableTicketCommands(guild, store) {
  const tickets = await store.getOpenTickets(guild.id);
  let updated = 0;
  for (const ticket of tickets) {
    const channel = await guild.channels.fetch(ticket.channelId).catch((error) => {
      if (error.code === 10003) return null;
      throw error;
    });
    if (!channel || channel.type !== ChannelType.GuildText) continue;
    const ids = new Set([guild.roles.everyone.id]);
    for (const overwrite of channel.permissionOverwrites.cache.values()) {
      if (overwrite.allow.has(PermissionFlagsBits.ViewChannel) && !overwrite.deny.has(PermissionFlagsBits.SendMessages)) ids.add(overwrite.id);
    }
    for (const id of ids) {
      const type = id === guild.roles.everyone.id ? 0 : channel.permissionOverwrites.cache.get(id).type;
      await channel.permissionOverwrites.edit(id, { UseApplicationCommands: true }, {
        type, reason: "Comandos Minecraft disponíveis aos participantes do ticket",
      });
    }
    updated++;
  }
  return updated;
}

function ticketPanelEmbed() {
  return new EmbedBuilder()
    .setColor(EMBED_COLOR)
    .setTitle("UNKNOWN • Central de compras")
    .setDescription("Escolha o tipo de atendimento abaixo.\n\n**Buy (solo)** — compra individual.\n**Buy (clan)** — compra para seu clan; você poderá convidar os integrantes ao abrir o ticket.")
    .setFooter({ text: "Atendimento privado • Equipe helper / unknown" });
}

export function ticketPanelPayload() {
  return { embeds: [ticketPanelEmbed()], components: [new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId("ticket:create:solo").setLabel("Buy (solo)").setEmoji("🛒").setStyle(ButtonStyle.Primary),
    new ButtonBuilder().setCustomId("ticket:create:clan").setLabel("Buy (clan)").setEmoji("👥").setStyle(ButtonStyle.Success),
  )] };
}

function ticketEmbed(ticket) {
  return new EmbedBuilder()
    .setColor(EMBED_COLOR)
    .setTitle(`Ticket #${ticket.number}`)
    .setDescription("Explique seu caso com o máximo de detalhes possível. A equipe de suporte responderá em breve.")
    .addFields(
      { name: "Criado por", value: `<@${ticket.ownerId}>`, inline: true },
      { name: "Compra", value: ticket.type === "clan" ? "Buy (clan)" : "Buy (solo)", inline: true },
      { name: "Assunto", value: clipText(ticket.subject, 1024), inline: false },
    )
    .setTimestamp(new Date(ticket.createdAt));
}

function ticketModal(type = "solo") {
  const subjectInput = new TextInputBuilder()
    .setCustomId("subject")
    .setLabel("Qual é o assunto do ticket?")
    .setPlaceholder(type === "clan" ? "Qual clan e o que vocês querem comprar?" : "O que você quer comprar?")
    .setStyle(TextInputStyle.Short)
    .setMinLength(4)
    .setMaxLength(200)
    .setRequired(true);

  return new ModalBuilder()
    .setCustomId(`ticket:create:${type}`)
    .setTitle(type === "clan" ? "Buy (clan)" : "Buy (solo)")
    .addComponents(new ActionRowBuilder().addComponents(subjectInput));
}

async function findTicket(interaction, store) {
  const ticket = await store.getTicketByChannel(interaction.channelId);
  if (!ticket) {
    await replyEphemeral(interaction, "Este comando só pode ser usado dentro de um ticket aberto.");
    return null;
  }
  if (ticket.status === "closing") {
    await replyEphemeral(interaction, "Este ticket já está sendo fechado.");
    return null;
  }
  return ticket;
}

async function findManageableTicket(interaction, store, config) {
  const ticket = await findTicket(interaction, store);
  if (!ticket) return null;
  if (!canManageTicket(interaction.member, ticket, config)) {
    await replyEphemeral(interaction, "Somente quem abriu o ticket ou um membro da equipe pode gerenciá-lo.");
    return null;
  }
  return ticket;
}

async function getTicketCategory(guild, config) {
  if (!config.ticketCategoryId) {
    const panel = await guild.channels.fetch(config.channels.tickets);
    return panel?.parent?.type === ChannelType.GuildCategory ? panel.parent : null;
  }
  const category = await guild.channels.fetch(config.ticketCategoryId).catch(() => null);
  return category?.type === ChannelType.GuildCategory ? category : null;
}

async function createTicket(interaction, subject, { config, store }, type = "solo") {
  if (!isStaff(interaction.member, config) && !interaction.member.roles.cache.has(config.verifiedRoleId)) {
    return replyEphemeral(interaction, `Reaja à mensagem em <#${config.channels.home}> antes de abrir um ticket.`);
  }
  await interaction.deferReply({ ephemeral: true });
  const category = await getTicketCategory(interaction.guild, config);
  if (!category) {
    await editEmbedReply(interaction, "O sistema de tickets ainda não está configurado: defina um `TICKET_CATEGORY_ID` válido.");
    return;
  }

  const reservation = await store.reserveTicket({
    id: randomUUID(),
    guildId: interaction.guildId,
    ownerId: interaction.user.id,
    subject,
    type,
    maximumOpenTickets: config.ticketMaxOpenPerUser,
    defaultFilterWords: config.defaultFilterWords,
  });

  if (!reservation) {
    await editEmbedReply(interaction, `Você já possui ${config.ticketMaxOpenPerUser} ticket(s) aberto(s). Feche um antes de criar outro.`);
    return;
  }

  let channel;
  try {
    const permissionOverwrites = [
      { id: interaction.guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel], allow: [PermissionFlagsBits.UseApplicationCommands] },
      {
        id: interaction.client.user.id,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ManageRoles, PermissionFlagsBits.AttachFiles,
          PermissionFlagsBits.EmbedLinks, PermissionFlagsBits.MentionEveryone],
      },
      {
        id: interaction.user.id,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.UseApplicationCommands],
      },
      ...config.staffRoleIds.map((roleId) => ({
        id: roleId,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.UseApplicationCommands],
      })),
    ];

    channel = await interaction.guild.channels.create({
      name: makeTicketChannelName(reservation.number, interaction.user.username),
      type: ChannelType.GuildText,
      parent: category.id,
      topic: `Ticket #${reservation.number} — criado por ${interaction.user.id}`,
      permissionOverwrites,
      reason: `Ticket #${reservation.number} criado por ${interaction.user.tag}`,
    });

    const ticket = await store.finalizeTicket(reservation.id, channel.id);
    if (!ticket) throw new Error("Reserva do ticket não encontrada após criar o canal.");

    await sendEmbed(channel, {
      mentionContent: `<@${ticket.ownerId}> ${config.staffRoleIds.map((id) => `<@&${id}>`).join(" ")}`,
      embeds: [ticketEmbed(ticket)],
      components: [ticketButtons],
      allowedMentions: { users: [ticket.ownerId], roles: config.staffRoleIds },
    });
    if (type === "clan") {
      await editEmbedReply(interaction, { content: `Ticket Buy (clan) criado: ${channel}. Escolha abaixo quem deseja convidar (opcional). Você também pode usar o botão no ticket depois.`,
        components: [new ActionRowBuilder().addComponents(new UserSelectMenuBuilder()
          .setCustomId(`ticket:clan-invite:${channel.id}`).setPlaceholder("Convide os integrantes do clan")
          .setMinValues(1).setMaxValues(10))] });
    } else {
      await editEmbedReply(interaction, `Ticket Buy (solo) criado: ${channel}`);
    }
  } catch (error) {
    if (channel) {
      await channel.delete("Falha ao finalizar a criação do ticket").catch(() => undefined);
    }
    await store.removeTicket(reservation.id);
    console.error("Não foi possível criar o ticket:", error);
    await editEmbedReply(interaction, "Não consegui criar o ticket. Verifique se o bot tem permissões para criar canais e gerenciar permissões.");
  }
}

async function addParticipant(interaction, user, { config, store }) {
  const ticket = await findManageableTicket(interaction, store, config);
  if (!ticket) return;
  if (user.bot) {
    await replyEphemeral(interaction, "Bots não podem ser adicionados como participantes de ticket.");
    return;
  }
  await interaction.deferReply({ ephemeral: true });
  const member = await interaction.guild.members.fetch(user.id).catch(() => null);
  if (!member) return editEmbedReply(interaction, "Esta pessoa não está neste servidor.");

  const channel = interaction.channel;
  await channel.permissionOverwrites.edit(user.id, {
    ViewChannel: true,
    SendMessages: true,
    ReadMessageHistory: true,
    UseApplicationCommands: true,
  }, `Participante adicionado ao ticket #${ticket.number} por ${interaction.user.tag}`);

  const result = await store.addParticipant(ticket.id, user.id);
  const message = result.changed
    ? `<@${user.id}> foi adicionado(a) ao ticket.`
    : `<@${user.id}> já tinha acesso a este ticket.`;
  await editEmbedReply(interaction, message);
}

async function removeParticipant(interaction, user, { config, store }) {
  const ticket = await findManageableTicket(interaction, store, config);
  if (!ticket) return;

  const targetMember = await interaction.guild.members.fetch(user.id).catch(() => null);
  if (targetMember && isStaff(targetMember, config)) {
    await replyEphemeral(interaction, "Esta pessoa faz parte da equipe e mantém acesso pelo cargo. Remova o cargo para revogar o acesso.");
    return;
  }

  const stored = await store.getTicketByChannel(interaction.channelId);
  if (stored?.ownerId === user.id) {
    await replyEphemeral(interaction, "Não é possível remover quem abriu o ticket. Use o botão de fechar se o atendimento terminou.");
    return;
  }
  await interaction.deferReply({ ephemeral: true });

  await interaction.channel.permissionOverwrites.delete(user.id, `Participante removido do ticket #${ticket.number} por ${interaction.user.tag}`);
  const result = await store.removeParticipant(ticket.id, user.id);
  await editEmbedReply(interaction, result.changed
    ? `<@${user.id}> foi removido(a) do ticket.`
    : `<@${user.id}> não era um participante adicional deste ticket.`);
}

async function createTranscript(channel, maximumMessages) {
  const messages = [];
  let before;

  while (messages.length < maximumMessages) {
    const remaining = Math.min(100, maximumMessages - messages.length);
    const batch = await channel.messages.fetch({ limit: remaining, ...(before ? { before } : {}) });
    if (batch.size === 0) break;
    messages.push(...batch.values());
    const oldest = [...batch.values()].sort((left, right) => left.createdTimestamp - right.createdTimestamp)[0];
    before = oldest.id;
    if (batch.size < remaining) break;
  }

  messages.sort((left, right) => left.createdTimestamp - right.createdTimestamp);
  const header = [
    `Transcript de #${channel.name}`,
    `Canal: ${channel.id}`,
    `Gerado em: ${new Date().toISOString()}`,
    "",
  ];
  const lines = messages.flatMap((message) => {
    const base = `[${new Date(message.createdTimestamp).toISOString()}] ${message.author.tag} (${message.author.id}): ${message.cleanContent || "[sem texto]"}`;
    const attachments = [...message.attachments.values()].map((attachment) => `  Anexo: ${attachment.url}`);
    return [base, ...attachments];
  });
  return Buffer.from([...header, ...lines].join("\n"), "utf8");
}

async function archiveTicket({ channel, ticket, closer, config }) {
  if (!config.ticketLogChannelId) return false;
  const logChannel = await channel.guild.channels.fetch(config.ticketLogChannelId).catch(() => null);
  if (!logChannel?.isTextBased()) return false;

  const transcript = await createTranscript(channel, config.ticketTranscriptMessageLimit);
  const attachment = new AttachmentBuilder(transcript, { name: `ticket-${ticket.number}-${channel.id}.txt` });
  const participants = [ticket.ownerId, ...ticket.participantIds].map((id) => `<@${id}>`).join(", ");
  const embed = new EmbedBuilder()
    .setColor(EMBED_COLOR)
    .setTitle(`Ticket #${ticket.number} fechado`)
    .addFields(
      { name: "Assunto", value: clipText(ticket.subject, 1024) },
      { name: "Aberto por", value: `<@${ticket.ownerId}>`, inline: true },
      { name: "Fechado por", value: `<@${closer.id}>`, inline: true },
      { name: "Participantes", value: clipText(participants, 1024), inline: false },
    )
    .setTimestamp();
  await sendEmbed(logChannel, { embeds: [embed], files: [attachment] });
  return true;
}

async function closeTicket(interaction, { config, store, minecraft, unknownSync }) {
  const ticket = await findManageableTicket(interaction, store, config);
  if (!ticket) return;

  await interaction.deferReply({ ephemeral: true });
  const closingTicket = await store.markTicketClosing(ticket.id);
  if (!closingTicket) {
    await editEmbedReply(interaction, "Este ticket já está sendo fechado.");
    return;
  }
  minecraft?.stop(interaction.channelId);
  await unknownSync?.clear(interaction.guildId, interaction.channelId).catch(() => undefined);
  let archived = false;
  try {
    archived = await archiveTicket({ channel: interaction.channel, ticket: closingTicket, closer: interaction.user, config });
  } catch (error) {
    console.error("Não foi possível gerar o transcript do ticket:", error);
  }

  await editEmbedReply(interaction, archived
    ? `Ticket será fechado em ${config.ticketCloseDelaySeconds} segundos. O transcript foi enviado ao canal de logs.`
    : `Ticket será fechado em ${config.ticketCloseDelaySeconds} segundos. Nenhum transcript foi enviado; confira TICKET_LOG_CHANNEL_ID.`);

  setTimeout(async () => {
    try {
      await interaction.channel.delete(`Ticket #${ticket.number} fechado por ${interaction.user.tag}`);
      await store.removeTicket(ticket.id);
    } catch (error) {
      await store.reopenTicket(ticket.id).catch(() => undefined);
      console.error("Não foi possível apagar o canal do ticket:", error.message);
    }
  }, config.ticketCloseDelaySeconds * 1000);
}

export async function handleTicketCommand(interaction, context) {
  const subcommand = interaction.options.getSubcommand();
  if (subcommand === "painel") {
    if (!isStaff(interaction.member, context.config)) {
      await replyEphemeral(interaction, "Somente a equipe configurada ou administradores podem enviar o painel.");
      return;
    }
    const channel = interaction.options.getChannel("canal", true);
    if (channel.id !== context.config.channels.tickets) {
      return replyEphemeral(interaction, `O painel deve ficar em <#${context.config.channels.tickets}>.`);
    }
    await interaction.deferReply({ ephemeral: true });
    const settings = await context.store.getGuildSettings(interaction.guildId);
    const existing = settings.ticketPanelMessageId ? await channel.messages.fetch(settings.ticketPanelMessageId).catch(() => null) : null;
    const message = existing ? await editEmbedMessage(existing, ticketPanelPayload()) : await sendEmbed(channel, ticketPanelPayload());
    await context.store.updateGuildSettings(interaction.guildId, { ticketPanelMessageId: message.id });
    await replyEphemeral(interaction, `Painel de tickets enviado em ${channel}.`);
    return;
  }

  if (subcommand === "abrir") {
    await interaction.showModal(ticketModal(interaction.options.getString("tipo", true)));
    return;
  }
  if (subcommand === "adicionar") {
    await addParticipant(interaction, interaction.options.getUser("membro", true), context);
    return;
  }
  if (subcommand === "remover") {
    await removeParticipant(interaction, interaction.options.getUser("membro", true), context);
    return;
  }
  if (subcommand === "fechar") {
    await closeTicket(interaction, context);
    return;
  }

  const ticket = await findTicket(interaction, context.store);
  if (!ticket) return;
  const participants = ticket.participantIds.length ? ticket.participantIds.map((id) => `<@${id}>`).join(", ") : "Nenhum";
  await replyEphemeral(interaction, {
    embeds: [new EmbedBuilder()
      .setColor(EMBED_COLOR)
      .setTitle(`Ticket #${ticket.number}`)
      .addFields(
        { name: "Criado por", value: `<@${ticket.ownerId}>`, inline: true },
        { name: "Assunto", value: clipText(ticket.subject, 1024), inline: false },
        { name: "Participantes adicionais", value: clipText(participants, 1024), inline: false },
      )
      .setTimestamp(new Date(ticket.createdAt))],
  });
}

export async function handleTicketButton(interaction, context) {
  if (interaction.customId.startsWith("ticket:create")) {
    if (!isStaff(interaction.member, context.config) && !interaction.member.roles.cache.has(context.config.verifiedRoleId)) {
      return replyEphemeral(interaction, `Reaja à mensagem em <#${context.config.channels.home}> antes de abrir um ticket.`);
    }
    await interaction.showModal(ticketModal(interaction.customId.endsWith(":clan") ? "clan" : "solo"));
    return;
  }
  if (interaction.customId === "ticket:close") {
    await closeTicket(interaction, context);
    return;
  }

  const ticket = await findManageableTicket(interaction, context.store, context.config);
  if (!ticket) return;
  const adding = interaction.customId === "ticket:add-user";
  const select = new UserSelectMenuBuilder()
    .setCustomId(adding ? "ticket:add-user:selected" : "ticket:remove-user:selected")
    .setPlaceholder(adding ? "Selecione quem poderá entrar no ticket" : "Selecione quem perderá o acesso")
    .setMinValues(1)
    .setMaxValues(1);
  await replyEphemeral(interaction, {
    content: adding ? "Escolha a pessoa que será adicionada:" : "Escolha a pessoa que será removida:",
    components: [new ActionRowBuilder().addComponents(select)],
  });
}

export async function handleTicketUserSelect(interaction, context) {
  if (interaction.customId.startsWith("ticket:clan-invite:")) {
    await interaction.deferReply({ ephemeral: true });
    const channelId = interaction.customId.split(":")[2];
    const ticket = await context.store.getTicketByChannel(channelId);
    if (!ticket || ticket.status !== "open" || ticket.type !== "clan" || !canManageTicket(interaction.member, ticket, context.config)) {
      return editEmbedReply(interaction, "Este convite não está mais disponível para você.");
    }
    const channel = await interaction.guild.channels.fetch(channelId);
    let added = 0;
    for (const user of interaction.users.values()) {
      if (user.bot || user.id === ticket.ownerId) continue;
      const member = await interaction.guild.members.fetch(user.id).catch(() => null);
      if (!member) continue;
      await channel.permissionOverwrites.edit(user.id, { ViewChannel: true, SendMessages: true, ReadMessageHistory: true, UseApplicationCommands: true });
      await context.store.addParticipant(ticket.id, user.id);
      added += 1;
    }
    return editEmbedReply(interaction, `${added} participante(s) convidado(s) para <#${channelId}>.`);
  }
  const selectedUser = interaction.users.first();
  if (!selectedUser) {
    await replyEphemeral(interaction, "Não foi possível identificar a pessoa selecionada.");
    return;
  }
  if (interaction.customId === "ticket:add-user:selected") {
    await addParticipant(interaction, selectedUser, context);
  } else if (interaction.customId === "ticket:remove-user:selected") {
    await removeParticipant(interaction, selectedUser, context);
  }
}

export async function handleTicketModal(interaction, context) {
  const subject = interaction.fields.getTextInputValue("subject").trim();
  if (subject.length < 4) return replyEphemeral(interaction, "Informe um assunto com pelo menos quatro caracteres.");
  await createTicket(interaction, subject, context, interaction.customId.endsWith(":clan") ? "clan" : "solo");
}

export async function handleTicketAnnouncement(interaction, { store, config }) {
  if (!isAdmin(interaction.member, config)) {
    await replyEphemeral(interaction, "Somente administradores podem enviar anúncios para todos os tickets.");
    return;
  }
  const botMember = interaction.guild.members.me ?? await interaction.guild.members.fetchMe();
  if (!botMember.permissions.has(PermissionFlagsBits.MentionEveryone)) {
    await replyEphemeral(interaction, "Não consigo marcar `@everyone`: conceda ao bot a permissão **Mencionar @everyone, @here e todos os cargos**.");
    return;
  }

  const text = interaction.options.getString("mensagem", true)
    .trim()
    .replace(/@(everyone|here)/giu, "@\u200b$1");
  if (!text) return replyEphemeral(interaction, "A mensagem não pode ficar vazia.");
  const tickets = await store.getOpenTickets(interaction.guildId);
  if (tickets.length === 0) {
    await replyEphemeral(interaction, "Não há tickets abertos para receber o anúncio.");
    return;
  }

  await interaction.deferReply({ ephemeral: true });
  let sent = 0;
  let failed = 0;
  for (const ticket of tickets) {
    const channel = await interaction.guild.channels.fetch(ticket.channelId).catch(() => null);
    if (!channel?.isTextBased()) {
      failed += 1;
      continue;
    }
    try {
      await sendEmbed(channel, {
        mentionContent: "@everyone",
        embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle("UNKNOWN • Aviso aos tickets").setDescription(text)],
        allowedMentions: { parse: ["everyone"] },
      });
      sent += 1;
    } catch (error) {
      failed += 1;
      console.error(`Não foi possível anunciar no ticket #${ticket.number}:`, error.message);
    }
  }

  await editEmbedReply(interaction,
    failed === 0
      ? `Anúncio enviado com @everyone para ${sent} ticket(s) aberto(s).`
      : `Anúncio enviado para ${sent} ticket(s); ${failed} canal(is) não puderam receber a mensagem. Confira as permissões do bot.`,
  );
}
