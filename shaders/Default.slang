struct VSOutput {
    float2 uv;
    float4 pos;
}

struct Vertex
{
    float3 pos : POSITION;
    float1 transformIndex;
    float2 uv;
}

[shader("vertex")]
VSOutput vertexMain(Vertex vertex) {
    VSOutput output;

    output.pos = float4(vertex.pos, 1.0);

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
    float4 color1 : SV_Target1;
}

[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {
    FSOutput output;

    output.color0 = float4(1.0, 0.0, 0.0, 1.0);
    output.color1 = float4(0.0, 1.0, 0.0, 1.0);

    return output;
}