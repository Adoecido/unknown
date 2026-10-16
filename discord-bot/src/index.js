import {
  Client,
  Events,
  GatewayIntentBits,
  Partials,
} from "discord.js";
import { loadConfig } from "./config.js";
import { handleConfigCommand, handleFilterMessage, handleModerationCommand } from "./moderation.js";
import { JsonStore } from "./store.js";
import {
  handleTicketButton,
  handleTicketAnnouncement,
  handleTicketCommand,
  handleTicketModal,
  handleTicketUserSelect,
} from "./tickets.js";
import { replyEphemeral } from "./discord-utils.js";
import { MinecraftSessions } from "./minecraft-sessions.js";
import { UnknownSyncClient } from "./unknown-sync.js";
import { handleInvseeCommand, handleMinecraftText } from "./invsee.js";
import { checkCommandLocation, handleMemberJoin, handlePublication, handleServerSetup, handleVerification, minecraftCommands, setupServer } from "./server.js";

const config = loadConfig();
const store = new JsonStore(config.dataFile);
await store.init();
await store.recoverTickets();

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.GuildMessageReactions,
    GatewayIntentBits.MessageContent,
  ],
  partials: [Partials.Message, Partials.Channel, Partials.Reaction],
  allowedMentions: { parse: [] },
});
const context = { config, store, minecraft: new MinecraftSessions(config.minecraft), unknownSync: new UnknownSyncClient(config.unknownSync) };

async function handleInteraction(interaction) {
  if (!interaction.inGuild()) {
    if (interaction.isRepliable()) await replyEphemeral(interaction, "Este bot só funciona dentro de servidores.");
    return;
  }

  if (interaction.isChatInputCommand()) {
    if (!await checkCommandLocation(interaction, context)) return;
    if (minecraftCommands.has(interaction.commandName)) return handleInvseeCommand(interaction, context);
    if (["anuncio", "features", "video"].includes(interaction.commandName)) return handlePublication(interaction, context);
    if (interaction.commandName === "config" && interaction.options.getSubcommand() === "servidor") return handleServerSetup(interaction, context);
    if (interaction.commandName === "ticket") await handleTicketCommand(interaction, context);
    if (interaction.commandName === "mod") await handleModerationCommand(interaction, context);
    if (interaction.commandName === "config") await handleConfigCommand(interaction, context);
    if (interaction.commandName === "anuncioticket") await handleTicketAnnouncement(interaction, context);
    return;
  }
  if (interaction.isButton() && interaction.customId.startsWith("ticket:")) {
    await handleTicketButton(interaction, context);
    return;
  }
  if (interaction.isUserSelectMenu() && interaction.customId.startsWith("ticket:")) {
    await handleTicketUserSelect(interaction, context);
    return;
  }
  if (interaction.isModalSubmit() && interaction.customId.startsWith("ticket:create")) {
    await handleTicketModal(interaction, context);
  }
}

client.once(Events.ClientReady, async (readyClient) => {
  console.log(`Conectado como ${readyClient.user.tag}.`);
  for (const guild of readyClient.guilds.cache.values()) {
    if (config.guildId ? guild.id !== config.guildId : !guild.channels.cache.has(config.channels.tickets)) continue;
    try { await setupServer(guild, context); }
    catch (error) { console.error(`Falha na configuração do servidor ${guild.id}:`, error.message); }
  }
});

client.on(Events.InteractionCreate, async (interaction) => {
  try {
    await handleInteraction(interaction);
  } catch (error) {
    console.error("Erro ao processar interação:", error);
    if (interaction.isRepliable()) {
      await replyEphemeral(interaction, "Ocorreu um erro ao executar esta ação. Confira as permissões e tente novamente.").catch(() => undefined);
    }
  }
});

client.on(Events.MessageCreate, async (message) => {
  try {
    const filtered = await handleFilterMessage(message, context);
    if (!filtered) await handleMinecraftText(message, context);
  } catch (error) { console.error("Erro ao processar mensagem:", error.message); }
});

client.on(Events.GuildCreate, (guild) => {
  if (config.guildId ? guild.id !== config.guildId : !guild.channels.cache.has(config.channels.tickets)) return;
  setupServer(guild, context).catch((error) => console.error("Falha ao configurar servidor após convite:", error.message));
});

client.on(Events.GuildMemberAdd, (member) => {
  handleMemberJoin(member, context).catch((error) => console.error("Erro no registro de entrada:", error.message));
});
client.on(Events.MessageReactionAdd, (reaction, user) => {
  handleVerification(reaction, user, context).catch((error) => console.error("Erro na verificação:", error.message));
});

client.on(Events.ChannelDelete, (channel) => {
  context.minecraft.stop(channel.id);
  store.removeTicketByChannel(channel.id).catch((error) => {
    console.error("Não foi possível limpar ticket excluído do armazenamento:", error);
  });
});

for (const signal of ["SIGINT", "SIGTERM"]) process.once(signal, () => {
  context.minecraft.stopAll();
  client.destroy();
});

await client.login(config.token);
