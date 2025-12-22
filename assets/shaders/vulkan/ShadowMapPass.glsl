#type compute
#version 460

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

#define MAX_LIGHTS 10

struct Camera {
    mat4 view;
    mat4 proj;
};

struct Light {
    mat4 view;
    mat4 proj;
};

struct Scene {
    Camera camera;
    Light lights[MAX_LIGHTS];
};

layout(std140, set = 0, binding = 0) readonly buffer SceneDesc {
    Scene scene;
} sceneDesc;

layout(set = 0, binding = 1) uniform sampler2D inputTexture;
layout(set = 0, binding = 2, rgba32f) writeonly uniform image2D outputTexture;
layout(set = 0, binding = 3) uniform sampler2D[] inputShadowMaps;

layout(push_constant) uniform PushConstants {
    int mode[1];
} shaderMode;

/*
We need access to the SceneDesc [-]
We need the inverse camera view and inverse camera projection
We need an attachment from Scene that contains only gl_FragCoord
*/

void main() {

    ivec2 uv = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);

    vec4 inputColor = texelFetch(inputTexture, uv, 0);
    vec4 outputColor = inputColor;
    imageStore(outputTexture, uv, outputColor);
}