#version 450

layout(location = 0) in vec4 f_color;
layout(location = 1) in vec2 f_uv;

layout(location = 0) out vec4 FragColor;

layout(binding = 1) uniform sampler2D texSampler;


void main() {
    FragColor = f_color * texture(texSampler, f_uv);
}