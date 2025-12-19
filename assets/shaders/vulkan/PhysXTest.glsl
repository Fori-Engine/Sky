#type vertex
#version 460

layout(location = 0) in vec3 inputPos;
layout(location = 1) in float inputTransformIndex;
layout(location = 2) in vec4 inputColor;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec3 outputPos;

layout(std140, set = 0, binding = 0) readonly buffer Cameras {
    mat4 view[2];
    mat4 proj[2];
} cameras;

layout(std140, set = 0, binding = 1) readonly buffer Transforms {
    mat4 models[];
} transforms;

layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;

void main() {
    int cameraIndex = shaderMode.mode[1];

    gl_Position = cameras.proj[cameraIndex] * cameras.view[cameraIndex] * transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0);
    outputPos = vec4(transforms.models[int(inputTransformIndex)] * vec4(inputPos.xyz, 1.0)).xyz;
    outputColor = inputColor;
}


#type fragment
#version 460
layout(location = 0) in vec4 inputColor;
layout(location = 1) in vec3 inputPos;
layout(location = 0) out vec4 outputColor;


layout(push_constant) uniform PushConstants {
    int mode[2];
} shaderMode;

void main() {
    if(shaderMode.mode[0] == 0) {
        outputColor = inputColor;
    }
    else {
        float depth = inputPos.z;
        outputColor = vec4(depth, depth, depth, 1.0);
    }
}
