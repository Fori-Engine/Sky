#version 450

layout(binding = 0) uniform LVKFrameUniforms {
    mat4 proj;
} lfu;

layout(location = 0) in vec2 v_pos;
layout(location = 1) in vec2 v_uv;
layout(location = 2) in vec4 v_color;





layout(location = 0) out vec4 f_color;
layout(location = 1) out vec2 f_uv;


void main() {
    f_color = v_color;
    f_uv = v_uv;

    gl_Position = lfu.proj * vec4(v_pos, 0.0, 1.0);
}