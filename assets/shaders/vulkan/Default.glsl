#version 450

#type vertex

layout(binding = 0) uniform LVKFrameUniforms {
    mat4 m_model;
    mat4 m_view;
    mat4 m_projection;
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


    gl_Position = lfu.m_projection * lfu.m_view * lfu.m_model * vec4(v_pos, 0.0, 1.0);
}

#type fragment



layout(location = 0) in vec4 f_color;
layout(location = 1) in vec2 f_uv;
layout(location = 2) flat in float f_texindex;
layout(location = 3) flat in float f_thickness;

layout(location = 0) out vec4 FragColor;
layout(binding = 1) uniform sampler2D u_textures[32];



void main()
{
    int index = int(f_texindex);



    if(index == -1){
        FragColor = f_color;
    }
    else if(index == -2){
        vec2 uv = f_uv * 2.0 - 1.0;

        float distance = length(uv);

        if(distance <= 1.0 && distance >= (1.0 - f_thickness))
        FragColor = f_color;
        else
        discard;

    }
    else {
        FragColor = texture(u_textures[index], f_uv) * f_color;
    }

}