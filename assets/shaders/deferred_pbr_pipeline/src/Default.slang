#include "SceneDesc.slang"

struct Transforms {
    float4x4 transforms[MAX_TRANSFORMS];
}

StructuredBuffer<Scene> sceneDesc;
StructuredBuffer<Transforms> transforms;

struct VSOutput {
    float4 pos : SV_POSITION;
    float4 posViewSpace;
    float4 color;
    float3 normal;
}

struct Vertex
{
    float3 pos;
    float1 entityIndex;
    float4 color;
    float3 normal;
}



[shader("vertex")]
VSOutput vertexMain(Vertex vertex, uniform int renderMode, uniform int lightIndex) {
    VSOutput output;
    float4x4 view, proj;


    
    if(renderMode == 0) {
        view = sceneDesc[0].camera.view;
        proj = sceneDesc[0].camera.proj;
    }
    if(renderMode == 1) {
        view = sceneDesc[0].lights[lightIndex].view;
        proj = sceneDesc[0].lights[lightIndex].proj;
    }
    
    float4x4 transform = transforms[0].transforms[int(vertex.entityIndex)];

    output.posViewSpace = mul(view, mul(transform, float4(vertex.pos, 1.0)));
    output.pos = mul(proj, output.posViewSpace);
    output.color = vertex.color;
    output.normal = mul(float3x3(transform), vertex.normal);

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
    float4 color1 : SV_Target1;
    float4 color2 : SV_Target2;
}

[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, uniform int renderMode, uniform int lightIndex) {
    FSOutput output;
    

    if(renderMode == 0) {
        output.color0 = float4(vsOutput.color.xyz, 1.0);
        output.color1 = float4(vsOutput.posViewSpace.xyz, 1.0);
        output.color2 = float4(vsOutput.normal.xyz, 1.0);
    }
    if(renderMode == 1) {
        output.color0 = float4(vsOutput.posViewSpace.xyz, 1.0);
    }



    return output;
}