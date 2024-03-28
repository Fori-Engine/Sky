#version 450

layout(location = 0) in vec4 f_color;
layout(location = 1) in vec2 f_uv;

layout(location = 0) out vec4 FragColor;

void main() {
    FragColor = f_color;
}