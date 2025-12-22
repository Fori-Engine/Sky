#type vertex
#version 460

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputTransformIndex;
layout(location = 2) in vec2 inputUV;

layout(location = 0) out vec2 outputUV;

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


    outputUV = inputUV;

}


#type fragment
#version 460

layout(location = 0) in vec2 inputUV;
layout(location = 0) out vec4 outputColor;

layout(set = 0, binding = 2) uniform sampler2D[] textures;

layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;



void main() {
    if(shaderMode.mode[0] == 0) {
        outputColor = texture(textures[0], inputUV);
    }
    else {
        float depth = gl_FragCoord.z;
        outputColor = vec4(depth, depth, depth, 1.0);
    }
}
