# Capas, Panic e notificacoes

## Alteracoes

- CapeManager: somente Tampinhas Group e Tampinhas Logo; suavizacao por textura
  sem alterar as configuracoes graficas globais (detalhes em cheatbreaker-capes.md).
- Panic, na categoria Misc, e uma acao de uso unico com bind configuravel e sem
  tecla padrao. Solicita `setEnabled(false, true)` para todos os modulos ativos,
  inclusive visuais/ocultos/submodulos. Nao descarrega a DLL/JVM, nao fecha o
  jogo, nao altera arquivos nem registros do Windows. A acao nunca fica ligada.
- Uma falha no onDisable de um modulo nao impede as outras tentativas. Um resumo
  informa se ainda restaram modulos ativos; notificacoes individuais sao
  temporariamente suprimidas e a configuracao anterior e restaurada.
- A quebra de texto deixou de estimar o corte pelo numero de caracteres e
  descartar o ultimo caractere. Agora mede largura real, preserva pares de
  formatacao e Unicode, e termina mesmo se uma letra for maior que a caixa.
- As notificacoes medem o titulo com a mesma fonte utilizada no desenho e
  reservam espaco para icone e margem direita. Dimensoes do texto sao calculadas
  antes de desenhar, sem reutilizar a largura de uma mensagem anterior.

O relato especifico da letra O ainda precisa de confirmacao visual na mesma
configuracao do usuario. Os testes cobrem a preservacao de Module/module/MODULE,
formatacao intercalada, palavras longas, caixas estreitas e espacos.

## Verificacao sem injetar no jogo

Iniciar o wrapper pelo JDK 8 neste computador, com `-PlegacyJava8Compiler=true`:

```
classes test capeTextureProbe capeTextureFilterProbe notificationTextProbe panicProbe capeHookObfuscationProbe -I tools/verify-capes.gradle -PcbPatch=<1.8.9.patch>
verifyInjectionPayload
```

Os probes usam texturas/GL/modulos simulados. O probe de hooks compila contra o
patch real do CheatBreaker, sem inicializar as classes do jogo. Nenhuma rotina
de limpeza do loader foi executada ou alterada, e nenhuma publicacao no backend remoto
foi feita nesta revisao. O bundle nativo existente nao incorpora estas mudancas
ate ser recompilado.
