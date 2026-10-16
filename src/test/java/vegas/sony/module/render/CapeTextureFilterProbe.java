package vegas.sony.module.render;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class CapeTextureFilterProbe {
    private static final class FakeGl implements CapeTextureFilter.Gl {
        int bound = 73;
        int generated;
        boolean supports = true;
        boolean fail;
        final Map<Integer, Integer> parameters = new HashMap<Integer, Integer>();
        public int boundTexture() { return bound; }
        public void bind(int texture) { bound = texture; }
        public boolean supportsMipmaps() { return supports; }
        public void parameter(int name, int value) {
            require(bound == 123, "Changed another texture's settings");
            parameters.put(name, value);
        }
        public void generateMipmaps() {
            if (fail) throw new IllegalStateException("Simulated driver failure");
            ++generated;
        }
    }

    public static void main(String[] args) {
        FakeGl gl = new FakeGl();
        CapeTextureFilter.apply(123, 1024, 512, gl);
        require(gl.bound == 73 && gl.generated == 1, "Binding or mip generation incorrect");
        require(gl.parameters.get(GL11.GL_TEXTURE_MAG_FILTER) == GL11.GL_LINEAR, "Magnification remains pixelated");
        require(gl.parameters.get(GL11.GL_TEXTURE_MIN_FILTER) == GL11.GL_LINEAR_MIPMAP_LINEAR, "No trilinear minification");
        require(gl.parameters.get(GL12.GL_TEXTURE_MAX_LEVEL) == 4, "Mipmaps cross atlas face boundaries");
        FakeGl legacy = new FakeGl();
        legacy.supports = false;
        CapeTextureFilter.apply(123, 1024, 512, legacy);
        require(legacy.generated == 0 && legacy.parameters.get(GL11.GL_TEXTURE_MIN_FILTER) == GL11.GL_LINEAR,
                "Legacy GL needs a complete bilinear texture");
        require(legacy.bound == 73 && legacy.parameters.get(GL12.GL_TEXTURE_MAX_LEVEL) == 0, "Legacy fallback incomplete");
        FakeGl failing = new FakeGl();
        failing.fail = true;
        try {
            CapeTextureFilter.apply(123, 1024, 512, failing);
            throw new AssertionError("Expected simulated failure");
        } catch (IllegalStateException expected) { }
        require(failing.bound == 73, "Driver failure leaked GL binding");
        require(CapeTextureFilter.maximumLevel(64, 32) == 0, "Vanilla atlas needs no downsampling");
        System.out.println("PASS: bilinear/trilinear filtering, bounded mipmaps, legacy fallback, GL state restoration");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
