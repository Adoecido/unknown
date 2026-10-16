import { EmbedBuilder, PermissionFlagsBits } from "discord.js";
import { replyEphemeral, requireUserPermission, writeModerationLog } from "./discord-utils.js";
import { clipText, findBlockedPhrase, normalizeFilterPhrase } from "./text.js";
import { EMBED_COLOR } from "./embeds.js";

function reasonFrom(interaction) {
  return interaction.options.getString("motivo")?.trim() || "Não informado";
}

function auditReason(interaction, reason) {
  return `Por ${interaction.user.tag}: ${reason}`.slice(0, 512);
}

async function getBotMember(guild) {
  return guild.members.me ?? guild.members.fetchMe();
}

async function requireBotPermission(interaction, permission, explanation) {
  const botMember = await getBotMember(interaction.guild);
  if (botMember.permissions.has(permission)) return true;
  await replyEphemeral(interaction, `Não consigo executar esta ação: preciso da permissão **${explanation}**.`);
  return false;
}

async function getTargetMember(interaction, user) {
  const member = interaction.options.getMember("membro") ?? await interaction.guild.members.fetch(user.id).catch(() => null);
  if (!member) {
    await replyEphemeral(interaction, "Não encontrei esse membro neste servidor.");
    return null;
  }
  if (member.id === interaction.user.id) {
    await replyEphemeral(interaction, "Você não pode aplicar esta ação em si mesmo.");
    return null;
  }
  if (member.id === interaction.guild.ownerId) {
    await replyEphemeral(interaction, "Não é possível moderar o dono do servidor.");
    return null;
  }
  return member;
}

async function logCase(context, interaction, values) {
  return writeModerationLog({
    guild: interaction.guild,
    config: context.config,
    store: context.store,
    moderatorId: interaction.user.id,
    ...values,
  });
}

async function kick(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.KickMembers, "Você precisa da permissão **Expulsar membros**.")) return;
  if (!await requireBotPermission(interaction, PermissionFlagsBits.KickMembers, "Expulsar membros")) return;
  const user = interaction.options.getUser("membro", true);
  const member = await getTargetMember(interaction, user);
  if (!member || !member.kickable) {
    if (member) await replyEphemeral(interaction, "Não posso expulsar esse membro. Confira a hierarquia dos cargos do bot.");
    return;
  }

  const reason = reasonFrom(interaction);
  await member.kick(auditReason(interaction, reason));
  const entry = await logCase(context, interaction, {
    action: "Kick",
    targetId: user.id,
    targetLabel: user.tag,
    reason,
    metadata: {},
  });
  await replyEphemeral(interaction, `${user.tag} foi expulso(a). Caso #${entry.number}.`);
}

async function ban(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.BanMembers, "Você precisa da permissão **Banir membros**.")) return;
  if (!await requireBotPermission(interaction, PermissionFlagsBits.BanMembers, "Banir membros")) return;
  const user = interaction.options.getUser("membro", true);
  const member = await getTargetMember(interaction, user);
  if (!member || !member.bannable) {
    if (member) await replyEphemeral(interaction, "Não posso banir esse membro. Confira a hierarquia dos cargos do bot.");
    return;
  }

  const reason = reasonFrom(interaction);
  await member.ban({ deleteMessageSeconds: 0, reason: auditReason(interaction, reason) });
  const entry = await logCase(context, interaction, {
    action: "Ban",
    targetId: user.id,
    targetLabel: user.tag,
    reason,
    metadata: {},
  });
  await replyEphemeral(interaction, `${user.tag} foi banido(a). Caso #${entry.number}.`);
}

async function softban(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.BanMembers, "Você precisa da permissão **Banir membros**.")) return;
  if (!await requireBotPermission(interaction, PermissionFlagsBits.BanMembers, "Banir membros")) return;
  const user = interaction.options.getUser("membro", true);
  const member = await getTargetMember(interaction, user);
  if (!member || !member.bannable) {
    if (member) await replyEphemeral(interaction, "Não posso executar softban nesse membro. Confira a hierarquia dos cargos do bot.");
    return;
  }

  const reason = reasonFrom(interaction);
  const detailedReason = auditReason(interaction, reason);
  await member.ban({ deleteMessageSeconds: 7 * 24 * 60 * 60, reason: detailedReason });
  try {
    await interaction.guild.bans.remove(user.id, detailedReason);
  } catch (error) {
    await replyEphemeral(interaction, `O ban foi aplicado, mas o desban falhou. ${user.tag} continua banido(a); remova o ban manualmente. (${error.message})`);
    return;
  }

  const entry = await logCase(context, interaction, {
    action: "Softban",
    targetId: user.id,
    targetLabel: user.tag,
    reason,
    metadata: { deletedMessagesWindow: "7 dias" },
  });
  await replyEphemeral(interaction, `Softban aplicado a ${user.tag}: mensagens recentes foram removidas e a conta foi desbanida. Caso #${entry.number}.`);
}

async function hackban(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.BanMembers, "Você precisa da permissão **Banir membros**.")) return;
  if (!await requireBotPermission(interaction, PermissionFlagsBits.BanMembers, "Banir membros")) return;
  const userId = interaction.options.getString("id", true).trim();
  if (!/^\d{17,20}$/.test(userId)) {
    await replyEphemeral(interaction, "O ID precisa ter entre 17 e 20 dígitos numéricos.");
    return;
  }
  if (userId === interaction.user.id || userId === interaction.guild.ownerId) {
    await replyEphemeral(interaction, "Não é possível banir essa conta por este comando.");
    return;
  }

  const reason = reasonFrom(interaction);
  await interaction.guild.bans.create(userId, { deleteMessageSeconds: 0, reason: auditReason(interaction, reason) });
  const entry = await logCase(context, interaction, {
    action: "Hackban",
    targetId: userId,
    targetLabel: `ID ${userId}`,
    reason,
    metadata: {},
  });
  await replyEphemeral(interaction, `A conta \`${userId}\` foi banida. Caso #${entry.number}.`);
}

async function clear(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.ManageMessages, "Você precisa da permissão **Gerenciar mensagens**.")) return;
  if (!await requireBotPermission(interaction, PermissionFlagsBits.ManageMessages, "Gerenciar mensagens")) return;
  const channel = interaction.channel;
  if (!channel?.isTextBased() || typeof channel.bulkDelete !== "function") {
    await replyEphemeral(interaction, "Este canal não permite limpeza de mensagens.");
    return;
  }

  const amount = interaction.options.getInteger("quantidade", true);
  const user = interaction.options.getUser("membro");
  const messages = await channel.messages.fetch({ limit: amount });
  const selected = user ? messages.filter((message) => message.author.id === user.id) : messages;
  if (selected.size === 0) {
    await replyEphemeral(interaction, "Não encontrei mensagens compatíveis para apagar entre as mais recentes.");
    return;
  }

  const deleted = await channel.bulkDelete(selected, true);
  const entry = await logCase(context, interaction, {
    action: "Limpeza de chat",
    targetId: user?.id ?? null,
    targetLabel: user?.tag ?? "Vários membros",
    reason: user ? `Limpeza de até ${amount} mensagens de ${user.tag}` : `Limpeza de até ${amount} mensagens`,
    metadata: { channelId: channel.id, deletedCount: deleted.size },
  });
  await replyEphemeral(interaction, `${deleted.size} mensagem(ns) apagada(s). Caso #${entry.number}. Mensagens com mais de 14 dias são ignoradas pelo Discord.`);
}

async function history(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.ModerateMembers, "Você precisa da permissão **Moderar membros**.")) return;
  const user = interaction.options.getUser("membro", true);
  const cases = await context.store.getModerationCases(interaction.guildId, user.id, 10, context.config.defaultFilterWords);
  if (cases.length === 0) {
    await replyEphemeral(interaction, `Não há casos registrados para ${user.tag}.`);
    return;
  }
  const description = cases.map((entry) => (
    `**#${entry.number} · ${entry.action}** — <t:${Math.floor(new Date(entry.createdAt).getTime() / 1000)}:R>\n${clipText(entry.reason, 180)}`
  )).join("\n\n");
  await replyEphemeral(interaction, {
    embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle(`Histórico de ${user.tag}`).setDescription(description)],
  });
}

export async function handleModerationCommand(interaction, context) {
  const subcommand = interaction.options.getSubcommand();
  const handlers = { kick, ban, softban, hackban, limpar: clear, historico: history };
  await handlers[subcommand](interaction, context);
}

export async function handleConfigCommand(interaction, context) {
  if (!await requireUserPermission(interaction, PermissionFlagsBits.ManageGuild, "Você precisa da permissão **Gerenciar servidor**.")) return;
  const subcommand = interaction.options.getSubcommand();
  const settings = await context.store.getGuildSettings(interaction.guildId, context.config.defaultFilterWords);

  if (subcommand === "listar") {
    const content = settings.filterWords.length
      ? settings.filterWords.map((phrase, index) => `${index + 1}. ${phrase}`).join("\n")
      : "Nenhuma frase está bloqueada.";
    await replyEphemeral(interaction, { embeds: [new EmbedBuilder().setColor(EMBED_COLOR).setTitle("Filtro de mensagens").setDescription(clipText(content, 4096))] });
    return;
  }

  const phrase = interaction.options.getString("frase", true).trim();
  if (normalizeFilterPhrase(phrase).length < 2) {
    await replyEphemeral(interaction, "Informe uma palavra ou frase válida, com pelo menos dois caracteres úteis.");
    return;
  }
  if (subcommand === "adicionar" && settings.filterWords.length >= 250) {
    await replyEphemeral(interaction, "O filtro atingiu o limite de 250 frases. Remova uma antes de adicionar outra.");
    return;
  }

  const result = subcommand === "adicionar"
    ? await context.store.addFilterPhrase(interaction.guildId, phrase, context.config.defaultFilterWords)
    : await context.store.removeFilterPhrase(interaction.guildId, phrase, context.config.defaultFilterWords);
  await replyEphemeral(interaction, result.added || result.removed
    ? `Filtro atualizado: “${phrase}”.`
    : subcommand === "adicionar" ? "Essa frase já está no filtro." : "Essa frase não estava no filtro.");
}

export async function handleFilterMessage(message, context) {
  if (!message.inGuild() || message.author.bot || !message.content) return false;
  if (message.member?.permissions.has(PermissionFlagsBits.Administrator)
    || context.config.staffRoleIds.some((roleId) => message.member?.roles.cache.has(roleId))) return;

  const settings = await context.store.getGuildSettings(message.guildId, context.config.defaultFilterWords);
  const phrase = findBlockedPhrase(message.content, settings.filterWords);
  if (!phrase || !message.deletable) return false;

  try {
    await message.delete();
  } catch (error) {
    console.error("Não foi possível apagar mensagem filtrada:", error.message);
    return;
  }
  await writeModerationLog({
    guild: message.guild,
    config: context.config,
    store: context.store,
    action: "Filtro",
    moderatorId: message.client.user.id,
    targetId: message.author.id,
    targetLabel: message.author.tag,
    reason: `Mensagem removida pelo filtro automático (${clipText(phrase, 100)}).`,
    metadata: { channelId: message.channel.id },
  });
  return true;
}
