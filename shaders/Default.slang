#define MAX_LIGHTS 10
#define MAX_TRANSFORMS 10

struct Camera {
    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;
}

struct Light {
    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;
}

struct Scene {
    Camera camera;
    Light lights[MAX_LIGHTS];
}

struct Transforms {
    float4x4 transforms[MAX_TRANSFORMS];
}

StructuredBuffer<Scene> sceneDesc;
StructuredBuffer<Transforms> transforms;

struct VSOutput {
    float2 uv : TEXCOORD0;
    float4 pos : SV_POSITION;
    float4 posViewSpace;
}

struct Vertex
{
    float3 pos : POSITION;
    float1 entityIndex;
    float2 uv : TEXCOORD;
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
    output.uv = vertex.uv;

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
    float4 color1 : SV_Target1;
}

Texture2D texture;
SamplerState textureSampler;



[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, uniform int renderMode, uniform int lightIndex) {
    FSOutput output;
    

    if(renderMode == 0) {
        output.color0 = texture.Sample(textureSampler, vsOutput.uv);
        output.color1 = float4(vsOutput.posViewSpace.xyz, 1.0);
    }
    if(renderMode == 1) {
        output.color0 = float4(0.0, 0.0, 0.0, 0.0);
        output.color1 = float4(vsOutput.posViewSpace.xyz, 1.0);
    }



    return output;
}