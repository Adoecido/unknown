# Configuração do servidor UNKNOWN

Os IDs abaixo são os padrões do código e já estão no `.env` local.

| Destino | ID | Comportamento |
| --- | --- | --- |
| ticket | [REDACTED] | Painel persistente Buy (solo) / Buy (clan). |
| commands | [REDACTED] | Comandos gerais e publicações da administração. |
| features | [REDACTED] | Recebe `/features`, com embed e imagem opcional. |
| midia | [REDACTED] | Recebe `/video`, link e convite para curtir/inscrever-se. |
| announcement | [REDACTED] | Recebe `/anuncio`, com embed e imagem opcional. |
| admin | [REDACTED] | Entradas, lista inicial de membros, transcripts e mod-log. |
| home | [REDACTED] | Mensagem de entrada com reação ✅. |

| Cargo | ID | Uso |
| --- | --- | --- |
| .gg/unknown | [REDACTED] | Entregue após a reação; acesso de membros. |
| helper | [REDACTED] | Equipe de atendimento e gestão de tickets. |
| unknown | [REDACTED] | Cargo do proprietário; comandos administrativos do bot. |

O bot precisa de Gerenciar canais/Gerenciar cargos e de um cargo acima de `.gg/unknown`. Ative **Server Members Intent** e **Message Content Intent** no Developer Portal. Os intents de mensagens/reação também estão habilitados no código.

## Inicialização

As dependências já foram instaladas neste diretório e `package-lock.json` fixa as versões. Preencha o token e Application ID no `.env`; `GUILD_ID` é recomendado para publicar os comandos rapidamente apenas neste servidor. Execute aqui:

```powershell
node scripts/deploy-commands.js
node src/index.js
```

Na primeira conexão ao Discord, o bot prepara as permissões dos canais informados e dos canais públicos existentes: usuários sem verificação veem #home, usuários com `.gg/unknown` veem os canais de membros, e #admin fica restrito à administração. Painéis e IDs das mensagens ficam persistidos em `data/state.json`. Remover e recriar uma mensagem é detectado na próxima configuração. Reações feitas enquanto o bot estiver offline são reconciliadas ao iniciar.

A verificação por reação é uma barreira de entrada simples. Não é um CAPTCHA que comprova que a conta é humana. Não entrega cargos helper ou unknown. Cargos de equipe/administradores são exceções ao bloqueio de entrada. Canais privados existentes mantêm sua política de acesso. Canais públicos criados posteriormente são configurados ao executar `/config servidor`.

## Publicações

Execute em #commands como administrador ou com o cargo unknown:

```text
/anuncio mensagem:Seu comunicado titulo:Novidades
/features mensagem:Lista dos recursos titulo:Features
/video link:https://youtu.be/SEU_VIDEO
/anuncioticket mensagem:Seu aviso para os tickets
```

`anuncio` e `features` aceitam Markdown no texto e uma imagem opcional. `/anuncioticket` mantém o @everyone automático nos tickets abertos. As outras publicações não fazem marcações automáticas.

Toda mensagem nova do bot, inclusive confirmações privadas, avisos, vídeos, logs e respostas Minecraft, é enviada em embed branco (`#FFFFFF`). O texto do anúncio fica no embed, mas `@everyone` permanece separado para que o Discord entregue a notificação. O mesmo vale para menções ao autor/equipe na abertura do ticket. Os painéis persistentes são atualizados ao iniciar; o histórico antigo de logs/mensagens não é reescrito.

Todos os comandos gerais são aceitos somente em #commands. Exceções: `/mod limpar` e `/mod historico` em qualquer canal, a gestão `/ticket adicionar|remover|fechar|info` dentro do ticket, e os comandos Minecraft descritos abaixo dentro do ticket. Os botões não dependem de #commands.

## Compras e Minecraft

1. Reaja com ✅ em #home para receber `.gg/unknown`.
2. Use Buy (solo) ou Buy (clan) em #ticket. O ticket é privado para o autor, participantes adicionados, helper, unknown e o bot.
3. Buy (clan) oferece um seletor opcional de até 10 integrantes ao criar o ticket. Os botões Adicionar/Remover continuam disponíveis em ambos os tipos.
4. A administração/unknown executa `/botinvsee nick:NomeDaConta` no ticket. O bot abre um cliente Mineflayer para aquele canal.
   Para testar somente a autenticação sem depender do P4Free aberto, use `/botinvsee nick:NomeDaConta modo:lobby`. O comando distingue conexão, login confirmado e verificação pendente; não abre o seletor nem automatiza verificação no modo lobby. Consultas Invsee/clan exigem o modo P4Free, que continua sendo o padrão. Desligue a conta antes de trocar o modo.
5. Todos os participantes que podem ver/enviar mensagens no ticket podem controlar essa sessão:

   ```text
   /invsee tag:CLAN
   /invsee-player nick:Jogador
   /invseeauto tag:CLAN segundos:60
   /invseeauto-stop
   /clanwarn estado:on
   /clanwarn estado:off
   /reconnect
   /syncunknown id:CODIGO_COPIADO_DO_TARGET
   /botinvsee-stop
   .use /invsee Jogador
   ```

6. `/botinvsee-stop` desliga a sessão e está disponível para todos os participantes. Somente a administração/unknown pode iniciar a conta e escolher seu nick com `/botinvsee`. Fechar/excluir o ticket também desliga a conta, cancela o auto-update e os listeners.

Com `/clanwarn estado:on` (sem `nick`), cada consulta `/invsee` ou atualização `/invseeauto` anuncia no chat Minecraft do clan o jogador com menos sets, mesmo que tenha sets completos. Empates anunciam todos os jogadores empatados. O aviso inclui a contagem de cada peça, por exemplo: `Jogador com menos sets: 0 set(s) - 1 capa 0 peito 2 calça 0 bota`. Jogadores offline, consultas sem resposta e possíveis mortes ficam fora dos avisos. `/clanwarn estado:off` desliga os anúncios. A opção `nick` continua enviando o comando individual `/tell warn <nick> on|off` do servidor.

Servidor: `drazyh.com:25565`, versão `1.8.9`, autenticação offline. A senha enviada ao `/login` e aos comandos de registro da base é o nick informado seguido de `123`, preservando maiúsculas/minúsculas. O perfil original seleciona **p4free** e trata os menus de captcha do servidor.

No modo P4Free, resolver o captcha não basta para anunciar sucesso: o bot espera a confirmação da transferência e o carregamento do mundo, ou um placar lateral que identifique o modo. Se o captcha apenas liberar o lobby, o seletor é aberto novamente, com até três tentativas de seleção. Enquanto houver fila, o bot aguarda a entrada sem selecionar novamente. O tempo limite continua sendo `MINECRAFT_SESSION_TIMEOUT_MS`.

Cada ticket precisa de um nick diferente para ficar online simultaneamente. O mesmo nick é bloqueado mesmo com diferenças de maiúsculas. Até 10 sessões simultâneas por padrão (`MINECRAFT_MAX_SESSIONS`). O início das conexões e reconexões é espaçado por cinco segundos (`MINECRAFT_CONNECTION_INTERVAL_MS=5000`), porque o Drazyh recusa conexões iniciadas muito rapidamente. Depois do login, as contas permanecem online simultaneamente. Consultas simultâneas dentro de um ticket são rejeitadas com aviso para aguardar; consultas em tickets diferentes não compartilham a conta, resultado, auto-update ou estado de clanwarn.

Auto-update aceita intervalos de 25 a 60 segundos e agenda a próxima rodada depois de terminar a consulta anterior. Ao reiniciar o processo, os tickets persistem, mas as conexões Minecraft não são religadas automaticamente: execute `/botinvsee` novamente. O Drazyh pode impor limite de contas por IP; isso depende do servidor, mesmo que o bot suporte múltiplas sessões.

A análise usa a regra atual da base: armaduras com pelo menos 75% de durabilidade e heads contadas como sets. A integração usa os módulos da base em `vendor/bot-invsee`; não execute separadamente seu `index.js` nem seu deploy de comandos, pois a aplicação principal gerencia o Discord e as sessões.
