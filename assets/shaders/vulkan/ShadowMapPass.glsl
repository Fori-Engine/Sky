#type compute
#version 460

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

layout(set = 0, binding = 0) uniform sampler2D inputTexture;
layout(set = 0, binding = 1, rgba32f) writeonly uniform image2D outputTexture;
layout(set = 0, binding = 2) uniform sampler2D[] inputShadowMaps;

layout(push_constant) uniform PushConstants {
    int mode[1];
} shaderMode;

void main() {

    ivec2 uv = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);

    vec4 inputColor = texelFetch(inputTexture, uv, 0);
    vec4 outputColor = inputColor;
    imageStore(outputTexture, uv, outputColor);
}