#version 450

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