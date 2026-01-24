struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color; 
    float2 uv;
    float mode;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
    float mode;
}

struct Camera
{
    float4x4 proj;
}

ConstantBuffer<Camera> camera;



[shader("vertex")]
VSOutput vertexMain(Vertex vertex) {
    VSOutput output;
    output.pos = mul(camera.proj, float4(vertex.pos, 0, 1.0));
    output.color = vertex.color;
    output.uv = vertex.uv;
    output.mode = vertex.mode;

    return output;
}

struct FSOutput {
    float4 color0SW : SV_Target0;
}

Texture2D inputColorTexture;
SamplerState inputColorTextureSampler;


[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {
    FSOutput output;
    if(vsOutput.mode >= 0) {
        output.color0SW = inputColorTexture.Sample(inputColorTextureSampler, vsOutput.uv);
    }
    else if(vsOutput.mode == -1) output.color0SW = vsOutput.color;
    else if(vsOutput.mode == -2) {
        float2 uv = vsOutput.uv * 2.0 - 1.0;

        float distance = length(uv);

        if(distance <= 1.0 && distance >= (1.0 - 0.5))
            output.color0SW = vsOutput.color;
        else
            discard;
    }



    return output;
}