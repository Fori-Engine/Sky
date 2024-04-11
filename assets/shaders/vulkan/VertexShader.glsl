#version 450

layout(binding = 0) uniform LVKFrameUniforms {
    mat4 proj;
} lfu;

layout(location = 0) in vec2 v_pos;
layout(location = 1) in vec2 v_uv;
layout(location = 2) in float v_texindex;
layout(location = 3) in vec4 v_color;
layout(location = 4) in float v_thickness;


layout(location = 0) out vec4 f_color;
layout(location = 1) out vec2 f_uv;
layout(location = 2) out float f_texindex;
layout(location = 3) out float f_thickness;



void main() {
    f_color = v_color;
    f_uv = v_uv;
    f_texindex = v_texindex;
    f_thickness = v_thickness;


    gl_Position = lfu.proj * vec4(v_pos, 0.0, 1.0);
}