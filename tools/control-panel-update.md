# Configs e party — 2026-09-03

## Causa reproduzida

Os formularios de configs/party possuem botoes `name="action"`. Isso oculta a
propriedade `HTMLFormElement.action`: ela vira `RadioNodeList` (varios botoes)
ou `HTMLButtonElement` (um botao). O `fetch(form.action)` antigo enviava para
uma URL como `[object RadioNodeList]`, sem chegar ao endpoint correto.

## Correcao

- Ler o destino por `getAttribute('action')`, preservando a raiz da sessao.
- Incluir exatamente a acao clicada, inclusive com Enter/submitter ausente.
- Bloquear envios duplicados enquanto configs/party aguardam resposta.
- Conservar os campos digitados e apresentar erros seguros da API.
- Exibir estado do slot, versao, data local e confirmacoes para substituir/excluir.
- Diferenciar indisponibilidade da nuvem de um slot vazio; desabilitar gravacao
  quando a listagem falha, evitando sobrescrever uma copia nao consultada.
- Manter a aba e a mensagem de sucesso apos recarregar.
- Nao manter em cache a ausencia de uma sessao que ainda pode chegar do loader.

## Verificacao

Java 8: `-I tools/verify-control.gradle controlPanelProbe controlSessionProbe`.
O probe gera fixtures pelo renderer real e valida recuperacao de token e mensagens seguras.

Navegador: `node tools/test-control-panel.cjs` usa Playwright. `PLAYWRIGHT_MODULE`
e `BROWSER_EXE` podem apontar para instalacoes locais. Testa salvar/carregar/excluir,
criar/entrar/sincronizar/sair, Enter, CSRF no envio, erros, duplicidade, escape de
HTML, indisponibilidade e layout mobile. Capturas em `build/control-probe`.

API: `python vps/test_server.py`
executa a funcao original com TablesDB simulado em memoria: quatro slots,
versionamento, isolamento por usuario e party com dois membros.

Nenhuma config ou party real foi criada, sobrescrita ou excluida para testar.
A aplicacao ao Minecraft e a sincronizacao entre jogos reais ainda precisam
ser conferidas com o cliente atualizado em execucao. O backend e o arquivo
publicado no backend remoto nao foram alterados nesta correcao.

Build validado: `verifyInjectionPayload` (7.746 classes, bytecode Java 8), probes
de capas, filtros, notificacoes e Panic. DLL local gerada via alvo
`SonyVegas421Native`: `build/native/dist/IllIIlIl.dll`. O EXE/loader e a DLL antiga
em `build/injection` nao foram reconstruidos; esse bundle anterior nao contem
as correcoes novas do painel.
