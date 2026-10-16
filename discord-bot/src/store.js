import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

const EMPTY_STATE = Object.freeze({ version: 1, tickets: {}, guilds: {} });

function newState() {
  return structuredClone(EMPTY_STATE);
}

function timestamp() {
  return new Date().toISOString();
}

export class JsonStore {
  #data = newState();
  #queue = Promise.resolve();

  constructor(filePath) {
    this.filePath = filePath;
  }

  async init() {
    await mkdir(dirname(this.filePath), { recursive: true });
    try {
      const parsed = JSON.parse(await readFile(this.filePath, "utf8"));
      if (!parsed || typeof parsed !== "object") throw new Error("arquivo não contém um objeto");
      this.#data = {
        version: 1,
        tickets: parsed.tickets && typeof parsed.tickets === "object" ? parsed.tickets : {},
        guilds: parsed.guilds && typeof parsed.guilds === "object" ? parsed.guilds : {},
      };
    } catch (error) {
      if (error.code !== "ENOENT") {
        throw new Error(`Não foi possível ler ${this.filePath}: ${error.message}`);
      }
      await this.#persist();
    }
  }

  async #persist() {
    const temporaryPath = `${this.filePath}.tmp`;
    await writeFile(temporaryPath, `${JSON.stringify(this.#data, null, 2)}\n`, "utf8");
    await rename(temporaryPath, this.filePath);
  }

  async #mutate(callback) {
    const operation = this.#queue.then(async () => {
      const result = await callback(this.#data);
      await this.#persist();
      return result;
    });
    this.#queue = operation.catch(() => undefined);
    return operation;
  }

  async #snapshot(callback) {
    await this.#queue;
    return callback(structuredClone(this.#data));
  }

  #guildSettings(data, guildId, defaultFilterWords = []) {
    if (!data.guilds[guildId]) {
      data.guilds[guildId] = {
        filterWords: [...defaultFilterWords],
        nextTicketNumber: 1,
        nextCaseNumber: 1,
        cases: [],
      };
    }
    return data.guilds[guildId];
  }

  async getGuildSettings(guildId, defaultFilterWords = []) {
    await this.#queue;
    if (this.#data.guilds[guildId]) return structuredClone(this.#data.guilds[guildId]);
    return this.#mutate((data) => structuredClone(this.#guildSettings(data, guildId, defaultFilterWords)));
  }

  async updateGuildSettings(guildId, patch) {
    return this.#mutate((data) => {
      const settings = this.#guildSettings(data, guildId);
      Object.assign(settings, patch);
      return structuredClone(settings);
    });
  }

  async addFilterPhrase(guildId, phrase, defaultFilterWords = []) {
    return this.#mutate((data) => {
      const settings = this.#guildSettings(data, guildId, defaultFilterWords);
      if (settings.filterWords.some((existing) => existing.toLocaleLowerCase("pt-BR") === phrase.toLocaleLowerCase("pt-BR"))) {
        return { added: false, words: [...settings.filterWords] };
      }
      settings.filterWords.push(phrase);
      return { added: true, words: [...settings.filterWords] };
    });
  }

  async removeFilterPhrase(guildId, phrase, defaultFilterWords = []) {
    return this.#mutate((data) => {
      const settings = this.#guildSettings(data, guildId, defaultFilterWords);
      const before = settings.filterWords.length;
      settings.filterWords = settings.filterWords.filter(
        (existing) => existing.toLocaleLowerCase("pt-BR") !== phrase.toLocaleLowerCase("pt-BR"),
      );
      return { removed: before !== settings.filterWords.length, words: [...settings.filterWords] };
    });
  }

  async reserveTicket({ id, guildId, ownerId, subject, type = "solo", maximumOpenTickets, defaultFilterWords = [] }) {
    return this.#mutate((data) => {
      const openTickets = Object.values(data.tickets).filter(
        (ticket) => ticket.guildId === guildId && ticket.ownerId === ownerId && ["opening", "open"].includes(ticket.status),
      );
      if (openTickets.length >= maximumOpenTickets) return null;

      const settings = this.#guildSettings(data, guildId, defaultFilterWords);
      const ticket = {
        id,
        number: settings.nextTicketNumber++,
        guildId,
        channelId: null,
        ownerId,
        participantIds: [],
        subject,
        type,
        status: "opening",
        createdAt: timestamp(),
      };
      data.tickets[id] = ticket;
      return structuredClone(ticket);
    });
  }

  async finalizeTicket(id, channelId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[id];
      if (!ticket) return null;
      ticket.channelId = channelId;
      ticket.status = "open";
      return structuredClone(ticket);
    });
  }

  async getTicketByChannel(channelId) {
    return this.#snapshot((data) => {
      const ticket = Object.values(data.tickets).find(
        (entry) => entry.channelId === channelId && ["open", "closing"].includes(entry.status),
      );
      return ticket ? structuredClone(ticket) : null;
    });
  }

  async getOpenTickets(guildId) {
    return this.#snapshot((data) => Object.values(data.tickets)
      .filter((ticket) => ticket.guildId === guildId && ticket.status === "open" && ticket.channelId)
      .map((ticket) => structuredClone(ticket)));
  }

  async recoverTickets() {
    return this.#mutate((data) => {
      for (const [id, ticket] of Object.entries(data.tickets)) {
        if (ticket.status === "opening" && !ticket.channelId) delete data.tickets[id];
        if (ticket.status === "closing") ticket.status = "open";
      }
    });
  }

  async markTicketClosing(ticketId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[ticketId];
      if (!ticket || ticket.status !== "open") return null;
      ticket.status = "closing";
      return structuredClone(ticket);
    });
  }

  async reopenTicket(ticketId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[ticketId];
      if (!ticket || ticket.status !== "closing") return null;
      ticket.status = "open";
      return structuredClone(ticket);
    });
  }

  async addParticipant(ticketId, userId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[ticketId];
      if (!ticket || ticket.status !== "open") return { changed: false, ticket: null };
      if (ticket.ownerId === userId || ticket.participantIds.includes(userId)) {
        return { changed: false, ticket: structuredClone(ticket) };
      }
      ticket.participantIds.push(userId);
      return { changed: true, ticket: structuredClone(ticket) };
    });
  }

  async removeParticipant(ticketId, userId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[ticketId];
      if (!ticket || ticket.status !== "open") return { changed: false, ticket: null, owner: false };
      if (ticket.ownerId === userId) return { changed: false, ticket: structuredClone(ticket), owner: true };
      const before = ticket.participantIds.length;
      ticket.participantIds = ticket.participantIds.filter((id) => id !== userId);
      return { changed: before !== ticket.participantIds.length, ticket: structuredClone(ticket), owner: false };
    });
  }

  async removeTicket(ticketId) {
    return this.#mutate((data) => {
      const ticket = data.tickets[ticketId];
      if (!ticket) return null;
      delete data.tickets[ticketId];
      return structuredClone(ticket);
    });
  }

  async removeTicketByChannel(channelId) {
    return this.#mutate((data) => {
      const entry = Object.entries(data.tickets).find(([, ticket]) => ticket.channelId === channelId);
      if (!entry) return null;
      const [ticketId, ticket] = entry;
      delete data.tickets[ticketId];
      return structuredClone(ticket);
    });
  }

  async addModerationCase({ guildId, action, moderatorId, targetId, targetLabel, reason, metadata = {}, defaultFilterWords = [] }) {
    return this.#mutate((data) => {
      const settings = this.#guildSettings(data, guildId, defaultFilterWords);
      const entry = {
        number: settings.nextCaseNumber++,
        action,
        moderatorId,
        targetId,
        targetLabel,
        reason,
        metadata,
        createdAt: timestamp(),
      };
      settings.cases.unshift(entry);
      settings.cases = settings.cases.slice(0, 1000);
      return structuredClone(entry);
    });
  }

  async getModerationCases(guildId, targetId, limit = 10, defaultFilterWords = []) {
    const settings = await this.getGuildSettings(guildId, defaultFilterWords);
    return settings.cases.filter((entry) => entry.targetId === targetId).slice(0, limit);
  }
}
