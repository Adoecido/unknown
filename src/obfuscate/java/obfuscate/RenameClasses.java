package obfuscate;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Renames every payload class and package segment to a CheatBreaker-style
 * identifier (only 'I' and 'l', e.g. IllIIlIlIl). References in bytecode,
 * descriptors and signatures are rewritten through ASM ClassRemapper.
 *
 * Deterministic: names are derived from a fixed seed, so rebuilds produce the
 * same mapping (required by the native DLL bootstrap strings).
 *
 * Writes RENAME_MAP.md (human-readable) and rename-map.txt (tooling) and the
 * native/renamed_classes.txt used by gen_obf.py to keep the DLL in sync.
 */
public class RenameClasses {

    private static final long SEED = 0x5EEDCAFEL;
    private static final int NAME_LENGTH = 18;
    private static final long NAME_MASK = (1L << NAME_LENGTH) - 1;
    private static final long NAME_XOR = new Random(SEED ^ 0x0BFCADDL).nextLong() & NAME_MASK;
    private static final int[] BIT_PERM;

    static {
        BIT_PERM = new int[NAME_LENGTH];
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < NAME_LENGTH; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions, new Random(SEED ^ 0x1E4A7E5L));
        for (int i = 0; i < NAME_LENGTH; i++) {
            BIT_PERM[i] = positions.get(i);
        }
    }

    private static int nameCounter;

    private static final java.util.Set<String> METHOD_KEEP = new java.util.HashSet<>(java.util.Arrays.asList(
            "start", "isStartCompleted", "buildPackageRoutingMap", "om", "wh", "ov",
            "scb", "smd", "gks", "gkn", "mvk", "cpy", "gcb", "glc", "gfb", "srl", "trs", "inv",
            "gat", "gzp", "rct", "cca", "rie", "slw", "dsv2", "ss_2", "mfv2", "ss", "sce", "blr",
            "agi", "acb",
            "loadMappings", "initializeManagers",
            "toString", "equals", "hashCode", "clone", "finalize", "getClass",
            "notify", "notifyAll", "wait", "values", "valueOf", "$deserializeLambda$",
            "create", "call", "check", "flip",
            // These names are embedded in Javassist source snippets. They must
            // remain stable because Javassist compiles the snippets at runtime,
            // after the bytecode remapper has already run.
            "filter", "override", "forceCapeVisible", "renderPlayerEnter", "renderPlayerExit", "capeLayerEnter", "capeLayerExit", "markCustomCape", "renderCustomCape", "process", "getRotationYaw", "getRotationPitch", "isOnGround",
            "getScores", "getVec", "isActive", "getBlockLayer", "getResult", "isResult",
            "getOutputContentComponent", "getVector", "getX", "getY", "getZ",
            "getTooltip", "getPacketInstance", "getDisplayName", "getDisplayNameInstance",
            "getRawDisplayName", "getWorldTime", "getVisibility",
            // Mode names cross the Java/native control-panel boundary.  Keep
            // these method names stable so a mode value cannot disappear from
            // the panel when the injected JVM resolves a parent class first.
            "getSerializedName", "getSectionSignStrippedName", "stripFormatting"));

    private static final java.util.Set<String> FIELD_KEEP = new java.util.HashSet<>(java.util.Arrays.asList(
            "serialVersionUID", "$VALUES", "ENUM$VALUES"));

    private static boolean memberRenamable(String owner,
            Map<String, String> superNames, Map<String, String[]> interfaces,
            Map<String, Boolean> cache) {
        Boolean cached = cache.get(owner);
        if (cached != null) {
            return cached.booleanValue();
        }
        if (!superNames.containsKey(owner)) {
            cache.put(owner, Boolean.FALSE);
            return false;
        }
        String superName = superNames.get(owner);
        boolean result = superName == null || superName.equals("java/lang/Object")
                || memberRenamable(superName, superNames, interfaces, cache);
        if (result) {
            String[] ownerInterfaces = interfaces.get(owner);
            for (String iface : ownerInterfaces) {
                if (!memberRenamable(iface, superNames, interfaces, cache)) {
                    result = false;
                    break;
                }
            }
        }
        cache.put(owner, Boolean.valueOf(result));
        return result;
    }

    private static boolean declaredInRenamableAncestor(String owner, String name,
            Map<String, String> superNames, Map<String, String[]> interfaces,
            Map<String, Set<String>> declaredMethods, Map<String, Boolean> cache) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(owner);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            if (superNames.containsKey(current)
                    && memberRenamable(current, superNames, interfaces, cache)
                    && declaredMethods.get(current).contains(name)) {
                return true;
            }
            String superName = superNames.get(current);
            if (superName != null && !visited.contains(superName)) {
                queue.add(superName);
            }
            String[] ownerInterfaces = interfaces.get(current);
            if (ownerInterfaces != null) {
                for (String iface : ownerInterfaces) {
                    if (!visited.contains(iface)) {
                        queue.add(iface);
                    }
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: RenameClasses <classesDir> <mapMarkdown> <mapTxt> <nativeSync>");
            System.exit(1);
        }
        Path root = Paths.get(args[0]);
        Path markdown = Paths.get(args[1]);
        Path mapTxt = Paths.get(args[2]);
        Path nativeSync = Paths.get(args[3]);

        List<Path> classFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            classFiles = stream.filter(p -> p.toString().endsWith(".class"))
                    .sorted().collect(Collectors.toList());
        }
        if (classFiles.isEmpty()) {
            System.err.println("no class files under " + root);
            System.exit(2);
        }

        List<String> originalNames = new ArrayList<>();
        Map<String, byte[]> classBytes = new HashMap<>();
        Map<String, String> superNames = new HashMap<>();
        Map<String, String[]> interfaces = new HashMap<>();
        Map<String, Set<String>> declaredMethods = new HashMap<>();
        for (Path file : classFiles) {
            byte[] data = Files.readAllBytes(file);
            ClassReader reader = new ClassReader(data);
            String name = reader.getClassName();
            originalNames.add(name);
            classBytes.put(name, data);
            superNames.put(name, reader.getSuperName());
            interfaces.put(name, reader.getInterfaces());
            Set<String> methods = new HashSet<>();
            reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                        String descriptor, String signature, String[] exceptions) {
                    methods.add(name);
                    // RegisterNatives resolves the declared Java name. Discover
                    // native entries before remapping so new bridge methods do
                    // not depend on updating the hand-maintained keep list.
                    if ((access & Opcodes.ACC_NATIVE) != 0) METHOD_KEEP.add(name);
                    return null;
                }
            }, 0);
            declaredMethods.put(name, methods);
        }

        Random rng = new Random(SEED);
        Map<String, String> pkgMapping = new TreeMap<>();
        List<String> packages = originalNames.stream()
                .map(RenameClasses::packageOf)
                .distinct().sorted().collect(Collectors.toList());
        for (String pkg : packages) {
            pkgMapping.put(pkg, generatePackageName(pkg, rng));
        }
        Map<String, String> mapping = new HashMap<>();
        for (String name : originalNames) {
            mapping.put(name, pkgMapping.get(packageOf(name)) + "/"
                    + generateSimpleName(classNameOf(name), rng));
        }

        Map<String, String> renamedMembers = new HashMap<>();
        Map<String, Boolean> memberRenamableCache = new HashMap<>();
        memberRenamableCache.put("java/lang/Object", Boolean.TRUE);
        for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
            byte[] data = entry.getValue();
            ClassReader reader = new ClassReader(data);
            String newName = mapping.get(reader.getClassName());
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            Remapper remapper = new Remapper() {
                @Override
                public String map(String internalName) {
                    String mapped = mapping.get(internalName);
                    return mapped != null ? mapped : internalName;
                }

                @Override
                public String mapMethodName(String owner, String name, String descriptor) {
                    if (name.equals("<init>") || name.equals("<clinit>")) {
                        return name;
                    }
                    boolean renamable = memberRenamable(owner, superNames, interfaces,
                            memberRenamableCache)
                            || declaredInRenamableAncestor(owner, name, superNames, interfaces,
                                    declaredMethods, memberRenamableCache);
                    if (renamable && !METHOD_KEEP.contains(name)) {
                        return renamedMembers.computeIfAbsent(name,
                                k -> nextName());
                    }
                    return name;
                }

                @Override
                public String mapFieldName(String owner, String name, String descriptor) {
                    if (memberRenamable(owner, superNames, interfaces, memberRenamableCache)
                            && !FIELD_KEEP.contains(name)) {
                        return renamedMembers.computeIfAbsent(name,
                                k -> nextName());
                    }
                    return name;
                }

                @Override
                public String mapInvokeDynamicMethodName(String name, String descriptor) {
                    if (name.equals("<init>") || name.equals("<clinit>")
                            || METHOD_KEEP.contains(name)) {
                        return name;
                    }
                    String returnType = descriptor.substring(descriptor.lastIndexOf(')') + 1);
                    if (!returnType.startsWith("L")) {
                        return name;
                    }
                    String iface = returnType.substring(1, returnType.length() - 1);
                    boolean renamable = memberRenamable(iface, superNames, interfaces,
                            memberRenamableCache)
                            || declaredInRenamableAncestor(iface, name, superNames, interfaces,
                                    declaredMethods, memberRenamableCache);
                    if (renamable) {
                        return renamedMembers.computeIfAbsent(name,
                                k -> nextName());
                    }
                    return name;
                }
            };
            reader.accept(new DebugStripper(writer, remapper), 0);
            byte[] out = writer.toByteArray();
            Path target = root.resolve(newName + ".class");
            Files.createDirectories(target.getParent());
            Files.write(target, out);
        }

        for (Path file : classFiles) {
            Files.deleteIfExists(file);
        }

        List<Path> directories;
        try (Stream<Path> stream = Files.walk(root)) {
            directories = stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparingInt(p -> -p.getNameCount()))
                    .collect(Collectors.toList());
        }
        for (Path directory : directories) {
            if (!directory.equals(root)) {
                try {
                    Files.deleteIfExists(directory);
                } catch (java.nio.file.DirectoryNotEmptyException ignored) {
                }
            }
        }

        writeMapFiles(mapping, originalNames, markdown, mapTxt, nativeSync);
        System.out.println("renamed classes=" + mapping.size());
    }

    private static String packageOf(String internal) {
        int index = internal.lastIndexOf('/');
        return index < 0 ? "" : internal.substring(0, index);
    }

    private static String classNameOf(String internal) {
        int index = internal.lastIndexOf('/');
        return index < 0 ? internal : internal.substring(index + 1);
    }

    private static String generatePackageName(String pkg, Random rng) {
        String[] segments = pkg.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(nextName());
        }
        return builder.toString();
    }

    private static String generateSimpleName(String simple, Random rng) {
        StringBuilder builder = new StringBuilder();
        String[] parts = simple.split("\\$", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('$');
            }
            builder.append(nextName());
        }
        return builder.toString();
    }

    private static String nextName() {
        if (nameCounter >= (1 << NAME_LENGTH)) {
            throw new IllegalStateException("exhausted " + NAME_LENGTH + "-bit name space");
        }
        long permuted = 0L;
        long value = (nameCounter ^ NAME_XOR) & NAME_MASK;
        for (int i = 0; i < NAME_LENGTH; i++) {
            if ((value & (1L << i)) != 0L) {
                permuted |= 1L << BIT_PERM[i];
            }
        }
        StringBuilder builder = new StringBuilder(NAME_LENGTH);
        for (int i = 0; i < NAME_LENGTH; i++) {
            builder.append((permuted & (1L << i)) != 0L ? 'l' : 'I');
        }
        nameCounter++;
        return builder.toString();
    }

    private static void writeMapFiles(Map<String, String> mapping, List<String> sorted,
                                      Path markdown, Path mapTxt, Path nativeSync)
            throws IOException {
        sorted.sort(String::compareTo);
        StringBuilder md = new StringBuilder();
        md.append("# Mapeamento de renomeacao (CheatBreaker-style)\n\n");
        md.append("Gerado por `RenameClasses` (ASM ClassRemapper). Deterministico "
                + "(seed fixa) - cada rebuild produz os mesmos nomes.\n\n");
        md.append("Algoritmo: nome original -> novo identificador (so `I` maiusculo "
                + "e `l` minusculo). Pacotes preservam a profundidade original; "
                + "classes internas preservam o `$`. Todas as classes de um mesmo "
                + "pacote original compartilham o MESMO novo pacote (acesso "
                + "package-private preservado).\n\n");
        md.append("> AVISO: este arquivo documenta o mapeamento interno do payload. "
                + "Nao embuti-lo no produto final.\n\n");
        md.append("## Classes referenciadas pela DLL nativa (bootstrap)\n\n");
        md.append("Estes nomes sao usados pelo `dllmain.c` e sincronizados "
                + "automaticamente via `native/renamed_classes.txt` + `gen_obf.py`:\n\n");
        md.append("| Ident (obf_strings.txt) | Nome antigo | Nome novo |\n");
        md.append("|---|---|---|\n");
        String[] critical = {
                "vegas/sony/runtime/NativeBridge",
                "vegas/sony/runtime/ForgePayloadClassLoader",
                "vegas/sony/event/impl/EventRenderWorldPassExecutorDrain",
                "vegas/sony/SonyVegas",
                "vegas/sony/mapping/PrimaryMappingTaskSet",
                "vegas/sony/event/impl/EventRender2D",
                "vegas/sony/mapping/Badlion189Render2DMappingTask",
        };
        Map<String, String> criticalMap = new HashMap<>();
        for (String name : critical) {
            String mapped = mapping.get(name);
            if (mapped != null) {
                criticalMap.put(name, mapped);
            }
        }
        String[][] identFor = {
                { "vegas/sony/event/impl/EventRenderWorldPassExecutorDrain", "EVENT_RENDER_WORLD_DRAIN" },
                { "vegas/sony/runtime/NativeBridge", "NATIVE_BRIDGE_CLASS" },
                { "vegas/sony/runtime/ForgePayloadClassLoader", "FORGE_PAYLOAD_CLASS_LOADER" },
        };
        for (String[] pair : identFor) {
            String mapped = criticalMap.get(pair[0]);
            if (mapped != null) {
                md.append("| ").append(pair[1]).append(" | `").append(pair[0])
                        .append("` | `").append(mapped).append("` |\n");
            }
        }
        md.append("\n## Mapeamento completo\n\n");
        md.append("| Classe original | Classe nova |\n");
        md.append("|---|---|\n");
        for (String original : sorted) {
            md.append("| `").append(original).append("` | `")
                    .append(mapping.get(original)).append("` |\n");
        }
        for (Path outFile : new Path[]{markdown, mapTxt, nativeSync}) {
            Files.createDirectories(outFile.toAbsolutePath().getParent());
        }
        Files.write(markdown, md.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder txt = new StringBuilder();
        for (String original : sorted) {
            txt.append(original).append('=').append(mapping.get(original)).append('\n');
        }
        Files.write(mapTxt, txt.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder sync = new StringBuilder();
        for (String name : criticalMap.keySet()) {
            String mapped = criticalMap.get(name);
            sync.append(name).append(' ').append(mapped).append('\n');
            sync.append(name.replace('/', '.')).append(' ')
                    .append(mapped.replace('/', '.')).append('\n');
        }
        // Native callback retention must recognize the same runtime packages
        // as the Java hooks. The root package alone has no single renamed form:
        // each source package receives its own path during renaming.
        String[] callbackRepresentatives = {
                "vegas/sony/event/impl/EventRender2D",
                "vegas/sony/mapping/EventPreTickCallback",
                "vegas/sony/input/ChatCommandHandler"
        };
        for (String name : callbackRepresentatives) {
            String mapped = mapping.get(name);
            if (mapped != null) {
                sync.append(packageOf(name)).append('/').append(' ')
                        .append(packageOf(mapped)).append('/').append('\n');
            }
        }
        Files.write(nativeSync, sync.toString().getBytes(StandardCharsets.UTF_8));
    }

    static final class DebugStripper extends ClassRemapper {
        DebugStripper(ClassVisitor cv, Remapper remapper) {
            super(Opcodes.ASM9, cv, remapper);
        }

        @Override
        public void visitSource(String source, String debug) {
            // Minecraft 1.8.9's CrashReportCategory calls getFileName().equals(...)
            // without checking for null. Preserve a non-sensitive placeholder so
            // payload exceptions can still be reported safely.
            super.visitSource("Source.java", null);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitLocalVariable(String name, String descriptor,
                        String signature, org.objectweb.asm.Label start,
                        org.objectweb.asm.Label end, int index) {
                }

                @Override
                public void visitParameter(String name, int access) {
                }

                @Override
                public void visitAttribute(org.objectweb.asm.Attribute attribute) {
                    if (attribute.type == null
                            || ("LocalVariableTable".equals(attribute.type))
                            || ("LocalVariableTypeTable".equals(attribute.type))) {
                        return;
                    }
                    super.visitAttribute(attribute);
                }
            };
        }

        @Override
        public void visitAttribute(org.objectweb.asm.Attribute attribute) {
            if (attribute.type == null || "SourceDebugExtension".equals(attribute.type)) {
                return;
            }
            super.visitAttribute(attribute);
        }
    }
}
