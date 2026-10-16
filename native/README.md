# SonyVegas 4.21 native test bridge

This directory contains an x64 Windows JNI/JVMTI bridge reconstructed from
the nine-method `RegisterNatives` table in `sample.dll`. It supports isolated
Minecraft 1.7.10 Forge, 1.8.9 Forge, and 1.12.2 Forge test instances, including
Forge-enabled Lunar Client injection. The Badlion Client 1.8.9 compatibility
path is described below; its runtime validation is separate from the build checks.
Minecraft 1.16.5 support is incomplete and may have mapping, rendering, and
module compatibility problems.

The authoritative bridge surface is:

```text
scb(Class, byte[]) : int
smd(int, int) : void
gks(int) : short
gkn(long) : String
mvk(int, int) : int
gcb(Class) : byte[]
gfb(String) : byte[]
trs(int) : void
inv(Method, Object, Object[]) : Object
```

The additional native declarations currently present in the recovered Java
class are not registered by `sample.dll`, and the PE has no export table or
second registration path. They are intentionally not invented here.

## Loader token handoff design difference

The local-service integration deliberately adds `gat()Ljava/lang/String;`
as a Product compatibility native while keeping its Java-visible name exactly
`gat`. It does not add a `native_gat()` Java method and does not change the
existing Java online, Zeus, friend, Party, or settings-sync implementations.
This tenth registration is not part of the nine-method `sample.dll` authority;
the legacy official DLL provides separate evidence for native `gat()`, but its
implementation used controller command `0x269` over a persistent EXE socket.

The Product design has two explicit launch modes:

- Direct `SonyVegas421Injector.exe` injection has no Loader bootstrap, so native
  `gat()` returns the string `"0"`.
- Loader startup obtains a long-lived token from the loopback Service by
  username and exposes it to `SonyVegas421Native.dll` through the temporary
  loopback controller socket. The DLL requests it with command `0x269`, caches
  it for `gat()`, reports `trs(step)` with `0x25c`, and reports completion with
  `0x25e`. The Loader remains open through the Finished Loading page.

The Service does not create a token-`"0"` developer account, performs no HWID
check, and reuses the existing long-lived token for a case-insensitive username
match. Because current Java initialization uses `gat()` for
`/api/v1/{token}/authenticated`, direct mode is only guaranteed to return the
standalone sentinel `"0"`; without changes to Java initialization it may stop
when that token is rejected or the Service is absent.

The versioned named-memory block is created before DLL injection and carries
only the controller port and Service endpoints; it never contains the token.
The token and loading state use the decomp-supported controller commands over
loopback. The full design is documented at
`../../native_method_research/loader_product_token_handoff_design.md`.

## Badlion Client 1.8.9 compatibility

The Badlion path was compared with `projeto tampinhas 2.0`. Both projects use
a Windows x64 DLL, JNI/JVMTI, the Minecraft client thread's ClassLoader, and
the canonical vanilla 1.8.9 mappings. The existing native bridge and Java
runtime detection already contain that compatibility logic.

For Badlion, the embedded JAR follows the same diskless path as CheatBreaker:
the native bootstrap reads the DLL resource, defines the payload classes in
the game's `LaunchClassLoader`, and keeps non-class resources in
`MemoryJarLoader`. `NativeBridge.start()` is called from the attached bootstrap
worker; the loader does not replace `WindowsDisplay.nUpdate`.

CheatBreaker mapping profiles are also diskless. The loader downloads the
seven mapping resources into RAM, publishes a versioned named-memory block for
the selected Java process, and waits for the DLL to copy and acknowledge it.
`PayloadResources` resolves those entries through the registered native bridge;
the loader then wipes and closes its shared view. No mapping cache or temporary
JAR is created under `%TEMP%` or `%LOCALAPPDATA%`.

The restored `Badlion189Render2DMappingTask` adds the HUD callback immediately
before `Display.update(boolean)` calls `swapBuffers()`, as in the reference
project. `PrimaryMappingTaskSet` enables it only for the Badlion 1.8.9 runtime;
`EntityRendererEventMappingTask` then skips the usual vanilla HUD callback so
the HUD is drawn once. Existing render-thread frame initialization, GUI input,
HUD layout, modules and settings continue through the current SonyVegas code.

The callback owner comes from `EventRender2D.class.getName()` so it follows
the production class renaming. Copying the reference's literal `gg/vape/...`
owner would leave a callback that the final payload cannot resolve.

Native callback retention also uses generated package prefixes for events,
mapping callbacks and chat input. Those prefixes are exported alongside the
bootstrap class names in `renamed_classes.txt`, so the existing JVMTI
retransformation bookkeeping recognizes the production callbacks and removes
retained definitions when callback-free original bytecode is restored.

`badlionRenderHookProbe` runs against the final injection JAR, including its
relocated ASM and real bundled LWJGL `Display` bytecode. It checks task
registration, mapping resources, callback resolution and position, repeated
application, and rejection of missing methods or duplicate callbacks. Every
`prepareInjectionBundle` build includes this check. These are offline checks;
they do not establish successful injection in a running Badlion release.

For delivery, upload `build/injection/IllIIlIl.dll` to the VPS used by the
loader and start a fresh Badlion 1.8.9 x64 process before loading it. The
default `unknown.exe` flow still downloads the authenticated remote payload;
rebuilding the DLL alone does not replace the version served by the VPS.

## Build

The embedded HUD position editor uses the game's existing client viewport.
Entering or leaving it must not resize, move, or make the Minecraft window
topmost. Its positioning helpers only operate on a separate desktop overlay.
The Java HUD reads normalized draft positions directly through JNI once per
rendered frame, so the actual modules follow the drag without waiting for
HTTP or a game tick. HUD moves use separate `x` and `y` form fields; the final mouse
release sends `commit=true` to capture and save the active profile. Local drag
positions remain visible until that final request completes. Losing focus or
closing the editor also commits the last valid drag position.

Notifications use the saved left/right edge for alignment and slide direction,
and the saved top/bottom edge for stack direction. Different card widths share
the selected edge, and their duration bars follow the same horizontal anchor.

Payload renaming preserves every method declared `native` automatically.
The `hps([F)I` frame-position bridge must retain that name for `RegisterNatives`;
renaming it causes bootstrap failure code 10 before the client starts.

Restart the game when replacing an older DLL: an already loaded version can
retain the window size and topmost state it applied earlier.

Use Gradle 8.8 from `product` to build the Java 8 payload, embed all remotely
managed runtime dependencies, compile the native targets, and assemble the
bundle:

```powershell
.\gradlew.bat prepareInjectionBundle -PtargetRelease=8 `
  -PnativeJavaHome="C:\Program Files\Java\jdk1.8.0_301"
```

For native-only development, invoke CMake directly with the injection JAR:

```powershell
cmake -S . -B build -A x64 `
  -DSONYVEGAS421_JAVA_HOME="C:\Program Files\Java\jdk1.8.0_301" `
  -DSONYVEGAS421_PRODUCT_JAR="..\build\libs\sonyvegas-1.0-injection.jar"
cmake --build build --config Release
```

Outputs are written to `build/dist`:

- `IllIIlIl.dll`
- `unknown.exe`

### Native string protection

Runtime strings in every source used by the loader executable are wrapped with
`SV_STR` or `SV_WSTR`. The build generator encrypts them as 64-bit XOR + NOT
blocks with per-block keys, following the design of
[Refactored-Xor-String](https://github.com/obseletecode72/Refactored-Xor-String)
while retaining C and Win32 compatibility. The generated header exists only in
the build directory, and decrypted buffers are cleared before their TLS slots
are reused.

The CMake build checks all loader sources and fails if a new unprotected runtime
literal is introduced. To wrap newly added literals, run:

```powershell
python tools/wrap_native_strings.py native/injector.c native/cleanup.c `
  native/loader_ui.c native/control_api_client.c native/hwid.c native/manual_map.c
```

## Direct injection

`SonyVegas421Native.dll` contains the recovered Java product as an `RCDATA`
resource. Start a supported Forge instance, or a Forge-enabled Lunar Client
instance, with a 64-bit JVM, then run the injector from the bundle directory:

```powershell
unknown.exe
```

The `unknown` injector opens the graphical loader used for license
verification, target selection, injection, and the final result. It refreshes
its list of visible `java.exe` and `javaw.exe` windows and displays their client
names. The complete bundle uses the companion `IllIIlIl.dll` beside the loader
by default, which makes a newly built bundle testable without downloading an
older VPS release. Use `--remote` to explicitly select the authenticated VPS
payload instead. The original non-interactive console injector remains available
for scripts in source builds:

```powershell
unknown.exe <pid> IllIIlIl.dll
```

Startup is successful only when Java's `NativeBridge.isStartCompleted()` is
true. Both the LWJGL render thread and worker bootstrap enforce this check;
an authorization failure that returns normally must not open the self destruct
screen. The completion method name stays in the obfuscation keep list. The
loader reports an authorization reason code and the Java version on failure,
without exposing the session token or request URL.

If the JVM's account request fails with a connection failure or socket timeout,
startup makes one bounded attempt through native WinHTTP (`agi`). The same
protected API host and Windows transport used by the loader are used, with
normal certificate checks, redirects disabled, a 64 KiB response limit and
5/5/10/10 second resolve/connect/send/receive timeouts. HTTP errors, TLS errors
and invalid account/license data do not trigger this second transport. A
successful Windows response still passes the existing RSA license verifier.
If Windows also fails, the loader displays a numeric `winhttp_` or
`native_http_` diagnostic without printing the token or request URL.

Payload downloads accept up to 32 MiB in existing loaders; startup account
data retains its 64 KiB limit. Protected release DLLs must fit within 32 MiB.
The VMProtect projects in `native/vmprotect-dll.vmp` and
`native/vmprotect-exe.vmp` use the 2.X virtual machine and leave output
packing disabled. The protection task rejects larger DLLs before publication.

Once mapped, the DLL worker waits for the JVM and Minecraft `Client thread`,
loads its embedded product JAR through the client-specific route described
above, and requires `NativeBridge` to resolve from the game loader. On the
in-memory route, host-owned Badlion libraries are resolved from the game
ClassLoader and unavailable optional platform classes are skipped. The bridge,
core payload and Badlion render-hook classes are then verified; bootstrap
fails when a required hook is unavailable. The generated `MemoryJarLoader`
remains the authoritative resource loader.
The worker then registers the nine authoritative methods plus the
Product `gat()` compatibility native and calls
`NativeBridge.start()` automatically. No second command or start flag is
required. Production builds do not write bootstrap or menu diagnostic logs.

The graphical bundle prefers its companion DLL. Remote delivery remains
available through `unknown.exe --remote`; VPS administration steps are
documented in `vps/README.md` in the source tree.

The injection payload is compiled with `--release 8`; its project classes use
class-file major version 52. Runtime dependencies are resolved from the
repositories declared in Gradle and merged into that payload, not restored as
vendored source directories. The injector rejects non-x64 processes.

## Native overlay module menu

After a successful injection, the DLL creates a separate native overlay while
the loader remains its own standalone window. The overlay uses a global menu
bind and can therefore be opened while Minecraft is unfocused or minimized.
RSHIFT is the recovery/default bind; click the `Menu` bind control to replace it
with a keyboard key or mouse button. Module and setting binds use the same
capture flow.

The menu reads every visible module, category and serializable setting from the
running client. It supports module toggles, booleans, numeric and range sliders,
modes, colors, lists, text and binds. Changes save through the existing profile
path, and the background refresh keeps the native menu in sync with changes made
inside the client. Closing the overlay hides it; pressing the menu bind shows it
again. The loader keeps its existing stream mode and self-destruct controls.

The Java client and loader exchange state through a loopback-only local service.
Its session path, CSRF token, route names, module identifiers and value
identifiers are randomized for every injection. No browser is launched and the
service is not exposed to the LAN.

To use another unprivileged port, set `SONYVEGAS_LOCAL_CONTROL_PORT` before
starting the launcher and Minecraft, then restart both processes:

```powershell
setx SONYVEGAS_LOCAL_CONTROL_PORT "4211"
```

## Friends, Notifications and Party Target

With Target's `Safe Hits` enabled, manual hits on clan allies proceed normally
in every Target mode, including Invsee and Party following. Allies retain
their normal hitboxes and remain visible; unrelated non-target players keep
the existing pass-through behavior.

Aura's `Ignore in Web` ignores players only when the central third of their
horizontal body footprint is covered by cobwebs at a body height. The top and
bottom 0.1 blocks are excluded from the scan. Grazing a web with a side,
corner, head or feet permits attacks; adjacent webs are treated continuously.

Friends is managed exclusively in `Settings > Friends`, using the persisted
friend manager and existing middle-click bind. It is no longer a module in
Misc, the ArrayList, profile module lists, or Party module synchronization.

`Settings > Other > Notifications` hides all client notifications, clears
queued notices, and suppresses their sounds. The choice uses the existing
saved client settings; enabling it again does not replay hidden notices.

While in a Party, members automatically follow the leader's Target selection
and enabled state. Settings are applied before activation; personal binds and
ticket-specific Invsee codes/reports are not shared. The leader sends changes
without requiring `Sync selected modules`. If updates expire, followers pause
attacks instead of reverting to their previous local target. Both leader and
members need the updated DLL. The API keeps only the latest queued target.

## Cape diagnostics

Release builds emit no diagnostic files: cape mapping, hook, player, resource,
and texture selection diagnostics are disabled, so neither `%TEMP%` nor the
Desktop receives log files. The same holds for KeepRange, LagRange, and
TimerRange state changes, the native bridge log, and `imgui.ini` (disabled in
both the loader and the overlay contexts).
