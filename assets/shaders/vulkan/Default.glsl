#type vertex
#version 450

layout(location = 0) in vec3 pos;
layout(location = 0) out vec3 fragColor;



void main() {
    gl_Position = vec4(pos.xyz, 1.0);
    fragColor = vec3(0.0, 0.7, 0.5);
}


#type fragment
#version 450

layout(location = 0) in vec3 fragColor;
layout(location = 0) out vec4 outColor;

void main() {
    outColor = vec4(fragColor, 1.0);
}
