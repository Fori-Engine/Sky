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

vec4 getWorldSpacePos(vec4 windowPos, mat4 invView, mat4 invProj, int width, int height) {
    //Window to clip space
    vec4 worldSpacePos = vec4(vec3(windowPos.xy / vec2(width, height), windowPos.z) * 2.0 - 1.0, windowPos.w);
    //Perspective divide
    worldSpacePos.xyz /= worldSpacePos.w;
    //Clip to view space
    worldSpacePos *= invProj;
    //View to world space
    worldSpacePos *= invView;

    return vec4(worldSpacePos.xyz, 1.0);
}

void main() {
    ivec2 inputTextureUV = ivec2(gl_WorkGroupID.x, gl_WorkGroupID.y);
    vec4 inputColor = texelFetch(inputShadowMaps[0], inputTextureUV, 0);
    vec4 windowPosFromCamera = texelFetch(inputPosTexture, inputTextureUV, 0);
    vec4 worldSpacePosFromCamera = getWorldSpacePos(windowPosFromCamera, sceneDesc.scene.camera.invView, sceneDesc.scene.camera.invProj, 1920, 1080);




    {
        Light light = sceneDesc.scene.lights[0];
        vec4 shadowMapUV = light.proj * light.view * worldSpacePosFromCamera;
        shadowMapUV.xyz /= shadowMapUV.w;


        shadowMapUV = shadowMapUV * 0.5 + 0.5;

        vec4 shadowMapPos = texture(inputShadowMaps[0], shadowMapUV.xy);

        if(shadowMapPos.z > worldSpacePosFromCamera.z) {
            for(int y = 0; y < 5; y++) {
                for(int x = 0; x < 5; x++) {
                    imageStore(outputTexture, ivec2(x, y), vec4(1.0, 0.0, 0.0, 1.0));
                }
            }


        }





    }


    vec4 outputColor = inputColor;
    imageStore(outputTexture, inputTextureUV, outputColor);
}