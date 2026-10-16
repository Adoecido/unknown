export function normalizeFilterPhrase(value) {
  return value
    .normalize("NFKD")
    .replace(/\p{Mark}/gu, "")
    .toLocaleLowerCase("pt-BR")
    .replace(/[^\p{Letter}\p{Number}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

export function findBlockedPhrase(message, phrases) {
  const words = normalizeFilterPhrase(message).split(" ").filter(Boolean);
  if (words.length === 0) return null;

  for (const phrase of phrases) {
    const normalizedPhrase = normalizeFilterPhrase(phrase);
    const phraseWords = normalizedPhrase.split(" ").filter(Boolean);
    if (phraseWords.length === 0) continue;

    for (let start = 0; start <= words.length - phraseWords.length; start += 1) {
      if (phraseWords.every((word, index) => words[start + index] === word)) {
        return phrase;
      }
    }
  }

  return null;
}

export function makeTicketChannelName(number, displayName) {
  const safeName = displayName
    .normalize("NFKD")
    .replace(/\p{Mark}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/(^-|-$)/g, "")
    .slice(0, 40) || "usuario";

  return `ticket-${String(number).padStart(4, "0")}-${safeName}`.slice(0, 100);
}

export function clipText(value, maximum = 900) {
  if (!value) return "Não informado";
  return value.length > maximum ? `${value.slice(0, maximum - 1)}…` : value;
}
