import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import matcher.Matcher;
import matcher.config.Config;
import matcher.config.ProjectConfig;
import matcher.type.*;
import matcher.serdes.MatchesIo;
import matcher.classifier.ClassifierUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Headless driver for FabricMC Matcher 0.1.0; never loads game classes. */
public final class MatchCheatBreaker {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("vanilla.jar cb.patch output-dir [resume.matches]");
        Path output = Paths.get(args[2]);
        Files.createDirectories(output);
        Matcher.init();
        ClassEnvironment env = new ClassEnvironment();
        env.assumeBothOrNoneObfuscated = false;
        Matcher matcher = new Matcher(env);
        ProjectConfig config = new ProjectConfig.Builder(
                Collections.singletonList(Paths.get(args[0])),
                Collections.singletonList(Paths.get(args[1])))
                .classPathA(Collections.emptyList())
                .classPathB(Collections.emptyList())
                .sharedClassPath(Collections.emptyList())
                .saveUnmappedMatches(true)
                .nonObfuscatedMemberPatternA("")
                .nonObfuscatedMemberPatternB("")
                .nonObfuscatedClassPatternA("(?:com|org|io|net/minecraft/server)/.*")
                .nonObfuscatedClassPatternB("(?:com|org|io|net/minecraft/server)/.*")
                .build();
        Config.setProjectConfig(config);
        System.out.println("Loading bytecode...");
        matcher.init(config, progress -> {});
        if (args.length > 3 && !args[3].equals("-")) {
            MatchesIo.read(Paths.get(args[3]), Arrays.asList(
                    Paths.get(args[0]).toAbsolutePath().getParent(),
                    Paths.get(args[1]).toAbsolutePath().getParent()), false, matcher, progress -> {});
        }
        if (args.length > 4 && !args[4].equals("-")) {
            int seeded = 0;
            for (String line : Files.readAllLines(Paths.get(args[4]), StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] columns = line.split("\t");
                boolean replace = columns[0].startsWith("!");
                if (replace) columns[0] = columns[0].substring(1);
                if (columns.length == 5) {
                    ClassInstance owner = env.getLocalClsByNameA(columns[1]);
                    if (owner == null || owner.getMatch() == null) throw new IllegalArgumentException("Unknown owner " + line);
                    String desc = targetDesc(env, columns[3]);
                    MemberInstance<?> a = columns[0].equals("FD") ? owner.getField(columns[2], columns[3]) : owner.getMethod(columns[2], columns[3]);
                    MemberInstance<?> b = columns[0].equals("FD") ? owner.getMatch().getField(columns[4], desc) : owner.getMatch().getMethod(columns[4], desc);
                    if (a == null || b == null || !a.isReal() || !b.isReal()) throw new IllegalArgumentException("Unknown member " + line);
                    if (replace && a.getMatch() != b) { if (a.getMatch() != null) matcher.unmatch(a); if (b.getMatch() != null) matcher.unmatch(b); }
                    if (a.getMatch() != null && a.getMatch() != b || b.getMatch() != null && b.getMatch() != a)
                        throw new IllegalArgumentException("Conflicting member seed " + line);
                    if (a.getMatch() == null) { matcher.match(a, b); seeded++; }
                    continue;
                }
                if (columns.length != 2) throw new IllegalArgumentException("Invalid seed " + line);
                ClassInstance a = env.getLocalClsByNameA(columns[0]);
                ClassInstance b = env.getLocalClsByNameB(columns[1]);
                if (a == null || b == null) throw new IllegalArgumentException("Unknown seed " + line);
                if (replace && a.getMatch() != b) { if (a.getMatch() != null) matcher.unmatch(a); if (b.getMatch() != null) matcher.unmatch(b); }
                if (a.getMatch() != null && a.getMatch() != b || b.getMatch() != null && b.getMatch() != a)
                    throw new IllegalArgumentException("Conflicting seed " + line);
                if (a.getMatch() == null) { matcher.match(a, b); seeded++; }
            }
            System.out.println("Loaded independently verified class seeds: " + seeded);
        }
        if (args.length > 6 && !args[6].equals("-")) {
            Set<String> required = new HashSet<>();
            for (String line : Files.readAllLines(Paths.get(args[6]), StandardCharsets.UTF_8)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) required.add(line);
            }
            Set<String> inputNames = new HashSet<>();
            for (ClassInstance cls : env.getClassesA()) if (cls.isInput()) inputNames.add(cls.getName());
            for (int depth = 0; depth < 2; depth++) {
                Set<String> related = new HashSet<>();
                for (ClassInstance cls : env.getClassesA()) {
                    if (!cls.isInput() || !required.contains(cls.getName())) continue;
                    if (cls.getSuperClass() != null) related.add(cls.getSuperClass().getName());
                    for (ClassInstance iface : cls.getInterfaces()) related.add(iface.getName());
                    for (FieldInstance field : cls.getFields()) {
                        addDescriptorTypes(related, field.getDesc());
                    }
                    for (MethodInstance method : cls.getMethods()) {
                        addDescriptorTypes(related, method.getDesc());
                    }
                }
                related.retainAll(inputNames);
                if (required.addAll(related)) continue;
                break;
            }
            int excluded = 0;
            for (ClassInstance cls : env.getClassesA()) {
                if (cls.isInput() && !required.contains(cls.getName())) {
                    cls.setMatchable(false);
                    excluded++;
                }
            }
            System.out.println("Restricting automatic matching to " + required.size()
                    + " required runtime classes (excluded " + excluded + ")");
        }
        for (int pass = 1; pass <= 2; pass++) {
            System.out.println("Matching pass " + pass);
            if (args.length > 5 && args[5].equals("propagate")) {
                propagate(matcher);
                propagateMembers(matcher, output.resolve("member-evidence-" + pass + ".tsv"));
            }
            matcher.autoMatchAll(progress -> {});
            write(matcher, output, pass);
        }
        Matcher.threadPool.shutdown();
    }

    private static void addDescriptorTypes(Set<String> names, String descriptor) {
        java.util.regex.Matcher types = java.util.regex.Pattern.compile("L([^;]+);").matcher(descriptor);
        while (types.find()) names.add(types.group(1));
    }

    private static void propose(ClassEnvironment env, Map<ClassInstance, Map<ClassInstance, Set<String>>> votes,
            String aName, String bName, String reason) {
        ClassInstance a = env.getLocalClsByNameA(aName), b = env.getLocalClsByNameB(bName);
        if (a == null || b == null || !a.isReal() || !b.isReal() || !a.isInput() || !b.isInput()
                || a.getMatch() != null || b.getMatch() != null || a.isArray() || b.isArray()) return;
        votes.computeIfAbsent(a, key -> new HashMap<>()).computeIfAbsent(b, key -> new HashSet<>()).add(reason);
    }

    private static void proposeType(ClassEnvironment env, Map<ClassInstance, Map<ClassInstance, Set<String>>> votes,
            Type a, Type b, String reason) {
        if (a.getSort() != b.getSort()) return;
        if (a.getSort() == Type.ARRAY) {
            if (a.getDimensions() != b.getDimensions()) return;
            a = a.getElementType(); b = b.getElementType();
        }
        if (a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT)
            propose(env, votes, a.getInternalName(), b.getInternalName(), reason);
    }

    private static int propagate(Matcher matcher) {
        ClassEnvironment env = matcher.getEnv();
        Map<ClassInstance, Map<ClassInstance, Set<String>>> votes = new HashMap<>();
        for (ClassInstance cls : env.getClassesA()) {
            if (!cls.isInput() || cls.getMatch() == null) continue;
            ClassInstance other = cls.getMatch();
            if (cls.getSuperClass() != null && other.getSuperClass() != null)
                propose(env, votes, cls.getSuperClass().getName(), other.getSuperClass().getName(), "super:" + cls.getName());
            for (FieldInstance field : cls.getFields()) {
                FieldInstance match = field.getMatch();
                if (!field.isReal() || match == null || !match.isReal()) continue;
                proposeType(env, votes, Type.getType(field.getDesc()), Type.getType(match.getDesc()), "field:" + field);
            }
            for (MethodInstance method : cls.getMethods()) {
                MethodInstance match = method.getMatch();
                if (!method.isReal() || match == null || !match.isReal()) continue;
                Type[] aTypes = Type.getArgumentTypes(method.getDesc()), bTypes = Type.getArgumentTypes(match.getDesc());
                if (aTypes.length == bTypes.length) for (int i = 0; i < aTypes.length; i++)
                    proposeType(env, votes, aTypes[i], bTypes[i], "arg:" + method + ":" + i);
                proposeType(env, votes, Type.getReturnType(method.getDesc()), Type.getReturnType(match.getDesc()), "return:" + method);
                if (method.getAsmNode() == null || match.getAsmNode() == null) continue;
                AbstractInsnNode[] aInsns = method.getAsmNode().instructions.toArray();
                AbstractInsnNode[] bInsns = match.getAsmNode().instructions.toArray();
                if (aInsns.length == 0 || bInsns.length == 0 || (long)aInsns.length * bInsns.length > 12000000) continue;
                int[] alignment = ClassifierUtil.mapInsns(method, match);
                if (alignment == null) continue;
                for (int i = 0; i < alignment.length; i++) {
                    int j = alignment[i];
                    if (j < 0 || j >= bInsns.length || i >= aInsns.length) continue;
                    AbstractInsnNode a = aInsns[i], b = bInsns[j];
                    if (a.getOpcode() != b.getOpcode()) continue;
                    String reason = "insn:" + method + ":" + i;
                    if (a instanceof TypeInsnNode && b instanceof TypeInsnNode) {
                        propose(env, votes, ((TypeInsnNode)a).desc, ((TypeInsnNode)b).desc, reason);
                    } else if (a instanceof MethodInsnNode && b instanceof MethodInsnNode) {
                        MethodInsnNode am = (MethodInsnNode)a, bm = (MethodInsnNode)b;
                        if (am.name.equals("<init>") == bm.name.equals("<init>")
                                && am.desc.replaceAll("L[^;]+;", "L@;").equals(bm.desc.replaceAll("L[^;]+;", "L@;")))
                            propose(env, votes, am.owner, bm.owner, reason);
                    } else if (a instanceof FieldInsnNode && b instanceof FieldInsnNode) {
                        FieldInsnNode af = (FieldInsnNode)a, bf = (FieldInsnNode)b;
                        if (af.desc.replaceAll("L[^;]+;", "L@;").equals(bf.desc.replaceAll("L[^;]+;", "L@;")))
                            propose(env, votes, af.owner, bf.owner, reason);
                    } else if (a instanceof LdcInsnNode && b instanceof LdcInsnNode) {
                        Object ac = ((LdcInsnNode)a).cst, bc = ((LdcInsnNode)b).cst;
                        if (ac instanceof Type && bc instanceof Type) proposeType(env, votes, (Type)ac, (Type)bc, reason);
                    }
                }
            }
        }
        Map<ClassInstance, Set<ClassInstance>> reverse = new HashMap<>();
        votes.forEach((a, choices) -> choices.keySet().forEach(b -> reverse.computeIfAbsent(b, key -> new HashSet<>()).add(a)));
        int added = 0;
        for (Map.Entry<ClassInstance, Map<ClassInstance, Set<String>>> entry : votes.entrySet()) {
            if (entry.getValue().size() != 1) continue;
            ClassInstance b = entry.getValue().keySet().iterator().next();
            Set<String> evidence = entry.getValue().get(b);
            if (reverse.get(b).size() != 1 || evidence.size() < 2) continue;
            matcher.match(entry.getKey(), b);
            System.out.println("Type/call evidence " + entry.getKey().getName() + " -> " + b.getName() + " (" + evidence.size() + ")");
            added++;
        }
        System.out.println("Propagated class matches: " + added);
        return added;
    }

    private static String targetDesc(ClassEnvironment env, String desc) {
        java.util.regex.Matcher names = java.util.regex.Pattern.compile("L([^;]+);").matcher(desc);
        StringBuffer result = new StringBuffer();
        while (names.find()) {
            ClassInstance cls = env.getLocalClsByNameA(names.group(1));
            String target = cls != null && cls.getMatch() != null ? cls.getMatch().getName() : names.group(1);
            names.appendReplacement(result, java.util.regex.Matcher.quoteReplacement("L" + target + ";"));
        }
        return names.appendTail(result).toString();
    }

    private static void propagateMembers(Matcher matcher, Path report) throws IOException {
        ClassEnvironment env = matcher.getEnv();
        Map<MemberInstance<?>, Map<MemberInstance<?>, Set<String>>> votes = new HashMap<>();
        for (ClassInstance cls : env.getClassesA()) {
            if (!cls.isInput() || cls.getMatch() == null) continue;
            for (MethodInstance method : cls.getMethods()) {
                MethodInstance match = method.getMatch();
                if (!method.isReal() || match == null || !match.isReal()
                        || method.getAsmNode() == null || match.getAsmNode() == null) continue;
                AbstractInsnNode[] aa = method.getAsmNode().instructions.toArray();
                AbstractInsnNode[] bb = match.getAsmNode().instructions.toArray();
                if (aa.length == 0 || bb.length == 0 || (long)aa.length * bb.length > 12000000) continue;
                int[] alignment = ClassifierUtil.mapInsns(method, match);
                if (alignment == null) continue;
                for (int i = 0; i < alignment.length && i < aa.length; i++) {
                    int j = alignment[i];
                    if (j < 0 || j >= bb.length || aa[i].getOpcode() != bb[j].getOpcode()) continue;
                    String ao, bo, an, bn, ad, bd;
                    boolean field = aa[i] instanceof FieldInsnNode && bb[j] instanceof FieldInsnNode;
                    if (field) {
                        FieldInsnNode a = (FieldInsnNode)aa[i], b = (FieldInsnNode)bb[j];
                        ao = a.owner; bo = b.owner; an = a.name; bn = b.name; ad = a.desc; bd = b.desc;
                    } else if (aa[i] instanceof MethodInsnNode && bb[j] instanceof MethodInsnNode) {
                        MethodInsnNode a = (MethodInsnNode)aa[i], b = (MethodInsnNode)bb[j];
                        ao = a.owner; bo = b.owner; an = a.name; bn = b.name; ad = a.desc; bd = b.desc;
                    } else continue;
                    ClassInstance ac = env.getLocalClsByNameA(ao), bc = env.getLocalClsByNameB(bo);
                    if (ac == null || bc == null || !ac.isInput() || !bc.isInput() || ac.getMatch() != bc
                            || !targetDesc(env, ad).equals(bd)) continue;
                    MemberInstance<?> a = field ? ac.getField(an, ad) : ac.getMethod(an, ad);
                    MemberInstance<?> b = field ? bc.getField(bn, bd) : bc.getMethod(bn, bd);
                    if (a == null || b == null || !a.isReal() || !b.isReal() || a.isStatic() != b.isStatic()
                            || a.getMatch() != null || b.getMatch() != null) continue;
                    votes.computeIfAbsent(a, key -> new HashMap<>()).computeIfAbsent(b, key -> new HashSet<>())
                            .add(method.getCls().getName() + "/" + method.getId());
                }
            }
        }
        Map<MemberInstance<?>, Set<MemberInstance<?>>> reverse = new HashMap<>();
        votes.forEach((a, choices) -> choices.keySet().forEach(b -> reverse.computeIfAbsent(b, key -> new HashSet<>()).add(a)));
        List<String> lines = new ArrayList<>();
        int added = 0;
        for (Map.Entry<MemberInstance<?>, Map<MemberInstance<?>, Set<String>>> entry : votes.entrySet()) {
            MemberInstance<?> a = entry.getKey();
            for (Map.Entry<MemberInstance<?>, Set<String>> candidate : entry.getValue().entrySet()) {
                MemberInstance<?> b = candidate.getKey();
                boolean safe = entry.getValue().size() == 1 && reverse.get(b).size() == 1 && candidate.getValue().size() >= 2;
                lines.add((a instanceof FieldInstance ? "FD" : "MD") + "\t" + a.getCls().getName() + "\t" + a.getName()
                        + "\t" + a.getDesc() + "\t" + b.getCls().getName() + "\t" + b.getName() + "\t" + b.getDesc()
                        + "\t" + candidate.getValue().size() + "\t" + safe);
                if (safe && a.getMatch() == null && b.getMatch() == null) { matcher.match(a, b); added++; }
            }
        }
        Collections.sort(lines);
        Files.write(report, lines, StandardCharsets.UTF_8);
        System.out.println("Propagated members from mutually unique instruction references: " + added);
    }

    private static void write(Matcher matcher, Path output, int pass) throws IOException {
        int classes = 0, fields = 0, methods = 0;
        List<ClassInstance> ordered = new ArrayList<>(matcher.getEnv().getClassesA());
        ordered.sort(Comparator.comparing(ClassInstance::getName));
        Path matches = output.resolve("matches.tsv");
        Path temporary = output.resolve("matches.tsv.tmp");
        try (BufferedWriter w = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            w.write("# kind\tnotch owner\tnotch member\tnotch descriptor\truntime owner\truntime member\truntime descriptor\n");
            for (ClassInstance a : ordered) {
                ClassInstance b = a.getMatch();
                if (!a.isInput() || !a.isReal() || b == null || !b.isInput() || !b.isReal()) continue;
                w.write("CL\t" + a.getName() + "\t\t\t" + b.getName() + "\t\t\n");
                classes++;
                for (FieldInstance fa : a.getFields()) {
                    FieldInstance fb = fa.getMatch();
                    if (!fa.isReal() || fb == null || !fb.isReal()) continue;
                    w.write("FD\t" + a.getName() + "\t" + fa.getName() + "\t" + fa.getDesc()
                            + "\t" + fb.getCls().getName() + "\t" + fb.getName() + "\t" + fb.getDesc() + "\n");
                    fields++;
                }
                for (MethodInstance ma : a.getMethods()) {
                    MethodInstance mb = ma.getMatch();
                    if (!ma.isReal() || mb == null || !mb.isReal()) continue;
                    w.write("MD\t" + a.getName() + "\t" + ma.getName() + "\t" + ma.getDesc()
                            + "\t" + mb.getCls().getName() + "\t" + mb.getName() + "\t" + mb.getDesc() + "\n");
                    methods++;
                }
            }
        }
        Files.move(temporary, matches, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        MatchesIo.write(matcher, output.resolve("pass-" + pass + ".matches"));
        System.out.printf("Saved pass %d: classes=%d fields=%d methods=%d%n", pass, classes, fields, methods);
    }
}
