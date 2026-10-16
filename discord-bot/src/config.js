import dotenv from "dotenv";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const sourceDirectory = dirname(fileURLToPath(import.meta.url));
export const projectDirectory = resolve(sourceDirectory, "..");
dotenv.config({ path: resolve(projectDirectory, ".env") });

const defaultStaff = "1552105416966668368,1526951514831065310";

function parsePositiveInteger(value, fallback, name) {
  if (value === undefined || value === "") return fallback;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    throw new Error(`${name} deve ser um número inteiro positivo.`);
  }
  return parsed;
}

function parseIdList(value) {
  if (!value) return [];
  return [...new Set(value.split(",").map((id) => id.trim()).filter(Boolean))];
}

function parsePhrases(value) {
  if (!value) return [];
  return [...new Set(value.split(";").map((phrase) => phrase.trim()).filter(Boolean))];
}

export function loadConfig({ requireToken = true } = {}) {
  const token = process.env.DISCORD_TOKEN?.trim();
  const clientId = process.env.CLIENT_ID?.trim();

  if (requireToken && !token) {
    throw new Error("DISCORD_TOKEN não foi definido. Copie .env.example para .env e preencha-o.");
  }

  if (!clientId) {
    throw new Error("CLIENT_ID não foi definido. Informe o Application ID do bot no arquivo .env.");
  }

  return Object.freeze({
    token,
    clientId,
    guildId: process.env.GUILD_ID?.trim() || null,
    ticketCategoryId: process.env.TICKET_CATEGORY_ID?.trim() || null,
    ticketLogChannelId: process.env.TICKET_LOG_CHANNEL_ID?.trim() || "1526424900175266013",
    staffRoleIds: parseIdList(process.env.STAFF_ROLE_IDS || defaultStaff),
    ownerRoleId: process.env.OWNER_ROLE_ID?.trim() || "1526951514831065310",
    verifiedRoleId: process.env.VERIFIED_ROLE_ID?.trim() || "1553419491013173360",
    channels: {
      tickets: process.env.TICKET_PANEL_CHANNEL_ID?.trim() || "1552125296411676712",
      commands: process.env.COMMANDS_CHANNEL_ID?.trim() || "1552105953053376543",
      features: process.env.FEATURES_CHANNEL_ID?.trim() || "1552105878403162273",
      media: process.env.MEDIA_CHANNEL_ID?.trim() || "1552105826368495646",
      announcement: process.env.ANNOUNCEMENT_CHANNEL_ID?.trim() || "1552104189528907837",
      admin: process.env.ADMIN_CHANNEL_ID?.trim() || "1526424900175266013",
      home: process.env.HOME_CHANNEL_ID?.trim() || "1553418966804992052",
    },
    ticketMaxOpenPerUser: parsePositiveInteger(process.env.TICKET_MAX_OPEN_PER_USER, 1, "TICKET_MAX_OPEN_PER_USER"),
    ticketCloseDelaySeconds: parsePositiveInteger(process.env.TICKET_CLOSE_DELAY_SECONDS, 8, "TICKET_CLOSE_DELAY_SECONDS"),
    ticketTranscriptMessageLimit: parsePositiveInteger(
      process.env.TICKET_TRANSCRIPT_MESSAGE_LIMIT,
      1000,
      "TICKET_TRANSCRIPT_MESSAGE_LIMIT",
    ),
    modLogChannelId: process.env.MOD_LOG_CHANNEL_ID?.trim() || "1526424900175266013",
    minecraft: {
      host: process.env.MINECRAFT_HOST?.trim() || "drazyh.com",
      port: parsePositiveInteger(process.env.MINECRAFT_PORT, 25565, "MINECRAFT_PORT"),
      version: process.env.MINECRAFT_VERSION?.trim() || "1.8.9",
      auth: process.env.MINECRAFT_AUTH?.trim() || "offline",
      passwordTemplate: process.env.MINECRAFT_PASSWORD_TEMPLATE || "{nick}123",
      maximumSessions: parsePositiveInteger(process.env.MINECRAFT_MAX_SESSIONS, 10, "MINECRAFT_MAX_SESSIONS"),
      connectionIntervalMs: parsePositiveInteger(process.env.MINECRAFT_CONNECTION_INTERVAL_MS, 5000, "MINECRAFT_CONNECTION_INTERVAL_MS"),
      sessionTimeoutMs: parsePositiveInteger(process.env.MINECRAFT_SESSION_TIMEOUT_MS, 45000, "MINECRAFT_SESSION_TIMEOUT_MS"),
    },
    defaultFilterWords: parsePhrases(process.env.DEFAULT_FILTER_WORDS),
    unknownSync: {
      baseUrl: process.env.UNKNOWN_CONTROL_URL?.trim() || "",
      botToken: process.env.UNKNOWN_INVSEE_BOT_TOKEN?.trim() || "",
    },
    dataFile: process.env.BOT_DATA_FILE || resolve(projectDirectory, "data", "state.json"),
  });
}
