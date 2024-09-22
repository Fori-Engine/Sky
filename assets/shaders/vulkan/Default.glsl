#type vertex
#version 450

layout(location = 0) in vec3 pos;
layout(location = 0) out vec3 fragColor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
} camera;

layout(std140,set = 0, binding = 1) readonly buffer Transforms {

    mat4 model;
} transforms;










vec3 colors[4] = vec3[](
    vec3(0.6, 0.0, 0.0),
    vec3(0.0, 1.0, 0.0),
    vec3(1.0, 0.6, 1.0),
    vec3(1.0, 0.0, 1.0)
);

void main() {
    gl_Position = camera.proj * camera.view * transforms.model * vec4(pos.xyz, 1.0);
    fragColor = colors[gl_VertexIndex % 4];
}


#type fragment
#version 450

layout(location = 0) in vec3 fragColor;
layout(location = 0) out vec4 outColor;

void main() {
    outColor = vec4(fragColor, 1.0);
}
