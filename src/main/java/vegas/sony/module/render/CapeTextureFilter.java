package vegas.sony.module.render;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import vegas.sony.wrapper.impl.TextureObject;

/** Filtering belongs to the cape's texture, not to the game's global graphics settings. */
final class CapeTextureFilter {
    interface Gl {
        int boundTexture();
        void bind(int texture);
        void parameter(int name, int value);
        boolean supportsMipmaps();
        void generateMipmaps();
    }

    static void apply(Object texture, int width, int height) {
        apply(new TextureObject(texture).getId(), width, height, new LwjglGl());
    }

    static int maximumLevel(int width, int height) {
        // Cape atlases contain a large transparent area around the two faces.
        // Mipmaps average that transparent black padding into the artwork and
        // make the cape appear as a dark/black strip in Badlion's layer.
        return 0;
    }

    static void apply(int texture, int width, int height, Gl gl) {
        if (texture <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid cape texture");
        }
        int previous = gl.boundTexture();
        try {
            gl.bind(texture);
            gl.parameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            gl.parameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            gl.parameter(GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            gl.parameter(GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            int maximum = maximumLevel(width, height);
            gl.parameter(GL12.GL_TEXTURE_BASE_LEVEL, 0);
            gl.parameter(GL12.GL_TEXTURE_MAX_LEVEL, 0);
            if (maximum > 0 && gl.supportsMipmaps()) {
                gl.parameter(GL12.GL_TEXTURE_MAX_LEVEL, maximum);
                gl.generateMipmaps();
                gl.parameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            }
        } finally {
            // Raw GL binds are paired; do not desynchronize the host's binding cache.
            gl.bind(previous);
        }
    }

    private static final class LwjglGl implements Gl {
        private final ContextCapabilities capabilities = GLContext.getCapabilities();
        public int boundTexture() { return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); }
        public void bind(int texture) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); }
        public void parameter(int name, int value) { GL11.glTexParameteri(GL11.GL_TEXTURE_2D, name, value); }
        public boolean supportsMipmaps() {
            return capabilities.OpenGL30 || capabilities.GL_ARB_framebuffer_object
                    || capabilities.GL_EXT_framebuffer_object;
        }
        public void generateMipmaps() {
            if (capabilities.OpenGL30) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            else if (capabilities.GL_ARB_framebuffer_object) ARBFramebufferObject.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            else EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
        }
    }
}
