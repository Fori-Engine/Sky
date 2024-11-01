#type vertex
#version 450

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputRenderQueuePos;
layout(location = 2) in float inputTransformIndex;
layout(location = 3) in vec2 inputUV;
layout(location = 4) in float inputMaterialBaseIndex;


layout(location = 0) out vec2 outputUV;
layout(location = 1) out float outputMaterialBaseIndex;
layout(location = 2) out float outputRenderQueuePos;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
} camera;

layout(std140, set = 0, binding = 1) readonly buffer Transforms {
    mat4 models[];
} transforms;

/*
layout(std140, set = 0, binding = 2) readonly buffer MaterialMap {
    float[] array;
} materialMap;

*/


void main() {

    gl_Position = camera.proj * camera.view * transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0);

    outputUV = inputUV;
    outputMaterialBaseIndex = inputMaterialBaseIndex;
    outputRenderQueuePos = inputRenderQueuePos;
}


#type fragment
#version 450
#extension GL_EXT_nonuniform_qualifier : require
#define MATERIAL_SIZE 4
#define MAX_MATERIALS 3

layout(location = 0) in vec2 inputUV;
layout(location = 1) in flat float inputMaterialBaseIndex;
layout(location = 2) in flat float inputRenderQueuePos;

layout(location = 0) out vec4 outputColor;

layout(set = 0, binding = 2) uniform sampler2D[] materials;




void main() {

    /*
    int baseIndex = int(inputRenderQueuePos) * MATERIAL_SIZE * MAX_MATERIALS;
    int albedoIndex = baseIndex + int(inputMaterialBaseIndex * MATERIAL_SIZE);
    int metallicIndex = baseIndex + int(inputMaterialBaseIndex * MATERIAL_SIZE) + 1;
    int normalIndex = baseIndex + int(inputMaterialBaseIndex * MATERIAL_SIZE) + 2;
    int roughnessIndex = baseIndex + int(inputMaterialBaseIndex * MATERIAL_SIZE) + 3;






    outputColor = texture(materials[metallicIndex], inputUV);

    */

    outputColor = vec4(inputUV.xy, 0.0, 1.0);
}
