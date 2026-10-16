# bot-invsee

Fonte: https://github.com/uDxkr/bot-invsee

Commit integrado: `a9ecaa6d56a0789331856dbc7f2332e03d02a971`.

O diretório `vendor/bot-invsee` mantém a base fornecida pelo usuário. A integração importa `src/mineflayerBot.js`, `src/armor.js` e `src/commands.js`. O cliente Discord original não é iniciado: este projeto usa seu próprio cliente e roteia cada comando para uma sessão Minecraft exclusiva do ticket.

Adaptações locais de `mineflayerBot.js`: encerramento explícito de sessão, invalidação de callbacks ao fechar um ticket, estado desconectado, espera de login com cancelamento do timer, parada de consultas múltiplas após encerrar a conta, tratamento de erros de janela e injeção opcional do cliente para testes de login/captcha sem conexão real. A contagem de sets usa o critério do código atual da base: durabilidade mínima de 75% e heads contadas como sets.

Não havia arquivo LICENSE no commit consultado. Esta nota registra a origem; não concede licença para redistribuir a base. As dependências npm mantêm suas próprias licenças.

## Nomes de itens do Minecraft

`vendor/bot-invsee/src/minecraft-item-names.json` relaciona nomes de itens/blocos em português brasileiro e europeu com seus nomes em inglês. Os rótulos foram extraídos dos arquivos de idioma oficiais da versão 1.8.9 para interpretar os títulos enviados pelo servidor. O gerador local está em `.tools/generate-item-names.py`.

Fontes Mojang/Microsoft: [pt_BR.lang](https://resources.download.minecraft.net/eb/eba6a289fd6815c89c8c35277c6775723621cfd7), [pt_PT.lang](https://resources.download.minecraft.net/7b/7b243164dfca23d30a01763a9f26ab1863932ac2) e `assets/minecraft/lang/en_US.lang` do [cliente 1.8.9](https://launcher.mojang.com/v1/objects/3870888a6c3d349d3771a3e9d16c9bf5e076b908/client.jar). Esta aplicação não distribui o cliente nem seus demais recursos.
