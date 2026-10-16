import test from "node:test";
import assert from "node:assert/strict";
import { findBlockedPhrase, makeTicketChannelName, normalizeFilterPhrase } from "../src/text.js";

test("normaliza acentos e separadores no filtro", () => {
  assert.equal(normalizeFilterPhrase("  Árvore---De   Natal! "), "arvore de natal");
});

test("encontra apenas palavras ou sequências completas", () => {
  assert.equal(findBlockedPhrase("Esta frase é MUITO ruim!", ["muito ruim"]), "muito ruim");
  assert.equal(findBlockedPhrase("A palavra passagem não deve acionar o filtro", ["pass"]), null);
});

test("cria um nome de canal Discord seguro", () => {
  assert.equal(makeTicketChannelName(12, "João da Silva!!!"), "ticket-0012-joao-da-silva");
});
