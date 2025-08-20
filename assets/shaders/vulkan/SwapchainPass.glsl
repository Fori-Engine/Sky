#type vertex
#version 460

layout(location = 0) in vec2 inputPos;
layout(location = 1) in vec4 inputColor;
layout(location = 2) in vec2 inputUV;


layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec2 outputUV;



layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
} camera;

layout(push_constant) uniform PushConstants {
    int mode;
} shaderMode;

void main() {

    gl_Position = camera.proj * vec4(inputPos.xy, 0, 1.0);
    outputColor = inputColor;
    outputUV = inputUV;
}


#type fragment
#version 450
#extension GL_EXT_nonuniform_qualifier : require



layout(location = 0) in vec4 inputColor;
layout(location = 1) in vec2 inputUV;

layout(location = 0) out vec4 outputColor;

layout(set = 0, binding = 1) uniform sampler2D inputTexture;

layout(push_constant) uniform PushConstants {
    int mode;
} shaderMode;

void main() {

    vec4 color = texture(inputTexture, inputUV);
    outputColor = color;
}
