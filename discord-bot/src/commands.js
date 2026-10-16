import { ChannelType, SlashCommandBuilder } from "discord.js";
import { commands as invseeCommands } from "../vendor/bot-invsee/src/commands.js";

const reasonOption = (option) => option
  .setName("motivo")
  .setDescription("Motivo registrado no log")
  .setMaxLength(512);

export const commandDefinitions = [
  new SlashCommandBuilder()
    .setName("ticket")
    .setDescription("Abra e gerencie tickets de suporte")
    .addSubcommand((subcommand) => subcommand
      .setName("painel")
      .setDescription("Envia o painel para abrir tickets")
      .addChannelOption((option) => option
        .setName("canal")
        .setDescription("Canal onde o painel será enviado")
        .addChannelTypes(ChannelType.GuildText)
        .setRequired(true)))
    .addSubcommand((subcommand) => subcommand
      .setName("abrir")
      .setDescription("Abre um ticket de compra")
      .addStringOption((option) => option.setName("tipo").setDescription("Tipo da compra")
        .addChoices({ name: "Buy (solo)", value: "solo" }, { name: "Buy (clan)", value: "clan" })
        .setRequired(true)))
    .addSubcommand((subcommand) => subcommand
      .setName("adicionar")
      .setDescription("Adiciona uma pessoa ao ticket atual")
      .addUserOption((option) => option
        .setName("membro")
        .setDescription("Pessoa que poderá ver e responder no ticket")
        .setRequired(true)))
    .addSubcommand((subcommand) => subcommand
      .setName("remover")
      .setDescription("Remove uma pessoa adicionada do ticket atual")
      .addUserOption((option) => option
        .setName("membro")
        .setDescription("Pessoa que deixará de acessar o ticket")
        .setRequired(true)))
    .addSubcommand((subcommand) => subcommand
      .setName("fechar")
      .setDescription("Fecha o ticket atual e salva o transcript"))
    .addSubcommand((subcommand) => subcommand
      .setName("info")
      .setDescription("Mostra os dados do ticket atual")),

  new SlashCommandBuilder()
    .setName("mod")
    .setDescription("Ferramentas de moderação")
    .addSubcommand((subcommand) => subcommand
      .setName("kick")
      .setDescription("Expulsa um membro")
      .addUserOption((option) => option.setName("membro").setDescription("Membro a expulsar").setRequired(true))
      .addStringOption(reasonOption))
    .addSubcommand((subcommand) => subcommand
      .setName("ban")
      .setDescription("Bane um membro")
      .addUserOption((option) => option.setName("membro").setDescription("Membro a banir").setRequired(true))
      .addStringOption(reasonOption))
    .addSubcommand((subcommand) => subcommand
      .setName("softban")
      .setDescription("Bane, limpa mensagens recentes e desbane o membro")
      .addUserOption((option) => option.setName("membro").setDescription("Membro para softban").setRequired(true))
      .addStringOption(reasonOption))
    .addSubcommand((subcommand) => subcommand
      .setName("hackban")
      .setDescription("Bane por ID, mesmo fora do servidor")
      .addStringOption((option) => option
        .setName("id")
        .setDescription("ID numérico da conta Discord")
        .setRequired(true)
        .setMinLength(17)
        .setMaxLength(20))
      .addStringOption(reasonOption))
    .addSubcommand((subcommand) => subcommand
      .setName("limpar")
      .setDescription("Apaga mensagens recentes do canal")
      .addIntegerOption((option) => option
        .setName("quantidade")
        .setDescription("Quantidade de mensagens, de 1 a 100")
        .setMinValue(1)
        .setMaxValue(100)
        .setRequired(true))
      .addUserOption((option) => option
        .setName("membro")
        .setDescription("Opcional: apaga apenas mensagens deste membro")))
    .addSubcommand((subcommand) => subcommand
      .setName("historico")
      .setDescription("Exibe os últimos casos registrados de um membro")
      .addUserOption((option) => option.setName("membro").setDescription("Membro consultado").setRequired(true))),

  new SlashCommandBuilder()
    .setName("config")
    .setDescription("Configurações do bot")
    .addSubcommand((subcommand) => subcommand.setName("servidor").setDescription("Atualiza painéis e permissões do servidor"))
    .addSubcommandGroup((group) => group
      .setName("filtro")
      .setDescription("Gerencia frases bloqueadas automaticamente")
      .addSubcommand((subcommand) => subcommand
        .setName("adicionar")
        .setDescription("Adiciona uma frase ao filtro")
        .addStringOption((option) => option
          .setName("frase")
          .setDescription("Palavra ou frase a bloquear")
          .setMinLength(2)
          .setMaxLength(100)
          .setRequired(true)))
      .addSubcommand((subcommand) => subcommand
        .setName("remover")
        .setDescription("Remove uma frase do filtro")
        .addStringOption((option) => option
          .setName("frase")
          .setDescription("Frase exatamente como foi cadastrada")
          .setMinLength(2)
          .setMaxLength(100)
          .setRequired(true)))
      .addSubcommand((subcommand) => subcommand
        .setName("listar")
        .setDescription("Lista as frases bloqueadas"))),

  new SlashCommandBuilder()
    .setName("anuncioticket")
    .setDescription("Envia um aviso com @everyone para todos os tickets abertos")
    .addStringOption((option) => option
      .setName("mensagem")
      .setDescription("Texto que será enviado em todos os tickets abertos")
      .setMinLength(1)
      .setMaxLength(1900)
      .setRequired(true)),

  ...["anuncio", "features"].map((name) => new SlashCommandBuilder()
    .setName(name)
    .setDescription(name === "anuncio" ? "Publica uma mensagem em #announcement" : "Publica uma novidade em #features")
    .addStringOption((option) => option.setName("mensagem").setDescription("Texto da publicação (aceita Markdown)").setMaxLength(4000).setRequired(true))
    .addStringOption((option) => option.setName("titulo").setDescription("Título do embed").setMaxLength(256))
    .addAttachmentOption((option) => option.setName("imagem").setDescription("Imagem opcional para o embed"))),

  new SlashCommandBuilder().setName("video").setDescription("Publica um vídeo em #midia com convite para curtir e se inscrever")
    .addStringOption((option) => option.setName("link").setDescription("Link do vídeo").setRequired(true).setMaxLength(1000)),

  new SlashCommandBuilder().setName("botinvsee").setDescription("Inicia uma conta Minecraft exclusiva para este ticket")
    .addStringOption((option) => option.setName("nick").setDescription("Nick da conta offline Minecraft").setMinLength(3).setMaxLength(16).setRequired(true))
    .addStringOption((option) => option.setName("modo").setDescription("P4Free (padrão) ou teste de conexão/login no lobby")
      .addChoices({ name: "P4Free — usar Invsee", value: "p4free" }, { name: "Lobby — testar conexão/login", value: "lobby" })),
  new SlashCommandBuilder().setName("botinvsee-stop").setDescription("Desliga a conta Minecraft deste ticket"),
  new SlashCommandBuilder().setName("syncunknown").setDescription("Vincula o ranking deste ticket ao Target do seu client")
    .addStringOption((option) => option.setName("id").setDescription("Código de 30 caracteres copiado pelo Target")
      .setMinLength(30).setMaxLength(30).setRequired(true)),
].map((command) => command.toJSON()).concat(invseeCommands).map((command) =>
  ["botinvsee", "botinvsee-stop", "syncunknown", ...invseeCommands.map(({ name }) => name)].includes(command.name)
    ? { ...command, default_member_permissions: null }
    : command);
