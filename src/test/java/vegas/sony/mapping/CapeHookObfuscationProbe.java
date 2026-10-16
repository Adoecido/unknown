package vegas.sony.mapping;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import vegas.sony.reflect.CheatBreaker189Mappings;

/** Reproduces the production renamer + Javassist snippet path, without installing a hook. */
public final class CapeHookObfuscationProbe {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("sonyvegas-cape-hook-probe-");
        Path classes = temp.resolve("classes");
        String original = CapeTextureOverrideHook.class.getName().replace('.', '/');
        Path hook = classes.resolve(original + ".class");
        Files.createDirectories(hook.getParent());
        try (InputStream input = CapeTextureOverrideHook.class.getResourceAsStream("/" + original + ".class")) {
            Files.copy(input, hook);
        }
        Class<?> renamer = Class.forName("obfuscate.RenameClasses");
        renamer.getMethod("main", String[].class).invoke(null, (Object) new String[]{
                classes.toString(), temp.resolve("map.md").toString(), temp.resolve("map.txt").toString(),
                temp.resolve("native.txt").toString()});
        Properties map = new Properties();
        try (InputStream input = Files.newInputStream(temp.resolve("map.txt"))) { map.load(input); }
        String renamed = map.getProperty(original).replace('/', '.');
        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(CapeHookObfuscationProbe.class.getClassLoader()));
        pool.insertClassPath(classes.toString());
        CtClass target = pool.makeClass("test.CapeSnippetTarget");
        CtMethod cape = CtNewMethod.make("public Object cape() { return null; }", target);
        CtMethod visible = CtNewMethod.make("public boolean visible(Object part) { return false; }", target);
        target.addMethod(cape);
        target.addMethod(visible);
        inject(cape, visible, renamed);
        target.toBytecode();
        if (args.length > 0) {
            // Javassist treats .patch as a directory; give its ZIP contents a .jar suffix.
            Path patchJar = temp.resolve("cheatbreaker.jar");
            Files.copy(Paths.get(args[0]), patchJar);
            pool.appendClassPath(patchJar.toString());
            String playerName = CheatBreaker189Mappings.remapClassName("net/minecraft/client/entity/AbstractClientPlayer");
            String entityName = CheatBreaker189Mappings.remapClassName("net/minecraft/entity/player/EntityPlayer");
            String location = CheatBreaker189Mappings.remapClassName("net/minecraft/util/ResourceLocation");
            String part = CheatBreaker189Mappings.remapClassName("net/minecraft/entity/player/EnumPlayerModelParts");
            CtClass player = pool.get(playerName.replace('/', '.'));
            CtClass entity = pool.get(entityName.replace('/', '.'));
            String capeName = runtimeName(playerName, "func_110303_q");
            String wearingName = runtimeName(entityName, "func_175148_a");
            inject(player.getMethod(capeName, "()L" + location + ";"),
                    entity.getMethod(wearingName, "(L" + part + ";)Z"), renamed);
            player.toBytecode();
            entity.toBytecode();
            System.out.println("PASS: both hooks compile against the original CheatBreaker patch after payload obfuscation");
        }
        System.out.println("PASS: stable cape callbacks survived RenameClasses and Javassist compilation");
    }

    private static void inject(CtMethod cape, CtMethod visible, String hook) throws Exception {
        cape.insertAfter("{ $_ = ($r) " + hook + "#override((java.lang.Object)$0, (java.lang.Object)$_); }");
        visible.insertAfter("{ $_ = " + hook + "#forceCapeVisible((java.lang.Object)$0, (java.lang.Object)$1, $_); }");
    }

    private static String runtimeName(String owner, String srg) throws Exception {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                CapeHookObfuscationProbe.class.getResourceAsStream("/mappings/cheatbreaker189/methods_fwd.tsv"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");
                if (columns.length >= 3 && columns[0].equals(owner) && columns[1].equals(srg)) return columns[2];
            }
        }
        throw new AssertionError("Missing cape mapping " + owner + "/" + srg);
    }
}
