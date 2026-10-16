package vegas.sony.mapping;

import vegas.sony.mapping.MappingClassBytecodeResolver;
import vegas.sony.reflect.Vanilla189Mappings;
import vegas.sony.runtime.ClassBytecodeCache;
import vegas.sony.runtime.NativeBridge;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import java.io.ByteArrayInputStream;

public class BytecodeClassPool
extends ClassPool {
    private final MappingClassBytecodeResolver W;

    private static Exception a(Exception exception) {
        return exception;
    }

    @Override
    public CtClass get(String string) throws NotFoundException {
        try {
            return super.get(string);
        }
        catch (NotFoundException notFoundException) {
            if (this.N(string)) {
                return super.get(string);
            }
            throw notFoundException;
        }
    }

    private static byte[] readAllBytes(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    public BytecodeClassPool(MappingClassBytecodeResolver mappingClassBytecodeResolver) {
        super(true);
        this.W = mappingClassBytecodeResolver;
    }

    @Override
    public CtClass getOrNull(String string) {
        CtClass ctClass = super.getOrNull(string);
        if (ctClass == null && this.N(string)) {
            ctClass = super.getOrNull(string);
        }
        return ctClass;
    }

    @Override
    public java.net.URL find(String classname) {
        java.net.URL url = super.find(classname);
        // find() serves resources WITHOUT validating the served bytes, so a
        // mismatched entry (Lunar serves notch originals for MCP paths)
        // poisons the cache and only detonates later in getClassFile3,
        // bypassing get()/N() entirely. Refuse poisoned game entries so
        // resolution falls through to the live loaders instead. Everything
        // else passes through byte-identical.
        if (url == null || classname == null
                || (!classname.startsWith("net.minecraft.") && !classname.startsWith("net.minecraftforge."))) {
            return url;
        }
        try (java.io.InputStream probe = url.openStream()) {
            if (probe == null) {
                return url;
            }
            byte[] bytes = readAllBytes(probe);
            String internal = new javassist.bytecode.ClassFile(
                    new java.io.DataInputStream(new ByteArrayInputStream(bytes))).getName().replace('/', '.');
            if (!classname.equals(internal)) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_FIND poison " + url + " serves " + internal + " for " + classname + " //$");
                return null;
            }
        } catch (Throwable ignored) {
            return url;
        }
        return url;
    }

    private boolean N(String string) {
        String className = string;
        if (className.startsWith("[")) {
            className = Descriptor.toClassName(className);
        }
        while (className.endsWith("[]")) {
            className = className.substring(0, className.length() - 2);
        }
        byte[] byArray = this.W.y(className);
        if (byArray == null) {
            // Notch runtimes (vanilla/Lunar/CheatBreaker 1.8.9) keep game
            // classes under obfuscated binary names while mapping code asks
            // for deobfuscated ones (and Lunar's module jars can shadow the
            // resource with a mismatched stub). Bridge through the live
            // mapping-aware resolver and re-key the bytes under the requested
            // name so stackmap computation can proceed.
            byArray = this.bridgeLiveBytes(className);
        } else {
            // Ichor/Genesis loaders can serve the ORIGINAL notch bytes for a
            // remapped (MCP) resource path. Javassist rejects bytes whose
            // internal name differs from the requested one, so re-key them
            // instead of caching poison.
            byArray = this.ensureInternalName(byArray, className);
        }
        if (byArray != null) {
            this.insertClassPath(new ByteArrayClassPath(className, byArray));
            return true;
        }
        return false;
    }

    private byte[] ensureInternalName(byte[] bytecode, String className) {
        if (bytecode == null || className == null) {
            return bytecode;
        }
        try {
            String internal = new javassist.bytecode.ClassFile(
                    new java.io.DataInputStream(new ByteArrayInputStream(bytecode))).getName().replace('/', '.');
            if (className.equals(internal)) {
                return bytecode;
            }
            ClassPool renamePool = new ClassPool(true);
            CtClass renamed = renamePool.makeClass(new ByteArrayInputStream(bytecode));
            renamed.replaceClassName(internal, className);
            byte[] fixed = renamed.toBytecode();
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE rekeyed " + internal + " as " + className + " //$");
            return fixed;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private byte[] bridgeLiveBytes(String className) {
        // TEMP-DEV: step logging to find why the Lunar Entity bridge misses.
        boolean trace = className != null && className.startsWith("net.minecraft.");
        try {
            if (className == null || className.indexOf(46) < 0) {
                return null;
            }
            if (className.startsWith("java.")
                    || className.startsWith("javax.")
                    || className.startsWith("jdk.")
                    || className.startsWith("sun.")) {
                return null;
            }
            // Notch runtimes (vanilla/Lunar/CheatBreaker 1.8.9) keep game
            // classes under obfuscated binary names while mapping code asks
            // for deobfuscated ones (and Lunar's module jars can shadow the
            // resource with a mismatched stub). Resolve MCP -> notch through
            // the already-loaded 1.8.9 SRG table, load the live class
            // mapping-aware, then re-key its bytes under the requested name
            // so Javassist stackmap computation can proceed.
            int version = -1;
            try {
                version = vegas.sony.wrapper.impl.ForgeVersion.c();
            } catch (Throwable versionFailure) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " version probe failed //$");
                }
                return null;
            }
            if (version != 15) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " skip version=" + version + " //$");
                }
                return null;
            }
            String notch;
            try {
                notch = Vanilla189Mappings.remapClassName(className);
            } catch (Throwable ignored) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " remap threw //$");
                }
                return null;
            }
            if (notch == null || notch.isEmpty()) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " notch=null //$");
                }
                return null;
            }
            Class<?> live = null;
            try {
                live = NativeBridge.gvc(notch);
            } catch (Throwable ignored) {
            }
            if (live == null) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " gvc null notch=" + notch + " //$");
                }
                return null;
            }
            if (className.equals(live.getName())) {
                return null;
            }
            byte[] liveBytes = ClassBytecodeCache.getClassBytecode(live, true);
            if (liveBytes == null) {
                if (trace) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " bytes null //$");
                }
                return null;
            }
            ClassPool bridgePool = new ClassPool(true);
            CtClass bridged = bridgePool.makeClass(new ByteArrayInputStream(liveBytes));
            bridged.replaceClassName(live.getName(), className);
            byte[] renamed = bridged.toBytecode();
            if (trace) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " bridged from " + live.getName() + " //$");
            }
            return renamed;
        } catch (Throwable ignored) {
            if (trace) {
                vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_BRIDGE " + className + " outer fail //$");
            }
            return null;
        }
    }
}
