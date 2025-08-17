#type compute
#version 460

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

layout(set = 0, binding = 0) uniform sampler2D inputTexture;
layout(set = 0, binding = 1, rgba32f) writeonly uniform image2D outputTexture;

void main() {
    ivec2 uv = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);

    vec4 inputColor = texelFetch(inputTexture, uv, 0);

    float grayscale = (inputColor.r + inputColor.g + inputColor.b) / 3.0;

    if(int(uv.x) % 2 != 0) {
        if(int(uv.y) % 2 == 0) {
            grayscale = 0.4;
        }
    }

    imageStore(outputTexture, uv, vec4(grayscale, grayscale, grayscale, 1));



}