#type vertex
#version 450

layout(location = 0) in vec3 pos;
layout(location = 1) in float transformIndex;
layout(location = 0) out vec2 outputUV;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
} camera;

layout(std140, set = 0, binding = 1) readonly buffer Transforms {

    mat4 models[];
} transforms;

vec2 uv[4] = vec2[](
vec2(1, 0),
vec2(0, 0),
vec2(0, 1),
vec2(1, 1)
);

void main() {

    gl_Position = camera.proj * camera.view * transforms.models[int(transformIndex)] * vec4(pos.xyz, 1.0);

    outputUV = uv[int(gl_VertexIndex % 4)];
}


#type fragment
#version 450

layout(location = 0) in vec2 inputUV;
layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 2) uniform sampler2D[] materials;




void main() {
    outColor = texture(materials[0], inputUV);
}
