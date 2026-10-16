# CapeManager no CheatBreaker 1.8.9

Patch analisado: SHA-256
`09dd73ec3fbd94786e0bcf76394e78e358d5fccc085b5324b788568b05373560`.

## Fluxo encontrado no bytecode

`RenderPlayer` ainda instala a camada vanilla `LayerCape`. Ela exige informacao
do jogador, jogador nao invisivel, a parte CAPE habilitada e uma textura nao
nula. Depois consulta `getLocationCape`, vincula a textura e desenha a capa.

O getter considera a configuracao de capas, recarga da capa OptiFine, dados
cosmeticos do cliente, a capa customizada e o `NetworkPlayerInfo`. Um hook em
todas as saidas normais desse getter permite substituir a textura local sem
alterar contas, downloads, capes dos outros jogadores ou preferencias do host.

Simbolos deste patch, confirmados no dump/bytecode:

| Papel | Classe/metodo runtime |
| --- | --- |
| LayerCape | `IlIllllIIlIllIlllIIllIlIl` |
| AbstractClientPlayer.getLocationCape | `IlIlIIIIIlIlIllIIIlIIIlll.IllllIIlllllIlllIIllllIlI()` |
| ResourceLocation | `IlllIIIIlIIIIIIlIllllIllI` |
| DynamicTexture(BufferedImage) | `IlIlIllIlIllIIIllIlIIlIIl` |
| TextureManager.getDynamicTextureLocation | `IlIlllIlIIIlIIlIlIIlllIll.IlIIlIIIllllIIlIllIlIlIII(String, DynamicTexture)` |

O gerenciador cria `dynamic/<nome>_<contador>` e registra o objeto atraves de
`loadTexture(ResourceLocation, ITextureObject)` antes de devolver o endereco.

## Por que a implementacao anterior falhava

1. O log `%TEMP%/sonyvegas-cape-debug.log` continha repetidamente
   `override(java.lang.Object,java.lang.Object) not found` e
   `forceCapeVisible(java.lang.Object,java.lang.Object,boolean) not found`.
   `RenameClasses` alterava esses nomes, mas as strings Javassist continuavam
   usando os nomes originais. Assim, nenhum dos dois hooks era instalado.
2. Mesmo instalado, o hook devolvia apenas `sonyvegas:capes/<estilo>.png`.
   Os PNGs pertencem ao classloader do payload, nao a um resource pack
   registrado no jogo. Abrir um stream de teste nao registra uma textura.
   Faltava carregar os pixels e entregar um DynamicTexture ao TextureManager.

## Correcao

- Os dois callbacks agora fazem parte de `METHOD_KEEP`. A verificacao do
  payload falha se eles forem renomeados/removidos novamente.
- `BundledCapeTexture` abre o PNG com `PayloadResources`, decodifica a imagem,
  instancia `DynamicTexture(BufferedImage)` e usa a API de registro do host.
  A API e reconhecida pela assinatura completa, incluindo o construtor da
  textura, sem nomes Allatori fixos; assinaturas ambiguas sao rejeitadas.
- Upload e registro acontecem sob demanda no hook de renderizacao, nao no
  toggle da interface. O cache evita abrir/decodificar/enviar a imagem em
  cada frame e registra novamente se a textura sumir ou o gerenciador mudar.
- Falhas preservam a textura original e limitam as novas tentativas. O modulo
  so substitui a capa do objeto do jogador local. Desativar o modulo volta a
  respeitar o resultado original, sem modificar estado persistente do CB.

Nao foram alterados os PNGs, a elegibilidade de capas de contas nem servicos
remotos. A capa e uma personalizacao visual local; nao e enviada aos outros
jogadores.

## Testes

```powershell
./gradlew.bat classes test capeTextureProbe capeHookObfuscationProbe -PlegacyJava8Compiler=true -I tools/verify-capes.gradle "-PcbPatch=$env:APPDATA/CheatBreaker/downloads/versions/1.8.9/1.8.9.patch"
./gradlew.bat verifyInjectionPayload -PlegacyJava8Compiler=true
```

Neste ambiente o wrapper foi iniciado pelo JDK 8 (veja `cheatbreaker-mappings.md`).
O teste de texturas usa um gerenciador simulado, verifica os dois PNGs HD, upload
unico por estilo, recarga, troca de gerenciador, recursos ausentes e fallback.
O teste de hooks passa o callback real por `RenameClasses` e compila os dois
snippets contra classes do patch original, sem carregar/inicializar o jogo.

Ainda e necessario um teste visual com o payload atualizado: habilitar
CapeManager, usar a camera em terceira pessoa, trocar os estilos e desativar.
Uma DLL previamente compilada continua contendo o payload antigo; precisa ser
reconstruida com o novo JAR antes de testar por ela. Nenhuma DLL foi injetada
ou processo do jogo modificado durante esses testes.

## Revisao de qualidade e catalogo (2026-09-03)

- O catalogo agora contem somente Tampinhas Group e Tampinhas Logo, ambas
  1024x512. Group e o padrao e o fallback de configuracoes antigas.
- Classic, Aurora, Forest, Rose, Lava e Blue foram retiradas dos recursos do
  produto. Copias de recuperacao locais ficam em `build/removed-capes`.
- Depois do registro no TextureManager, a textura recebe ampliacao bilinear
  e reducao trilinear com mipmaps quando o OpenGL oferece suporte. Os niveis
  param na resolucao base do atlas de capa para reduzir mistura entre faces.
- OpenGL antigo usa filtragem bilinear sem exigir suporte a mipmaps. As
  alteracoes sao restritas a textura da capa e restauram o binding anterior.
- Os PNGs originais nao foram ampliados artificialmente: textos muito pequenos
  continuam limitados pelo numero de pixels que a capa ocupa na tela.
- `capeTextureFilterProbe` verifica a politica de filtros e restauracao de
  estado com um backend simulado. Ainda falta confirmar visualmente no jogo.
