#type compute
#version 460

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

//layout(set = 0, binding = 0) uniform sampler2D inputColorTexture;
layout(set = 0, binding = 1, rgba32f) writeonly uniform image2D outputTexture;
//layout(set = 0, binding = 2) uniform sampler2D inputPosTexture;


layout(push_constant) uniform PushConstants {
    int mode;
} shaderMode;

void main() {

    ivec2 uv = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);

    /*
    vec4 inputColor = texelFetch(inputColorTexture, uv, 0);
    vec4 outputColor = inputColor;

    if(shaderMode.mode == 0) {
        outputColor = inputColor;
    }
    else if(shaderMode.mode == 1){

        float grayscale = (inputColor.r + inputColor.g + inputColor.b) / 3.0;

        if (int(uv.x) % 4 != 0) {
            if (int(uv.y) % 4 == 0) {
                grayscale = (uv.x / 2) + (uv.y / 2);
            }
        }
        outputColor = vec4(outputColor.r, grayscale, grayscale, 1);
    }
    */

    vec4 outputColor = vec4(1.0, 0.0, 0.0, 1.0);
    imageStore(outputTexture, uv, outputColor);
}