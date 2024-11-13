#type vertex
#version 450

layout(location = 0) in vec2 inputPos;
layout(location = 1) in float inputQuadType;
layout(location = 2) in vec4 inputColor;
layout(location = 3) in float inputThickness;
layout(location = 4) in vec2 inputUV;
layout(location = 5) in float inputTextureIndex;



layout(location = 0) out float outputQuadType;
layout(location = 1) out vec4 outputColor;
layout(location = 2) out float outputThickness;
layout(location = 3) out vec2 outputUV;
layout(location = 4) out float outputTextureIndex;


layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
} camera;


void main() {

    gl_Position = camera.proj * vec4(inputPos.xy, 0, 1.0);

    outputQuadType = inputQuadType;
    outputColor = inputColor;
    outputThickness = inputThickness;
    outputUV = inputUV;
    outputTextureIndex = inputTextureIndex;

}


#type fragment
#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout(location = 0) in float inputQuadType;
layout(location = 1) in vec4 inputColor;
layout(location = 2) in float inputThickness;
layout(location = 3) in vec2 inputUV;
layout(location = 4) in float inputTextureIndex;

layout(location = 0) out vec4 outputColor;
layout(set = 0, binding = 1) uniform sampler2D[] textures;

#define Texture 0
#define Circle 1
#define Quad 2

void main() {

    int type = int(inputQuadType);

    if(type == Quad){
        outputColor = inputColor;
    }
    else if(type == Circle){
        vec2 uv = inputUV * 2.0 - 1.0;
        float distance = length(uv);
        if(distance <= 1.0 && distance >= (1.0 - inputThickness)) outputColor = inputColor;
        else discard;
    }
    else if(type == Texture){
        outputColor = texture(textures[int(inputTextureIndex)], inputUV) * inputColor;
    }


}
