import { EmbedBuilder, PermissionFlagsBits } from "discord.js";
import { isAdmin, replyEphemeral } from "./discord-utils.js";
import { DURABILITY_THRESHOLD } from "../vendor/bot-invsee/src/armor.js";
import { clipText } from "./text.js";
import { EMBED_COLOR, sendEmbed, replyEmbed, editEmbedReply, editEmbedMessage } from "./embeds.js";
import { setTimeout as delay } from "node:timers/promises";

const durabilityLabel = `${Math.round(DURABILITY_THRESHOLD * 100)}%`;

export function describeMinecraftFailure(status = {}) {
  if (status.errorCode === "CONNECTION_RATE_LIMIT") return "O servidor pediu mais tempo entre conexões Minecraft. Aguarde alguns segundos e tente novamente.";
  if (status.errorCode === "INVALID_PASSWORD") return "O servidor recusou a senha da conta. A configuração usa a regra nick + 123.";
  if (status.verificationRequired) {
    return `${status.authenticated ? "Login confirmado, mas" : "Conexão estabelecida, mas o login ainda não foi confirmado:"} o servidor exige captcha/verificação adicional. ${status.mode === "lobby" ? "O teste de lobby não automatiza essa verificação." : "A verificação ainda não foi concluída."}`;
  }
  if (status.errorCode && status.errorCode !== "SESSION_TIMEOUT") {
    return `Falha de rede (${status.errorCode}). A sessão Minecraft não ficou pronta.`;
  }
  if (status.phase === "disconnected") return "O servidor encerrou a conexão antes de concluir a sessão Minecraft.";
  if (status.waitingInQueue) return "Login confirmado, mas a conta ainda está na fila para entrar no P4Free. O servidor não confirmou a entrada no tempo limite.";
  if (status.authenticated) return "Login confirmado no lobby, mas a entrada no P4Free não foi concluída. O modo pode estar fechado ou indisponível. Para testar só o lobby, use modo:lobby.";
  if (status.connected) return "A conexão foi estabelecida, mas o servidor não confirmou o login no tempo limite.";
  return "A conexão Minecraft não foi concluída no tempo limite.";
}

async function requireTicketAccess(interaction, context) {
  const ticket = await context.store.getTicketByChannel(interaction.channelId);
  const permissions = interaction.channel?.permissionsFor(interaction.member);
  if (!ticket || ticket.status !== "open" || !permissions?.has([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages])) {
    await replyEphemeral(interaction, "Este comando está disponível apenas para participantes de um ticket aberto.");
    return null;
  }
  return ticket;
}

function playerEmbed(result) {
  if (!result.ok) throw new Error(result.notReady ? "A conta ainda está concluindo o login e a seleção do P4Free." : "Não consegui ler o inventário: jogador offline, sem permissão ou sem resposta do servidor.");
  const totals = result.analysis.totals;
  return new EmbedBuilder().setTitle(`Inventário de ${result.player}`).setColor(EMBED_COLOR)
    .setDescription(`**${result.sets} set(s)** com durabilidade ≥ ${durabilityLabel}.\n${result.dead ? "Inventário vazio ou apenas com espada; possível morte." : ""}`)
    .addFields({ name: "Capacetes", value: String(totals.helmet), inline: true },
      { name: "Peitorais", value: String(totals.chestplate), inline: true },
      { name: "Calças", value: String(totals.leggings), inline: true },
      { name: "Botas", value: String(totals.boots), inline: true },
      { name: "Heads (cada uma conta como 1 set)", value: String(result.analysis.heads), inline: false }).setTimestamp();
}

export function syncReport(results, interval) {
  return {
    players: results.filter((result) => result.ok && !result.dead)
      .map((result) => {
        const totals = result.analysis?.totals;
        return {
          name: result.player,
          sets: result.sets,
          ...(totals ? {
            helmet: totals.helmet ?? 0,
            chestplate: totals.chestplate ?? 0,
            leggings: totals.leggings ?? 0,
            boots: totals.boots ?? 0,
          } : {}),
        };
      }).sort((a, b) => a.sets - b.sets),
    // Invsee identifies *possible* deaths. Only the local entity can confirm death.
    deadPlayers: [],
    ttlSeconds: Math.min(900, Math.max(180, (interval ?? 60) * 2 + 60)),
  };
}

async function clearSynchronization(context, session, guildId, channelId) {
  if (session) { session.syncGeneration = (session.syncGeneration ?? 0) + 1; session.latestInvseeReport = null; }
  await context.unknownSync?.clear(guildId, channelId).catch(() => undefined);
}

async function clanReport(context, session, tag, interval, guildId) {
  const generation = session.syncGeneration ?? 0;
  const roster = await session.api.getClanRoster(tag);
  if (!roster.ok) throw new Error(roster.notReady ? "A conta ainda está concluindo o login e a seleção do P4Free." : "Não consegui obter a lista de membros do clan.");
  if (roster.online.length > 100) throw new Error("O clan possui mais de 100 membros online. Consulte jogadores individualmente.");
  const results = await session.api.invseeMany(roster.online);
  const alive = results.filter((result) => result.ok && !result.dead).sort((a, b) => a.sets - b.sets);
  const dead = results.filter((result) => result.ok && result.dead).map((result) => result.player);
  const failed = [...roster.offline, ...results.filter((result) => !result.ok).map((result) => result.player)];
  const ranking = alive.map((result) => `${result.player}: ${result.sets} set(s)`).join("\n");
  let syncState = "";
  if (!session.closed && generation === (session.syncGeneration ?? 0)) {
    session.latestInvseeReport = { report: syncReport(results, interval), createdAt: Date.now() };
    if (context.unknownSync) {
      try {
        await context.unknownSync.publish(guildId, session.channelId, session.latestInvseeReport.report);
        syncState = " • Target atualizado";
      } catch { syncState = " • Sync indisponível (client suspende ao vencer o relatório)"; }
    }
  }
  const body = [`Durabilidade mínima: ${durabilityLabel}.`, alive.length ? `Menos sets: **${alive[0].player} (${alive[0].sets})**` : "Nenhum membro com sets analisado.",
    "", "**Ranking**", ranking || "Sem resultados.", "", `**Sem sets:** ${alive.filter((result) => result.sets === 0).map((result) => result.player).join(", ") || "Nenhum"}`,
    `**Possível morte:** ${dead.join(", ") || "Nenhum"}`, `**Offline/sem resposta:** ${failed.join(", ") || "Nenhum"}`].join("\n");
  if (session.warnEnabled && !session.closed && alive.length) {
    const minimumSets = alive[0].sets;
    let warned = false;
    for (const result of alive.filter((entry) => entry.sets === minimumSets)) {
      const totals = result.analysis?.totals;
      if (!totals) continue;
      if (warned) await delay(500);
      if (session.closed || !session.warnEnabled || generation !== (session.syncGeneration ?? 0)) break;
      const sent = await session.api.sendChatCommand(`/. ${result.player} com menos sets: ${result.sets} set(s) - ${totals.helmet} capa ${totals.chestplate} peito ${totals.leggings} calça ${totals.boots} bota`);
      if (!sent.ok) throw new Error("Não consegui enviar o aviso de clan: a conta Minecraft não está pronta.");
      warned = true;
    }
  }
  return new EmbedBuilder().setTitle(`Clan ${tag}`).setColor(EMBED_COLOR).setDescription(clipText(body, 4096))
    .setFooter({ text: (interval ? `Conta: ${session.nick} • Atualiza a cada ${interval}s, após concluir a consulta` : `Conta: ${session.nick}`) + syncState }).setTimestamp();
}

function stopAuto(session) {
  if (session.auto) clearTimeout(session.auto.timer);
  session.auto = null;
}

function scheduleAuto(context, session, tracker, channel) {
  tracker.timer = setTimeout(async () => {
    if (session.closed || session.auto !== tracker) return;
    try {
      if (!session.busy) {
        const embed = await context.minecraft.run(session.channelId, (current) => clanReport(context, current, tracker.tag, tracker.seconds, channel.guildId));
        if (!session.closed && session.auto === tracker) await editEmbedMessage(tracker.message, { embeds: [embed], allowedMentions: { parse: [] } });
      }
    } catch (error) {
      stopAuto(session);
      await clearSynchronization(context, session, channel.guildId, session.channelId);
      if (!session.closed) await sendEmbed(channel, { content: `Atualização automática parada: ${error.message}`, allowedMentions: { parse: [] } }).catch(() => undefined);
      return;
    }
    if (!session.closed && session.auto === tracker) scheduleAuto(context, session, tracker, channel);
  }, tracker.seconds * 1000);
}

export async function handleInvseeCommand(interaction, context) {
  const ticket = await requireTicketAccess(interaction, context);
  if (!ticket) return;
  const command = interaction.commandName;
  if (command === "syncunknown") {
    await interaction.deferReply({ flags: 64 });
    try {
      const code = interaction.options.getString("id", true).trim();
      if (!/^[A-Za-z0-9]{30}$/.test(code)) throw new Error("Use o código de 30 caracteres copiado pelo Target.");
      if (!context.unknownSync) throw new Error("Sincronização não configurada no bot.");
      await context.unknownSync.bind(code, interaction.guildId, interaction.channelId);
      const cached = context.minecraft.get(interaction.channelId)?.latestInvseeReport;
      if (cached && Date.now() - cached.createdAt < cached.report.ttlSeconds * 1000) {
        const remaining = Math.floor(cached.report.ttlSeconds - (Date.now() - cached.createdAt) / 1000);
        if (remaining >= 30) await context.unknownSync.publish(interaction.guildId, interaction.channelId,
          { ...cached.report, ttlSeconds: remaining });
      }
      return editEmbedReply(interaction, "Client vinculado a este ticket. Use /invsee ou /invseeauto; o ranking aparecerá no Target.");
    } catch (error) { return editEmbedReply(interaction, { content: error.message, allowedMentions: { parse: [] } }); }
  }
  if (command === "botinvsee") {
    if (!isAdmin(interaction.member, context.config)) return replyEphemeral(interaction, "Somente a administração/cargo unknown pode iniciar uma conta no ticket.");
    await interaction.deferReply();
    let session;
    try {
      const mode = interaction.options.getString("modo") ?? "p4free";
      session = context.minecraft.start(interaction.channelId, interaction.options.getString("nick", true).trim(), mode);
      const ready = await session.api.waitForSession();
      if (!ready || session.closed || session.api.getStatus().connected === false) {
        const status = session.api.getStatus();
        console.log(`[Minecraft:${session.nick}] Inicialização incompleta: ${JSON.stringify(status)}`);
        context.minecraft.stop(interaction.channelId);
        return editEmbedReply(interaction, { content: `${describeMinecraftFailure(status)}\nSessão encerrada. Você pode tentar /botinvsee novamente.`, allowedMentions: { parse: [] } });
      }
      if (mode === "lobby") return editEmbedReply(interaction, `Conta **${session.nick}** com login confirmado no lobby. Nenhum acesso ao P4Free foi tentado. Invsee não está habilitado neste modo; para trocar, use /botinvsee-stop e inicie novamente sem modo:lobby.`);
      return editEmbedReply(interaction, `Conta **${session.nick}** com entrada confirmada no **P4Free**, conectada exclusivamente a este ticket. Todos os participantes podem usar /invsee, /invsee-player, /invseeauto, /clanwarn, /reconnect e .use <comando>.`);
    } catch (error) {
      if (session) context.minecraft.stop(interaction.channelId);
      return editEmbedReply(interaction, { content: error.message, allowedMentions: { parse: [] } });
    }
  }
  if (command === "botinvsee-stop") {
    const stopped = context.minecraft.stop(interaction.channelId);
    await clearSynchronization(context, null, interaction.guildId, interaction.channelId);
    return replyEphemeral(interaction, stopped ? "Conta Minecraft desligada neste ticket." : "Este ticket não tem uma conta ativa.");
  }
  if (command === "invseeauto-stop") {
    const session = context.minecraft.get(interaction.channelId);
    if (!session) return replyEphemeral(interaction, "Este ticket não tem uma conta ativa.");
    const active = Boolean(session.auto);
    stopAuto(session);
    await clearSynchronization(context, session, interaction.guildId, interaction.channelId);
    return replyEphemeral(interaction, active ? "Atualização automática parada neste ticket." : "Não há atualização automática neste ticket.");
  }
  await interaction.deferReply();
  try {
    await context.minecraft.run(interaction.channelId, async (session) => {
      if (command === "reconnect") {
        stopAuto(session);
        await clearSynchronization(context, session, interaction.guildId, interaction.channelId);
        const result = await session.api.reconnect();
        return editEmbedReply(interaction, result.ok ? `Conta reconectada e pronta no ${session.mode === "lobby" ? "lobby" : "P4Free"}.` : describeMinecraftFailure(session.api.getStatus()));
      }
      if (session.mode === "lobby") throw new Error("Esta conta está em modo de teste do lobby. Para usar Invsee/clan, desligue com /botinvsee-stop e inicie novamente no modo p4free.");
      if (command === "clanwarn") {
        const enabled = interaction.options.getString("estado", true) === "on";
        const nick = interaction.options.getString("nick");
        if (nick) {
          if (!/^[A-Za-z0-9_]{3,16}$/.test(nick)) throw new Error("Nick inválido.");
          const result = await session.api.sendChatCommand(`/tell warn ${nick} ${enabled ? "on" : "off"}`);
          if (!result.ok) throw new Error("A conta ainda não está pronta.");
          return editEmbedReply(interaction, `Aviso de ${nick} ${enabled ? "ligado" : "desligado"} neste ticket.`);
        }
        session.warnEnabled = enabled;
        return editEmbedReply(interaction, enabled
          ? "Aviso automático do clan ligado. Em cada /invsee ou atualização do /invseeauto, aviso no chat do clan quem tem menos sets e quantas capas, peitos, calças e botas possui. Em caso de empate, aviso todos os empatados."
          : "Aviso automático do clan desligado neste ticket.");
      }
      const target = interaction.options.getString(command === "invsee-player" ? "nick" : "tag", true);
      if (!/^[A-Za-z0-9_]{1,16}$/.test(target)) throw new Error("Use apenas letras, números ou sublinhados no nick/tag.");
      if (command === "invsee-player") {
        const embed = playerEmbed(await session.api.invseePlayer(target));
        return editEmbedReply(interaction, { embeds: [embed], allowedMentions: { parse: [] } });
      }
      const seconds = command === "invseeauto" ? interaction.options.getInteger("segundos") ?? 60 : null;
      if (seconds && session.auto) throw new Error("Já existe uma atualização ativa neste ticket. Use /invseeauto-stop primeiro.");
      const embed = await clanReport(context, session, target, seconds, interaction.guildId);
      if (session.closed) return;
      const message = await editEmbedReply(interaction, { embeds: [embed], allowedMentions: { parse: [] } });
      if (seconds) {
        session.auto = { tag: target, seconds, message, timer: null };
        scheduleAuto(context, session, session.auto, interaction.channel);
      }
    });
  } catch (error) {
    await editEmbedReply(interaction, { content: error.message, allowedMentions: { parse: [] } });
  }
}

export async function handleMinecraftText(message, context) {
  if (!message.inGuild() || message.author.bot || !/^\.use(?:\s|$)/i.test(message.content)) return false;
  const ticket = await context.store.getTicketByChannel(message.channelId);
  if (!ticket || ticket.status !== "open") return false;
  if (!message.channel.permissionsFor(message.member)?.has([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages])) return true;
  const command = message.content.slice(4).trim();
  if (!command || command.length > 256 || /[\r\n]/.test(command)) {
    await replyEmbed(message, "Use .use <comando>, em uma linha de até 256 caracteres.");
    return true;
  }
  try {
    await context.minecraft.run(message.channelId, async (session) => {
      const replies = [];
      const unsubscribe = session.api.onChatMessage((text) => {
        if (replies.length < 100) replies.push(text);
      });
      let timer;
      let resolveWindow;
      const cleanup = () => { clearTimeout(timer); unsubscribe(); resolveWindow?.(); session.cleanups.delete(cleanup); };
      session.cleanups.add(cleanup);
      try {
        const result = await session.api.sendChatCommand(command);
        if (!result.ok) throw new Error("A conta Minecraft ainda não está pronta.");
        await replyEmbed(message, { content: "Comando enviado à conta deste ticket. Coletando respostas por 10 segundos…", allowedMentions: { parse: [] } });
        if (session.closed) return;
        await new Promise((resolve) => { resolveWindow = resolve; timer = setTimeout(resolve, 10000); });
        if (!session.closed) await sendEmbed(message.channel, { content: clipText(replies.join("\n") || "Nenhuma resposta recebida.", 1950), allowedMentions: { parse: [] } });
      } finally { cleanup(); }
    });
  } catch (error) {
    await replyEmbed(message, { content: error.message, allowedMentions: { parse: [] } });
  }
  return true;
}
