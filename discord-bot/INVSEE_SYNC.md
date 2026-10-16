# Invsee → Target

A integração usa a API existente do client, não um servidor local. Precisa publicar **bot, API e DLL atualizados**; só atualizar o bot não muda clientes antigos.

## Publicação do bot e da API

Publicado na VPS em 26/09/2026: bot e API ativos, segredo exclusivo configurado nos dois serviços e `/syncunknown id:` registrado no Discord. A origem configurada é `https://[REDACTED]`. A comunicação HTTPS autenticada e a definição do comando foram verificadas. Os backups anteriores à atualização estão em `[REDACTED]` (acesso somente de root).

A DLL não foi recompilada nem publicada nesta etapa. Clientes antigos precisam atualizar a DLL; o pareamento com o jogo e um ticket real ainda precisa ser validado. O reinício do bot encerrou as sessões Minecraft anteriores, então execute `/botinvsee` novamente no ticket.

Atualização posterior em 26/09/2026: a DLL compilada com o pareamento foi publicada no canal **developer** (28.815.872 bytes; SHA-256 `f67e0ff7ce98873706bb558ebbbe947e3611b6136f3e0e9f7e98a0e839f416c7`). O arquivo de release já tinha esse hash e não foi alterado nesta publicação. Substituição atômica, permissões `sonyvegas:sonyvegas`/`0600`, hash remoto conferido e API saudável. Backup do developer anterior: `[REDACTED]`. Para carregar a atualização, feche o Minecraft, abra novamente e reinjete pelo loader com a conta developer. Validação com um ticket e o jogo real ainda pendente.

## Configuração

Correção das contagens por peça em 26/09/2026: a versão em execução na VPS publicava somente `name`/`sets`, e a API também descartava `helmet`, `chestplate`, `leggings` e `boots`. Foram atualizados somente esses dois trechos, preservando a contagem real de cada peça (sem substituir tudo pelo total de sets). A DLL do bundle atual já lê esses campos e não precisa ser recompilada para esta correção. Testes locais: bot 65/65, API 12/12, probe Java de leitura/serialização e testes nativos 4/4. Na VPS passaram o teste do bot com oito peças de cada tipo e cinco testes isolados de Invsee/configurações; os serviços ficaram ativos e a API saudável. Backup: `[REDACTED]`. O reinício do bot encerra as sessões Minecraft em memória: abra novamente com `/botinvsee` e execute `/invsee` ou `/invseeauto` para publicar um relatório novo; não é necessário refazer `/syncunknown` se a associação ainda existir.

Gere um segredo exclusivo aleatório com pelo menos 32 caracteres. Na VPS configure `SONYVEGAS_INVSEE_BOT_TOKEN`; no bot configure o mesmo valor em `UNKNOWN_INVSEE_BOT_TOKEN`. Não use token administrativo nem token Discord. Configure `UNKNOWN_CONTROL_URL` com a origem HTTPS da API. O bot publica em `/v1/invsee/sync`. Reinicie os dois serviços e registre os comandos pelo script existente `npm run deploy`. Nunca comite os segredos.

## Uso

1. Ligue **Invsee Synchronization** no Target. Um código de 30 caracteres é copiado; vence em 10 minutos e só pode ser usado uma vez. **Copy Sync Code** copia de novo enquanto pendente, ou gera outro depois do pareamento/vencimento.
2. No seu ticket aberto, use `/syncunknown id:<código>`. Qualquer participante com acesso para ver e escrever no ticket pode vincular seu client. A resposta é privada e não repete o código.
3. Use `/invsee tag:...` para uma consulta ou `/invseeauto` para atualizações. O menu mostra até 100 jogadores, ordenados por menos sets. Empates mantêm a ordem do relatório.
4. **Manual** fixa o jogador clicado. **Automatic** usa **Invsee Position** (1–100). **Follow Position** acompanha o ocupante a cada relatório; **Until Death** fixa o ocupante inicial até uma morte confirmada pelo jogo, depois escolhe a posição novamente.

O bot chama inventário vazio de *possível morte*, não confirmação. Sair do alcance, ficar offline ou sumir do relatório não libera a trava Until Death. Relatório indisponível/vencido suspende ataques; não há fallback para outro player. Se não houver jogador naquela posição, aguarda. Ao desligar a sincronização, a associação é removida. Desligar só o Target pausa o módulo, mantendo a associação.

`/invseeauto-stop`, `/botinvsee-stop` e fechar o ticket limpam o relatório. Falhas de rede/reinício expiram pela validade: 180–900 segundos conforme o intervalo; ranking representa consultas sequenciais, não inventários instantâneos. Reparear não renova silenciosamente um relatório já vencido.

Verificação: `npm test`, `python -m unittest test_server test_invsee_sync` em `vps`, e `gradlew invseeRankingProbe` na raiz. Ainda é necessário validar com client e ticket reais após publicar.
