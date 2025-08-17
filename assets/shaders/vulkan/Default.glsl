#type vertex
#version 460

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputTransformIndex;
layout(location = 2) in vec2 inputUV;

layout(location = 0) out vec2 outputUV;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
} camera;

layout(std140, set = 0, binding = 1) readonly buffer Transforms {
    mat4 models[];
} transforms;

void main() {

    gl_Position = camera.proj * camera.view * transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0);

    outputUV = inputUV;
}


#type fragment
#version 460
#extension GL_EXT_nonuniform_qualifier : require
#define MATERIAL_SIZE 4

layout(location = 0) in vec2 inputUV;
layout(location = 0) out vec4 outputColor;

layout(set = 0, binding = 2) uniform sampler2D[] textures;




void main() {
    outputColor = texture(textures[0], inputUV);
}
