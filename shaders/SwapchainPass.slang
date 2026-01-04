struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color; 
    float2 uv : TEXCOORD0;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
}

ConstantBuffer<float4x4> cameraProj;



[shader("vertex")]
VSOutput vertexMain(Vertex vertex) {
    VSOutput output;
    output.pos = mul(cameraProj, float4(vertex.pos, 0, 1.0));
    output.color = vertex.color;
    output.uv = vertex.uv;

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
}

Texture2D inputTexture;
SamplerState inputTextureSamplerState;


[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {
    FSOutput output;
    output.color0 = inputTexture.Sample(inputTextureSamplerState, vsOutput.uv);
    return output;
}