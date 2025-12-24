#type vertex
#version 460

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputTransformIndex;
layout(location = 2) in vec4 inputColor;

layout(location = 0) out vec4 outputColor;

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

layout(std140, set = 0, binding = 1) readonly buffer Transforms {
    mat4 models[];
} transforms;

layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;

void main() {
    mat4 m;
    int renderMode = shaderMode.mode[0];

    if(renderMode == 0) m = sceneDesc.scene.camera.proj * sceneDesc.scene.camera.view;
    if(renderMode == 1) {
        int lightIndex = shaderMode.mode[1];
        m = sceneDesc.scene.lights[lightIndex].proj * sceneDesc.scene.lights[lightIndex].view;
    }
    gl_Position = m * transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0);
    outputColor = inputColor;
}


#type fragment
#version 460
layout(location = 0) in vec4 inputColor;
layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputPos;

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


layout(push_constant) uniform PushConstants {
    int mode[2];
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
    int renderMode = shaderMode.mode[0];
    int lightIndex = shaderMode.mode[1];

    if(renderMode == 0) {
        outputColor = inputColor;
        outputPos = getWorldSpacePos(gl_FragCoord, sceneDesc.scene.camera.invView, sceneDesc.scene.camera.invProj, 1920, 1080);
    }
    else if(renderMode == 1) {
        outputColor = getWorldSpacePos(gl_FragCoord, sceneDesc.scene.lights[lightIndex].invView, sceneDesc.scene.lights[lightIndex].invProj, 960, 540);
    }
}
