#type compute
#version 460

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

#define MAX_LIGHTS 10

struct Camera {
    mat4 view;
    mat4 proj;

    mat4 invView;
    mat4 invProj;
};

struct Light {
    mat4 view;
    mat4 proj;

    mat4 invView;
    mat4 invProj;
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
layout(set = 0, binding = 3) uniform sampler2D[1] inputShadowMaps;
layout(set = 0, binding = 4) uniform sampler2D inputPosTexture;


layout(push_constant) uniform PushConstants {
    int mode[1];
} shaderMode;




void main() {
    ivec2 inputTextureUV = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);
    vec4 color = texelFetch(inputTexture, inputTextureUV, 0);
    vec4 wpFromCamera = sceneDesc.scene.camera.invView * texelFetch(inputPosTexture, inputTextureUV, 0);

    Light light = sceneDesc.scene.lights[0];
    vec4 shadowMapUV = light.proj * light.view * wpFromCamera;
    shadowMapUV /= shadowMapUV.w;
    shadowMapUV = shadowMapUV * 0.5 + 0.5;

    vec4 lpFromShadowMap = texture(inputShadowMaps[0], shadowMapUV.xy);
    vec4 wpFromShadowMap = light.invView * lpFromShadowMap;


    if(wpFromShadowMap.z < wpFromCamera.z - 0.005) {
        color.xyz *= 0.1;
    }


    imageStore(outputTexture, inputTextureUV, color);
}