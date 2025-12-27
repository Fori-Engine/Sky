#type vertex
#version 460

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputTransformIndex;
layout(location = 2) in vec2 inputUV;

layout(location = 0) out vec2 outputUV;
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

layout(std140, set = 0, binding = 1) readonly buffer Transforms {
    mat4 models[];
} transforms;

layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;

void main() {

    mat4 view, proj;
    int renderMode = shaderMode.mode[0];

    if(renderMode == 0) {
        view = sceneDesc.scene.camera.view;
        proj = sceneDesc.scene.camera.proj;
    }
    if(renderMode == 1) {
        int lightIndex = shaderMode.mode[1];
        view = sceneDesc.scene.lights[lightIndex].view;
        proj = sceneDesc.scene.lights[lightIndex].proj;
    }

    outputPos = view * transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0);
    gl_Position = proj * outputPos;
    outputUV = inputUV;

}


#type fragment
#version 460

layout(location = 0) in vec2 inputUV;
layout(location = 1) in vec4 inputPos;
layout(location = 0) out vec4 attachment0;
layout(location = 1) out vec4 attachment1;


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


layout(set = 0, binding = 2) uniform sampler2D[] textures;

layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;




void main() {
    int renderMode = shaderMode.mode[0];

    if(renderMode == 0) {
        attachment0 = texture(textures[0], inputUV);
        attachment1 = vec4(inputPos.xyz, 1.0);
    }
    else if(renderMode == 1) {
        attachment0 = vec4(0.0);
        attachment1 = vec4(inputPos.xyz, 1.0);
    }
}
