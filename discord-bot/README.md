# Bot de Tickets e Moderação para Discord

Bot independente em `discord.js` para atendimento e moderação. Ele foi estruturado a partir das funcionalidades desejadas nas referências [discord-tickets/bot](https://github.com/discord-tickets/bot) e [Red-DiscordBot](https://github.com/Cog-Creators/Red-DiscordBot), sem copiar código delas.

Também integra a base [uDxkr/bot-invsee](https://github.com/uDxkr/bot-invsee), com uma sessão Mineflayer exclusiva por ticket. Os IDs do servidor e o acesso ao Drazyh já estão configurados. Veja [CONFIGURACAO.md](CONFIGURACAO.md) para os destinos dos comandos e o fluxo de verificação.

## O que está pronto

- Todas as novas mensagens e respostas usam embeds brancos (`#FFFFFF`), incluindo erros, logs e Minecraft. Somente as menções necessárias para notificações ficam fora do embed; botões e anexos continuam disponíveis.
- Tickets privados **Buy (solo)** e **Buy (clan)**, com seleção de convidados para o clan.
- Modal para descrever o assunto antes de criar o ticket.
- Controle de acesso no próprio ticket: botão **Adicionar pessoa**, botão **Remover pessoa** e equivalentes `/ticket adicionar` e `/ticket remover`.
- Fechamento com transcript `.txt` enviado ao canal de logs, seguido da exclusão do canal.
- `/mod kick`, `/mod ban`, `/mod softban`, `/mod hackban`, `/mod limpar` e `/mod historico`.
- Mod-log persistente com numeração de casos e envio opcional para um canal de logs.
- Filtro automático de palavras/frases, configurado por `/config filtro adicionar`, `remover` e `listar`.
- `/anuncioticket mensagem:...`, exclusivo para administradores, envia um aviso com `@everyone` a todos os tickets abertos.
- Dados persistidos em `data/state.json`; token e dados operacionais não entram no Git.
- Verificação por reação ✅ em #home, que entrega somente o cargo `.gg/unknown`.
- Registro de entradas em #admin, com lista dos membros atuais no primeiro início.
- `/anuncio`, `/features` e `/video` publicam nos respectivos canais.
- `/botinvsee nick:...` inicia a conta Minecraft do ticket; todos os participantes podem usar os comandos Invsee nesse ticket.
- `/botinvsee nick:... modo:lobby` testa a conexão e a autenticação sem abrir o seletor ou tentar entrar no P4Free. Quando o servidor apresenta um captcha em menu, o bot seleciona o item pedido antes de confirmar o login.

## Instalação

1. Instale o Node.js 22 ou superior, incluindo o `npm` (Node.js 24 recomendado).
2. No terminal, entre nesta pasta e instale as dependências:

   ```powershell
   cd C:\Users\Administrator\Documents\sonyvegas\discord-bot
   npm install
   ```

3. O `.env` local já foi criado com os IDs enviados. Preencha `DISCORD_TOKEN`, `CLIENT_ID` e, preferencialmente, `GUILD_ID`. Em outra máquina, use `.env.example` como modelo.
4. No Discord Developer Portal, habilite os intents privilegiados **Server Members Intent** e **Message Content Intent**. Copie o token para `DISCORD_TOKEN`, sem compartilhá-lo.
5. Convide o bot com os escopos `bot` e `applications.commands`. Conceda **Gerenciar canais**, **Gerenciar cargos**, **Ver canal**, **Enviar mensagens**, **Ler histórico de mensagens**, **Gerenciar mensagens**, **Adicionar reações**, **Inserir links/Embed Links**, **Anexar arquivos**, **Mencionar @everyone**, **Expulsar membros** e **Banir membros**. Coloque o cargo do bot acima de `.gg/unknown` e dos cargos que ele precisará moderar.
6. Publique os comandos e inicie:

   ```powershell
   npm run deploy
   npm start
   ```

Com `GUILD_ID` preenchido, os comandos são publicados apenas naquele servidor e ficam disponíveis quase imediatamente — ideal para desenvolvimento. Sem ele, a publicação é global e pode demorar para propagar.

## Configuração essencial

| Variável | Uso |
| --- | --- |
| `DISCORD_TOKEN` | Token secreto do bot. |
| `CLIENT_ID` | Application ID do Developer Portal. |
| `GUILD_ID` | Servidor de desenvolvimento opcional. |
| `TICKET_CATEGORY_ID` | Categoria onde os canais de ticket serão criados. |
| `TICKET_LOG_CHANNEL_ID` | Canal privado que recebe transcripts ao fechar tickets. |
| `STAFF_ROLE_IDS` | IDs de cargos da equipe, separados por vírgula. Administradores também têm acesso. |
| `MOD_LOG_CHANNEL_ID` | Canal privado para logs de moderação. |
| `DEFAULT_FILTER_WORDS` | Frases iniciais separadas por `;`; depois elas podem ser administradas por comando. |

Sem `TICKET_CATEGORY_ID`, o bot usa a categoria que contém #ticket. Os logs usam #admin por padrão.

## Uso inicial

O bot cria/atualiza o painel em #ticket e a mensagem de verificação em #home ao iniciar, sem duplicá-los. Para atualizar manualmente as permissões e painéis, use `/config servidor` em #commands. Para atualizar somente o painel, um membro da equipe pode executar em #commands:

```text
/ticket painel canal:#ticket
```

Quem abrir um ticket e a equipe configurada podem clicar em **Adicionar pessoa** e selecionar um usuário. O bot cria uma permissão explícita para essa pessoa no canal. Pessoas adicionadas não podem se adicionar ou remover outras; essa gestão fica com o autor do ticket e a equipe.

Um administrador pode enviar um comunicado para todos os atendimentos abertos com, por exemplo:

```text
/anuncioticket mensagem:O suporte ficará indisponível por alguns minutos.
```

O bot acrescenta `@everyone` automaticamente e não permite menções extras no texto personalizado. Para a marcação funcionar, o cargo do bot precisa ter a permissão **Mencionar @everyone, @here e todos os cargos**.

## Verificações locais

```powershell
npm test
```

O bot não inicia nem publica comandos como parte dos testes. Antes de colocar em produção, teste as permissões em um servidor de desenvolvimento e confirme se o cargo do bot está acima dos cargos que ele precisa moderar.
