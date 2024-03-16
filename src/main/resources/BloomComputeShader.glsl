#version 430 core

layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

layout(rgba32f, binding = 0) uniform image2D outputFB;
layout(rgba32f, binding = 1) uniform image2D inputFB;


ivec2 toOutputFBSpace(ivec2 coords, ivec2 inputFBsize, ivec2 outputFBsize){
    ivec2 scaledCoords = ivec2(float(coords.x) * float(outputFBsize.x) / float(inputFBsize.x),
    float(coords.y) * float(outputFBsize.y) / float(inputFBsize.y));

    return scaledCoords;
}


void main() {

    ivec2 inputFBsize = imageSize(inputFB);
    ivec2 outputFBsize = imageSize(outputFB);


    ivec2 inputFBcoords = ivec2(gl_GlobalInvocationID.xy);


    ivec2 outputFBCoords = toOutputFBSpace(inputFBcoords, inputFBsize, outputFBsize);
    imageStore(outputFB, outputFBCoords, vec4(1.0, 0.0, 0.0, 1.0));


    vec4 sampledColor = imageLoad(inputFB, inputFBcoords);





    float brightness = (sampledColor.r * 0.2126) + (sampledColor.g * 0.7152) + (sampledColor.b * 0.0722);

    if(brightness > 0.7){
        sampledColor *= brightness;
        imageStore(outputFB, outputFBCoords, sampledColor);
    }
    else {
        imageStore(outputFB, outputFBCoords, vec4(1.0, 0.0, 0.0, 1.0));
    }
}
