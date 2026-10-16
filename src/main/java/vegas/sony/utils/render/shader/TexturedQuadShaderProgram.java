package vegas.sony.utils.render.shader;

import vegas.sony.utils.render.shader.ShaderProgram;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class TexturedQuadShaderProgram
extends ShaderProgram {
    private static final String FRAGMENT_SHADER_SOURCE = "#version 430 compatibility\nlayout(location = 0) uniform sampler2D image;\nlayout(location = 1) uniform vec2 screen_dimensions;\nlayout(location = 2) uniform float blur_Radius;\nlayout(location = 3) uniform int blur_Direction;\nlayout(location = 4) uniform float corner_Radius;\nlayout(location = 5) uniform vec4 mask_Rect;\nin vec2 f_Position;\nout vec4 FragmentColor;\n\n// Function to calculate Gaussian weight\nfloat gaussian(float x, float sigma) {\n    return exp(-(x * x) / (2.0 * sigma * sigma)) / (2.0 * 3.14159 * sigma * sigma);\n}\n\nvec4 get_gaussian_blurred() {\n    vec4 blurred = vec4(0.0);\n    vec2 coords = gl_TexCoord[0].xy;\n    float sigma = blur_Radius / 3.0; // Adjust sigma value as needed\n\n    float totalWeight = 0.0;\n    for (float x = -blur_Radius; x <= blur_Radius; x++) {\n        // Calculate weight\n        float weight = gaussian(x, sigma);\n\n        // Sample texture\n        vec2 offset = vec2(x, 0.0) / screen_dimensions;\n        vec4 sample1 = texture(image, coords + offset) * weight;\n\n        // Accumulate weighted sample\n        blurred += sample1;\n        totalWeight += weight;\n    }\n    blurred /= totalWeight;\n    totalWeight = 0.0;\n    vec4 blurred2 = vec4(0.0);\n    for (float y = -blur_Radius; y <= blur_Radius; y++) {\n        // Calculate weight\n        float weight = gaussian(y, sigma);\n\n        // Sample texture\n        vec2 offset = vec2(0.0, y) / screen_dimensions;\n        vec4 sample1 = texture(image, coords + offset) * weight;\n\n        // Accumulate weighted sample\n        blurred2 += sample1;\n        totalWeight += weight;\n    }\n\n    return blurred2 / totalWeight;\n}\n\nvoid main(void) {\n    vec4 blurred = vec4(0.0);\n    vec2 coords = gl_TexCoord[0].xy;\n    if (corner_Radius > 0.0) {\n        vec2 mask_o = mask_Rect.z > 0.0 ? mask_Rect.xy : vec2(0.0);\n        vec2 mask_s = mask_Rect.z > 0.0 ? mask_Rect.zw : screen_dimensions;\n        vec2 mask_p = coords * screen_dimensions - mask_o;\n        vec2 mask_b = vec2((mask_s.x + corner_Radius) * 0.5, mask_s.y * 0.5);\n        vec2 mask_q = abs(mask_p - mask_b) - mask_b + corner_Radius;\n        if (length(max(mask_q, 0.0)) + min(max(mask_q.x, mask_q.y), 0.0) - corner_Radius > 0.0) discard;\n    }\n    float sigma = blur_Radius / 3.0;\n    float totalWeight = 0.0;\n    for (float i = -blur_Radius; i <= blur_Radius; i++) {\n        float weight = gaussian(i, sigma);\n        vec2 offset = blur_Direction == 0 ? vec2(i, 0) : vec2(0, i);\n        offset /= screen_dimensions;\n        vec4 sample1 = texture(image, coords + offset) * weight;\n        blurred += sample1;\n        totalWeight += weight;\n    }\n    gl_FragColor = blurred / totalWeight;\n}";
    private static final String VERTEX_SHADER_SOURCE = "#version 430 compatibility\n\nout vec2 f_Position;\nvoid main(void)\n{\nf_Position = gl_Vertex.xy;\ngl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\ngl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n}";
    private static final long GL_TEXTURE0_VALUE = 1789813357298091200L;

    public void configureTexture(float screenWidth, float screenHeight) {
        GL13.glActiveTexture((int)((int)GL_TEXTURE0_VALUE));
        GL20.glUniform1i((int)0, (int)0);
        GL20.glUniform2f((int)1, (float)screenWidth, (float)screenHeight);
    }

    public void setBlurRadius(float blurRadius) {
        GL20.glUniform1f((int)2, (float)blurRadius);
    }

    public void setBlurDirection(int blurDirection) {
        GL20.glUniform1i((int)3, (int)blurDirection);
    }

    /** Rounds the two corners on the leading edge. Zero keeps the quad square. */
    public void setCornerRadius(float cornerRadius) {
        GL20.glUniform1f((int)4, (float)cornerRadius);
    }

    /**
     * Restricts the rounded mask to one rectangle of the capture, in texels with
     * y measured from the bottom. A zero width means the whole quad.
     */
    public void setMaskRect(float x, float y, float width, float height) {
        GL20.glUniform4f((int)5, (float)x, (float)y, (float)width, (float)height);
    }

    public TexturedQuadShaderProgram() {
        super(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
    }
}
