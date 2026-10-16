# Proteção binária (VMProtect) — pipeline e status

## Build nativa protegida

`buildNative` e `prepareInjectionBundle` aplicam VMProtect automaticamente
à DLL e ao loader. O console padrão é
`C:\Program Files\VMProtect Ultimate\VMProtect_Con.exe`; outro local pode ser
passado com `-Pvmprotect.console=...`. Sem o console, a configuração falha.
Os binários de linkagem ficam em `build/native/raw`; os binários finais e os
projetos VMProtect usados ficam em `build/native/dist` com os nomes
`IllIIlIl.dll`, `unknown.exe`, `IllIIlIl.dll.vmp` e `unknown.exe.vmp`.
Os projetos versionados em `native/vmprotect-dll.vmp` e
`native/vmprotect-exe.vmp` são a fonte das configurações. A compilação e a
proteção não confirmam execução no Minecraft.

## Pipeline exigido (ordem importa)

1. `./gradlew prepareInjectionBundle` — gera o JAR ofuscado, compila a DLL e
   o loader em `build/native/raw`, protege os dois e copia os binários finais
   para `build/injection`.
2. Assinatura de código, se houver, somente depois da proteção.
3. Mapas/símbolos gerados pelo VMProtect ficam em `build/` (gitignored) e
   nunca são distribuídos; use-os apenas para re-symbolication local.

### Tamanho do payload e compatibilidade de download

A build usa os projetos `native/vmprotect-dll.vmp` e
`native/vmprotect-exe.vmp`: VMProtect 2.X, instâncias padrão, complexidade
None, proteções de memória/imports/recursos e detecções da imagem. A opção
`Pack the Output File` fica desligada. O perfil do EXE também remove as
relocações; a opção só se aplica a EXEs. DLLs de release protegidas acima de
32 MiB são rejeitadas pela etapa CMake de proteção, pois loaders já distribuídos
mantêm esse limite. O JAR embutido usa Deflate nível 9 para manter a DLL
protegida abaixo desse limite sem mudar as opções do VMProtect. Ao proteger
manualmente, passar `-pf` com o respectivo arquivo `.vmp` e conferir o
tamanho antes de publicar.

## Critérios de avaliação quando a licença existir

- Compatibilidade: validar boot no ambiente autorizado (Badlion/CheatBreaker)
  com autenticação válida E inválida antes de distribuir.
- Proteção seletiva: priorizar as rotinas de carregamento/anti-tamper
  (`dllmain.c`, `loader_bootstrap.c`, `control_api_client.c`, `native_bridge.c`
  e a verificação de licença). O bytecode Java NÃO é protegido por VMProtect;
  o JAR já é ofuscado separadamente.
- Não incluir o wrapper Rust (`[REDACTED]`):
  ele é um SDK Rust não oficial e não se aplica a este pipeline C/C++.

## Rollout com a licença assinada (Etapa 3)

1. Na configuração inicial, gerar chave: `python vps/license_keygen.py` (a
   privada fica na estação do operador/VPS; **nunca** no git — veja .gitignore).
   Para um cliente já compilado, usar a chave existente que corresponde à
   chave pública embutida no `LicenseVerifier`; não gerar outro par.
2. Instalar `sonyvegas_server.py`, `license_crypto.py` e `license_private.json`
   em `/opt/sonyvegas`, com a privada em modo `0640`, dono `root:sonyvegas`,
   e reiniciar `sonyvegas.service`. O `vps/install.sh` agora exige os três
   arquivos em `/root/sonyvegas-deploy` e instala a chave com essas permissões.
3. Só depois distribuir clientes com `LicenseVerifier`: resposta autenticada
   **sem licença assinada válida** (ausente, assinatura inválida, chave errada,
   expirada ou produto incorreto) encerra o processo imediatamente
   (`Runtime.halt(13)`), por decisão do proprietário. Falhas de conectividade
   (API inalcançável/sem token) encerram o mod de forma controlada sem crash.
   Servidor primeiro, cliente depois.

### Rollout aplicado em 27/09/2026

O servidor em `[REDACTED]` não tinha o assinador nem a chave, e respondia
com sucesso sem `data.license`. A integração de assinatura foi instalada
com a chave correspondente à build existente e o serviço foi reiniciado às
18:04:10 UTC. O serviço ficou `active/running`, o assinador e a chave ficaram
legíveis pelo usuário do serviço e o endpoint local de saúde respondeu.
Backup do servidor anterior:
`[REDACTED]`.

## Higiene de strings na memória do javaw (2026-10)

Nenhuma string sensível deve persistir na memória do processo `javaw.exe`:
sem IPs numéricos, sem `http://`/`https://` e sem tokens de sessão em texto.
O que mudou para garantir isso:

- **Transporte nativo próprio** (`secure_transport.c`): HTTP/1.1 bruto sobre
  winsock + TLS via Schannel. O host de controle continua mascarado
  (byte array XOR) e com SPKI pin; o slot 2 de `control_spki_pins` fica
  zerado como espaço de rotação — para trocar o certificado, grave o novo
  hash no slot 2, distribua, e depois mova-o para o slot 1. Sem suporte a
  proxy, sem redirecionamentos, e o cabeçalho `Host` só é enviado para hosts
  com DNS público (o pin manual dispensa validação de nome/cadeia/expiração).
- **API de conta sem URL no Java**: `ApiHttpClient`/`ApiServices` só lidam com
  caminhos (`/api/v1/...`); o host, o esquema e o TLS ficam no lado nativo
  (`NativeBridge.acb`). O JAR nunca monta uma URL de rede.
- **Painel sem URL de sessão**: o servidor local usa `ServerSocket` cru com
  segredos (raiz da sessão e CSRF) guardados como `byte[]` mascarado por
  XOR, apagados após cada uso; a raiz nunca vira `String` longeva. O IP de
  LAN é montado dígito a dígito a partir de bytes crus.
- **Menu nativo sem Host header**: o cliente nativo envia requisições ao
  painel sem `Host` (contrato de detecção); navegadores sempre enviam e são
  validados byte a byte contra loopback/LAN/localhost. Mutações continuam
  protegidas por CSRF em tempo constante.
- **Bootstrap do menu em binário**: o Java publica para a DLL um blob
  `SVUB` (magic, versão 2, porta, gui_bind, raiz, CSRF e 16 apelidos de
  rota), sem nenhuma URL no caminho de dados.
- **QR do celular desenhado pelo navegador**: o servidor só publica um
  hint mascarado (`lan ^ porta`, Base64) e a porta; `app.js` reconstrói a
  URL e desenha o QR code (versões 1–6, EC-L) no heap do navegador. O
  `javaw` nunca contém a URL do celular.
- **Memória do JDK**: não varrer nem reescrever o heap do `javaw` para
  remover o texto de `net.properties`. Uma página que `VirtualQuery`
  apontou como legível pode ser liberada ou movida pelo GC antes da leitura;
  a antiga rotina `memscrub` causou `EXCEPTION_ACCESS_VIOLATION` em uma
  chamada nativa da API. O JRE pode manter suas próprias strings em memória;
  o cliente continua evitando construir URLs da API no heap Java.
- **Zeus local via bootstrap**: a porta do serviço local (8091) vem do bloco
  de bootstrap do injetor (V3), sem variável de ambiente.
