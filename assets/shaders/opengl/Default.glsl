#version 450

#type vertex

layout (location = 0) in vec2 v_pos;
layout (location = 1) in vec2 v_uv;
layout (location = 2) in float v_texindex;
layout (location = 3) in vec4 v_color;
layout (location = 4) in float v_thickness;


out vec2 f_uv;
out float f_texindex;
out vec4 f_color;
out float f_thickness;


uniform mat4 v_model;
uniform mat4 v_view;
uniform mat4 v_projection;






void main() {

    f_texindex = v_texindex;
    f_uv = v_uv;
    f_color = v_color;
    f_thickness = v_thickness;



    gl_Position = v_projection * v_view * v_model * vec4(v_pos, 0.0, 1.0);
}

#type fragment

out vec4 FragColor;

in vec2 f_uv;
in float f_texindex;
in vec4 f_color;
in float f_thickness;

layout(binding = 1) uniform sampler2D u_textures[32];

layout(binding = 2) uniform Color {
    vec4 color;
} test;




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
        vec4 color;
        //Don't look here...
        {
            if(index == 0) color = texture(u_textures[0], f_uv) * f_color;
            if(index == 1) color = texture(u_textures[1], f_uv) * f_color;
            if(index == 2) color = texture(u_textures[2], f_uv) * f_color;
            if(index == 3) color = texture(u_textures[3], f_uv) * f_color;
            if(index == 4) color = texture(u_textures[4], f_uv) * f_color;
            if(index == 5) color = texture(u_textures[5], f_uv) * f_color;
            if(index == 6) color = texture(u_textures[6], f_uv) * f_color;
            if(index == 7) color = texture(u_textures[7], f_uv) * f_color;
            if(index == 8) color = texture(u_textures[8], f_uv) * f_color;
            if(index == 9) color = texture(u_textures[9], f_uv) * f_color;
            if(index == 10) color = texture(u_textures[10], f_uv) * f_color;
            if(index == 11) color = texture(u_textures[11], f_uv) * f_color;
            if(index == 12) color = texture(u_textures[12], f_uv) * f_color;
            if(index == 13) color = texture(u_textures[13], f_uv) * f_color;
            if(index == 14) color = texture(u_textures[14], f_uv) * f_color;
            if(index == 15) color = texture(u_textures[15], f_uv) * f_color;
            if(index == 16) color = texture(u_textures[16], f_uv) * f_color;
            if(index == 17) color = texture(u_textures[17], f_uv) * f_color;
            if(index == 18) color = texture(u_textures[18], f_uv) * f_color;
            if(index == 19) color = texture(u_textures[19], f_uv) * f_color;
            if(index == 20) color = texture(u_textures[20], f_uv) * f_color;
            if(index == 21) color = texture(u_textures[21], f_uv) * f_color;
            if(index == 22) color = texture(u_textures[22], f_uv) * f_color;
            if(index == 23) color = texture(u_textures[23], f_uv) * f_color;
            if(index == 24) color = texture(u_textures[24], f_uv) * f_color;
            if(index == 25) color = texture(u_textures[25], f_uv) * f_color;
            if(index == 26) color = texture(u_textures[26], f_uv) * f_color;
            if(index == 27) color = texture(u_textures[27], f_uv) * f_color;
            if(index == 28) color = texture(u_textures[28], f_uv) * f_color;
            if(index == 29) color = texture(u_textures[29], f_uv) * f_color;
            if(index == 30) color = texture(u_textures[30], f_uv) * f_color;
            if(index == 31) color = texture(u_textures[31], f_uv) * f_color;
        }
        FragColor = color;


    }

    FragColor *= test.color;

}
