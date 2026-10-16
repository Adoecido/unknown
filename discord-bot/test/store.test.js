import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { JsonStore } from "../src/store.js";

async function temporaryStore(t) {
  const directory = await mkdtemp(join(tmpdir(), "discord-bot-store-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const store = new JsonStore(join(directory, "state.json"));
  await store.init();
  return store;
}

test("reserva apenas o número permitido de tickets abertos por usuário", async (t) => {
  const store = await temporaryStore(t);
  const [first, second] = await Promise.all([
    store.reserveTicket({ id: "first", guildId: "guild", ownerId: "owner", subject: "Ajuda", maximumOpenTickets: 1 }),
    store.reserveTicket({ id: "second", guildId: "guild", ownerId: "owner", subject: "Ajuda", maximumOpenTickets: 1 }),
  ]);

  assert.ok(first);
  assert.equal(first.number, 1);
  assert.equal(second, null);
  await store.finalizeTicket(first.id, "channel");
  assert.equal((await store.getTicketByChannel("channel")).status, "open");
});

test("mantém participantes e estado de fechamento de maneira persistente", async (t) => {
  const store = await temporaryStore(t);
  const ticket = await store.reserveTicket({ id: "ticket", guildId: "guild", ownerId: "owner", subject: "Ajuda", maximumOpenTickets: 1 });
  await store.finalizeTicket(ticket.id, "channel");
  assert.equal((await store.addParticipant(ticket.id, "guest")).changed, true);
  assert.equal((await store.getOpenTickets("guild")).length, 1);
  assert.equal((await store.markTicketClosing(ticket.id)).status, "closing");
  assert.equal((await store.getOpenTickets("guild")).length, 0);
  assert.equal((await store.reopenTicket(ticket.id)).status, "open");
  assert.equal((await store.removeParticipant(ticket.id, "guest")).changed, true);
});
