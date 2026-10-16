# Friends, binds e party

- Friends: adicionar/remover pelo painel e opção de middle-click no jogador. A lista local usa o filtro de alvos existente.
- Binds: clique direito no cartão do módulo e escolha uma tecla em “Bind - ativar / desativar”. “Sem bind” remove a tecla. Os binds ficam no perfil local.
- Party: o líder seleciona os módulos e escolhe “Only config” (mantém o estado ligado/desligado de cada usuário) ou “Ativar for all” (aplica a configuração e liga os selecionados, incluindo o líder).
- O envio remove metadados de interface e binds pessoais, respeita 16 KB por comando e divide módulos grandes entre comandos sem dividir valores individuais.
- Todos os participantes precisam usar o JAR atualizado para interpretar “Only config”. Não foi alterado ou publicado o servidor VPS.
- As saídas de diagnóstico do código Java para console, arquivos e bridge foram removidas. A formatação interna de exceções continua disponível.
- Foi protegido o callback EventMouseOverUpdate contra acesso aos mappings antes da inicialização, de acordo com o relatório local crash-2026-09-04_18.49.43-client.txt. Não foi possível vincular esse arquivo ao identificador CB-CRASH fornecido.

## Build Java isolado

Usar Java 8 e executar:

```powershell
.\gradlew.bat -I tools/verify-control.gradle partySyncProbe controlPanelProbe controlSessionProbe remoteIdentityProbe verifyInjectionPayload -PlegacyJava8Compiler=true -PjavaOnlyBuild=true --console=plain
```

`javaOnlyBuild=true` grava o mapa auxiliar em build/obfuscation e não limpa nem acessa as saídas de native. O artefato é build/libs/lIIlIIlI-1.0-injection.jar. Nenhuma DLL/EXE é recompilada.

## Verificação

Os probes verificam seleção, limite UTF-8, divisão de módulos grandes, modos de sincronização, exclusão de binds pessoais, callback anterior aos mappings e sessões do painel. tools/test-control-panel.cjs testa o renderer e os formulários com respostas locais simuladas, incluindo desktop e celular. O teste não substitui uma sessão real de Minecraft com dois clientes.
