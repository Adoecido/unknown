# Atualizacao dos mappings CheatBreaker 1.8.9

Atualizacao de 2026-09-03, usando o patch local atualizado no mesmo dia. Nenhuma
classe do jogo e executada durante o matching; o patch instalado fica intacto.

- Patch: `%APPDATA%/CheatBreaker/downloads/versions/1.8.9/1.8.9.patch`
- SHA-256: `1dbf331ec95bddf06b42e082ed49e4280b3cfabb071271e9aa494acdec4a2225`
- Referencia: cliente vanilla 1.8.9, SHA-1 `3870888a6c3d349d3771a3e9d16c9bf5e076b908`
- SRG/MCP e parser: `../cb_mappings/mc_ref` e `../cb_mappings/dump_cb_mappings.py`.
- Destino: `src/main/resources/mappings/cheatbreaker189`.

Resultado: 2.290 classes, 8.237 campos e 18.593 metodos, incluindo construtores
e nomes estaveis. Todas as 282 classes 1.8.9 referenciadas por `MappedClasses.m`
estao presentes. O auditor legado conta 281 porque nao reconhece uma chamada
fora do padrao de atribuicao que seu regex aceita.

## Limites conhecidos

`GuiIngame.renderScoreboard(ScoreObjective, ScaledResolution)` nao existe na
classe atual. Nao foi substituido por um metodo parecido: o hook opcional
`ScoreboardObjectiveRenderMappingTask` ja verifica falha de resolucao e o ignora.
E a unica pendencia de nomes encontrada pelo auditor de modulos (1.599 nomes
examinados, incluindo trechos de outras versoes). Isso nao equivale a um teste
com o cliente aberto.

O overload `FontRenderer.drawString(String,int,int,int)` mudou para
`(String,float,float,int)`. `MFontRenderer` registra os parametros corretos e
converte as coordenadas somente no CB 1.8.9. Esta excecao explicita fica em
`cb189_compatibility.json`, presa ao SHA-256 do patch. Outras assinaturas
incompativeis ficam fora dos TSVs; o relatorio lista as 36 rejeitadas e os
membros/classes vanilla ainda nao resolvidos (inclusive classes nao usadas).

## Dump enriquecido

Em `../cb_mappings/out`:

- `classes_resolved.json`: todas as 3.469 classes originais, com nomes notch/MCP
  e metadados de membros quando identificados; itens nao resolvidos ficam nulos.
- `resolved_mappings.tsv` / `.json`: campos e metodos efetivamente importados.
- `mc_classes.txt`: nomes de classes atualizados.
- `resolved_matches.tsv`: matching bruto, inclusive candidatos posteriormente
  rejeitados por assinatura; nao consumir como tabela final do runtime.
- `mapping-update.json`: hashes, contagens, adaptacoes e pendencias completas.
- `.mapping-backups`: versoes anteriores dos arquivos regenerados.

`classes.json`, `classes.txt`, `known_mappings.txt` e os outros dumps anteriores
foram preservados. O `known_mappings.txt` antigo e um artefato historico; os
novos nomes estao nos arquivos `resolved_*`.

## Reimportar ou conferir este patch

Python 3.10+; executar da raiz do projeto. `$vanillaJar` deve apontar para o
cliente vanilla 1.8.9 acima, nao para outro cliente modificado.

```powershell
$vanillaJar = 'C:/caminho/minecraft-1.8.9-client.jar'
$patchJar = "$env:APPDATA/CheatBreaker/downloads/versions/1.8.9/1.8.9.patch"
python tools/update_cb_mappings.py --matches ../cb_mappings/out/resolved_matches.tsv --vanilla $vanillaJar --cb-jar $patchJar --check
```

Sem `--check` e sem `--write`, so faz uma simulacao. `--report arquivo.json`
grava um relatorio de diagnostico. `--write` regenera recursos e dump, com
backups. Classes obrigatorias ausentes, simbolos inexistentes, conflitos e
hash diferente das notas de compatibilidade bloqueiam a gravacao.

## Refazer o matching em uma proxima atualizacao

Na maquina preparada, o fluxo completo pode ser executado com duplo clique em
`../cb_mappings/ATUALIZAR MAPPINGS E GERAR DLL.bat`. O atualizador guarda o JAR
normalizado e as mappings aceitas pelo hash do patch, deriva seeds estruturais,
faz o matching entre builds, valida antes de gravar, preserva o perfil anterior
e gera a DLL/loader. Se uma classe obrigatoria continuar ambigua, ele interrompe
sem substituir as mappings atuais e deixa um relatorio em
`../cb_mappings/auto_state/runs/<sha256>/report.json`.

As ferramentas Java usam o JAR completo do FabricMC Matcher **0.1.0**
(`matcher-0.1.0-win.jar`, incluindo ASM), fora do classpath do produto. Nao
substituir por outra versao sem ajustar a API. Compilar as ferramentas em uma
pasta temporaria com `javac -cp <matcher.jar> -d <temp> tools/NormalizeCheatBreaker.java
tools/MatchCheatBreaker.java tools/InspectCheatBreaker.java`.

1. `NormalizeCheatBreaker <patch> <novo.jar>` dobra expressoes inteiras e
   restitui literais de tabelas de strings numa copia para analise. Nao usar
   esse JAR normalizado para iniciar o jogo: ele nao mantem frames/debug.
2. `MatchCheatBreaker <vanilla.jar> <normalizado.jar> <saida>` produz
   `matches.tsv` e `pass-2.matches`.
3. `seed_cb_matches.py --vanilla ... --normalized ... --parser
   ../cb_mappings/dump_cb_mappings.py --out <seeds.tsv>` produz candidatos de
   classes por literais mutuamente unicos. Conferir candidatos antes de usar.
4. `MatchCheatBreaker <vanilla> <normalizado> <nova-saida> <anterior/pass-2.matches>
   <seeds.tsv> propagate` adiciona referencias de tipos/instrucoes e membros.
   Cada execucao faz duas passagens; continuar em nova pasta enquanto houver
   progresso relevante. `propagate_cb_matches.py` tambem permite inspecionar
   candidatos e conflitos de tipos separadamente.
5. `InspectCheatBreaker <jar> <classe> [regex-metodo] [regex-descriptor]`
   permite revisar bytecode. Os seeds manuais deste patch estao em
   `cb189_verified_seeds.tsv`; **nao reaproveitar nomes em outro patch**.
   Prefixo `!` corrige explicitamente um match previo, sem editar o jogo.
6. Atualizar notas de compatibilidade/hash e importar com o patch **original**,
   nunca com a copia normalizada. Revisar contagens e pendencias antes de gravar.

Matching por semelhanca nao garante equivalencia semantica. Campos artificiais
do ofuscador e copias modificadas pelo CB/OptiFine exigem revisao (especialmente
pacotes de movimento, estados GL e wrappers de renderizacao). Nao preencher
lacunas pela ordem de declaracao de campos/metodos.

## Verificacao

```powershell
python tools/test_update_cb_mappings.py
python tools/audit_cb.py --mc-ref ../cb_mappings/mc_ref
./gradlew.bat classes test -I tools/verify-cb-mappings.gradle cbFontRendererProbe -PlegacyJava8Compiler=true
```

Neste ambiente foi necessario iniciar o wrapper com o JDK 8 instalado em
`C:/Program Files/Eclipse Adoptium/jdk-8.0.504.1-hotspot/bin/java.exe` e definir
`-Dorg.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-8.0.504.1-hotspot`.
Os JDKs 17/21 falharam na conexao loopback do daemon antes de compilar. A
verificacao Java 8 compila o produto e testa tanto o caminho vanilla quanto o
boxing de coordenadas float do CB, sem iniciar o cliente nem injetar DLLs.
