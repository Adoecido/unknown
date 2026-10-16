package vegas.sony.mapping;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import vegas.sony.input.ChatCommandHandler;
import vegas.sony.reflect.CheatBreaker189Mappings;

/** Verifies the real chat method can call the command handler after payload renaming. */
public final class ChatCommandHookObfuscationProbe {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("sonyvegas-chat-command-hook-probe-");
        Path classes = temp.resolve("classes");
        String original = ChatCommandHandler.class.getName().replace('.', '/');
        Path handler = classes.resolve(original + ".class");
        Files.createDirectories(handler.getParent());
        try (InputStream input = ChatCommandHandler.class.getResourceAsStream("/" + original + ".class")) {
            Files.copy(input, handler);
        }

        Class<?> renamer = Class.forName("obfuscate.RenameClasses");
        renamer.getMethod("main", String[].class).invoke(null, (Object)new String[]{
                classes.toString(), temp.resolve("map.md").toString(), temp.resolve("map.txt").toString(),
                temp.resolve("native.txt").toString()});
        Properties map = new Properties();
        try (InputStream input = Files.newInputStream(temp.resolve("map.txt"))) {
            map.load(input);
        }
        String renamed = map.getProperty(original).replace('/', '.');
        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(ChatCommandHookObfuscationProbe.class.getClassLoader()));
        pool.insertClassPath(classes.toString());

        CtClass target = pool.makeClass("test.ChatCommandSnippetTarget");
        CtMethod send = CtNewMethod.make("public void send(String message) { }", target);
        target.addMethod(send);
        inject(send, renamed);
        target.toBytecode();

        if (args.length > 0) {
            Path patchJar = temp.resolve("cheatbreaker.jar");
            Files.copy(Paths.get(args[0]), patchJar);
            pool.appendClassPath(patchJar.toString());
            String playerName = CheatBreaker189Mappings.remapClassName(
                    "net/minecraft/client/entity/EntityPlayerSP");
            CtClass player = pool.get(playerName.replace('/', '.'));
            String sendName = runtimeName(playerName, "func_71165_d");
            inject(player.getMethod(sendName, "(Ljava/lang/String;)V"), renamed);
            player.toBytecode();
            System.out.println("PASS: chat hook compiles against the current CheatBreaker patch");
        }
        System.out.println("PASS: ChatCommandHandler.process survived payload renaming");
    }

    private static void inject(CtMethod method, String handler) throws Exception {
        method.insertBefore("{ if (" + handler
                + "#process((java.lang.String)$1)) return; }");
    }

    private static String runtimeName(String owner, String srg) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ChatCommandHookObfuscationProbe.class.getResourceAsStream(
                        "/mappings/cheatbreaker189/methods_fwd.tsv"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");
                if (columns.length >= 3 && columns[0].equals(owner)
                        && columns[1].equals(srg)) return columns[2];
            }
        }
        throw new AssertionError("Missing chat mapping " + owner + "/" + srg);
    }
}
