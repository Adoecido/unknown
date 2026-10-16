package vegas.sony.module.blatant.invwalk;

import vegas.sony.utils.render.RenderBatchShaderProgram;

public class InvWalkKeyLayout {
    public static RenderBatchShaderProgram blockEspShader;
    public static RenderBatchShaderProgram universalShader;


    public static void initializeShaders() {
        if (universalShader == null) {
            universalShader = new RenderBatchShaderProgram(
                    "shader/universal_vert.vert", "shader/universal_frag.frag");
        }
        if (blockEspShader == null) {
            blockEspShader = new RenderBatchShaderProgram(
                    "shader/block_esp_vert.vert", "shader/block_esp_frag.frag");
        }
    }

    public static void clearShaders() {
        universalShader = null;
        blockEspShader = null;
    }
}

