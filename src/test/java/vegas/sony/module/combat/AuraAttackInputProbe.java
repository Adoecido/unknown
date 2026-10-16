package vegas.sony.module.combat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sun.misc.Unsafe;
import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventSendClickBlockToController;
import vegas.sony.event.impl.SyntheticAttackRequestEvent;
import vegas.sony.input.MouseInput;
import vegas.sony.input.MouseInputState;
import vegas.sony.mapping.Mapper;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MGameSettings;
import vegas.sony.mapping.mappings.MKeyBinding;
import vegas.sony.mapping.mappings.MMinecraft;
import vegas.sony.mapping.mappings.MPlayerControllerMP;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

/** Exercises Aura's actual input pause through the mapped vanilla mining path. */
public final class AuraAttackInputProbe {
    private static final Unsafe UNSAFE = unsafe();

    public static void main(String[] args) throws Exception {
        SonyVegas previousClient = SonyVegas.INSTANCE;
        int previousVersion = ForgeVersion.gameVersion;
        SonyVegas client = allocate(SonyVegas.class);
        SonyVegas.INSTANCE = client;
        ForgeVersion.gameVersion = 15;
        SonyVegas previousWrapperClient = Wrapper.sonyVegasInstance;
        Object previousMinecraft = get(Minecraft.class, "L", null);
        Object previousMouse = get(MouseInput.class, "cachedState", null);
        try {
            Wrapper.sonyVegasInstance = client;
            Mapper mapper = allocate(Mapper.class);
            set(SonyVegas.class, "mapper", client, mapper);
            Game game = new Game();
            set(Minecraft.class, "L", null, game);
            MinecraftMapping minecraft = allocate(MinecraftMapping.class);
            set(MMinecraft.class, "Oy", minecraft, field(Game.class, "leftClickCounter"));
            set(MMinecraft.class, "Of", minecraft, method(Game.class, "continueAttack", boolean.class));
            set(Mapper.class, "U", mapper, minecraft);
            MPlayerControllerMP controller = allocate(MPlayerControllerMP.class);
            set(MPlayerControllerMP.class, "l", controller, field(Game.class, "digging"));
            set(Mapper.class, "hj", mapper, controller);
            MGameSettings settings = allocate(MGameSettings.class);
            set(MGameSettings.class, "u", settings, field(Settings.class, "attackKey"));
            set(Mapper.class, "RM", mapper, settings);
            KeyMapping keys = allocate(KeyMapping.class);
            keys.key = game.settings.attackKey;
            set(Mapper.class, "hJ", mapper, keys);
            PhysicalMouse mouse = new PhysicalMouse();
            set(MouseInput.class, "cachedState", null, mouse);

            // Mining was started by a previous physical click. Aura now takes
            // over: pending clicks and mining must end before the entity attack.
            game.aura = allocate(SilentAura.class);
            mouse.down = true;
            require(Boolean.TRUE.equals(invoke(game.aura, "suppressClicksForAttackPacket")),
                    "Input pause rejected a successfully stopped mining action");
            game.attack();
            require(game.trace.equals(Arrays.asList("stop-mining", "attack")),
                    "An entity attack was sent before the existing mining action ended: " + game.trace);
            require(!game.settings.attackKey.pressed && game.settings.attackKey.pressTime == 0,
                    "Physical/synthetic queued clicks survived the pause");
            require(!game.aura.canClickAttack(), "Aura clicker resumed during the pause");
            require(!game.aura.canSwingLegitVisual(), "Fail Swing resumed during the pause");
            EventClickMouse manual = new EventClickMouse();
            game.aura.onClickMouse(manual);
            require(manual.isCanceled(), "A manual click was accepted during the pause");
            SyntheticAttackRequestEvent synthetic = new SyntheticAttackRequestEvent(game.aura);
            game.aura.onSyntheticAttackRequest(synthetic);
            require(synthetic.isCanceled(), "A queued synthetic click was accepted during the pause");
            require(Boolean.FALSE.equals(invoke(game.aura, "suppressClicksForAttackPacket")),
                    "A second attack took over during the same input tick");
            game.continueAttack(true);
            require(!game.digging, "Mining restarted during the attack tick");

            invoke(game.aura, "restoreClicksAfterAttackPacket");
            require(game.settings.attackKey.pressed, "A still-held button was not restored");
            require(game.settings.attackKey.pressTime == 0,
                    "Restoring a held button generated another click");
            EventClickMouse resumed = new EventClickMouse();
            game.aura.onClickMouse(resumed);
            require(!resumed.isCanceled(), "Manual clicks stayed blocked after the pause");
            game.continueAttack(true);
            require(game.digging, "Manual mining did not resume after the pause");

            game.settings.attackKey.pressTime = 2;
            invoke(game.aura, "suppressClicksForAttackPacket");
            mouse.down = false;
            invoke(game.aura, "restoreClicksAfterAttackPacket");
            require(!game.settings.attackKey.pressed,
                    "A button released during the pause was pressed again");
            require(game.settings.attackKey.pressTime == 0, "Stale clicks returned after release");

            game.digging = true;
            game.stopWorks = false;
            require(Boolean.FALSE.equals(invoke(game.aura, "suppressClicksForAttackPacket")),
                    "An attack was permitted even though mining interruption failed");
            invoke(game.aura, "restoreClicksAfterAttackPacket");

            System.out.println("PASS: mining ends before attack, queued clicks are cleared, and current physical input resumes without an extra click");
        } finally {
            set(MouseInput.class, "cachedState", null, previousMouse);
            set(Minecraft.class, "L", null, previousMinecraft);
            Wrapper.sonyVegasInstance = previousWrapperClient;
            ForgeVersion.gameVersion = previousVersion;
            SonyVegas.INSTANCE = previousClient;
        }
    }

    public static final class Game {
        public final Settings settings = new Settings();
        public final List<String> trace = new ArrayList<String>();
        public int leftClickCounter;
        public boolean digging = true;
        boolean stopWorks = true;
        SilentAura aura;

        public void continueAttack(boolean held) {
            EventSendClickBlockToController event = new EventSendClickBlockToController();
            aura.onSendClickBlockToController(event);
            if (event.isCanceled()) return;
            if (held) {
                digging = true;
            } else if (digging && stopWorks) {
                trace.add("stop-mining");
                digging = false;
            }
        }

        public void attack() {
            require(!digging, "attack digging=true: the input pause did not end existing mining");
            trace.add("attack");
        }
    }

    public static final class Settings {
        public final Key attackKey = new Key();
    }

    public static final class Key {
        boolean pressed = true;
        int pressTime = 2;
    }

    public static final class MinecraftMapping extends MMinecraft {
        @Override public Object b(Object game) { return ((Game)game).settings; }
        @Override public Object r(Object game) { return game; }
    }

    public static final class KeyMapping extends MKeyBinding {
        Key key;
        @Override public int B(Object object) { return -100; }
        @Override public boolean S$src$Z$wvm5qt(Object object) { return key.pressed; }
        @Override public void g(int code, boolean pressed) { key.pressed = pressed; }
        @Override public void q(Object object, int count) { key.pressTime = count; }
        @Override public void s(int code) { key.pressTime++; }
    }

    private static final class PhysicalMouse extends MouseInputState {
        boolean down;
        @Override public boolean isButtonDown(int button) { return button == 0 && down; }
    }

    private static MappingField field(Class<?> owner, String name) throws Exception {
        MappingField mapping = allocate(MappingField.class);
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        set(MappingField.class, "reflectedField", mapping, field);
        return mapping;
    }

    private static MappingMethod method(Class<?> owner, String name, Class<?>... arguments) throws Exception {
        MappingMethod mapping = allocate(MappingMethod.class);
        Method method = owner.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        set(MappingMethod.class, "reflectedMethod", mapping, method);
        return mapping;
    }

    private static Object invoke(SilentAura aura, String name) throws Exception {
        Method method = SilentAura.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(aura);
    }

    private static Object get(Class<?> owner, String name, Object instance) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void set(Class<?> owner, String name, Object instance, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static <T> T allocate(Class<T> type) throws InstantiationException {
        return type.cast(UNSAFE.allocateInstance(type));
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe)field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
