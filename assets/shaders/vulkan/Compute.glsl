#type compute
#version 450

layout(set = 0, binding = 0) uniform TestUniform {
    mat4 m;
} test;

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

void main() {


}