struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color; 
    float2 uv;
    float fillMode;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
    float fillMode;
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
    output.fillMode = vertex.fillMode;

    return output;
}

struct FSOutput {
    float4 color0SW : SV_Target0;
}

Texture2D input_color_texture;
SamplerState input_color_texture_sampler;



[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {


    FSOutput output;
    if(vsOutput.fillMode >= 0) {
        output.color0SW = input_color_texture.Sample(input_color_texture_sampler, vsOutput.uv) * vsOutput.color;
    }
    else if(vsOutput.fillMode == -1) output.color0SW = vsOutput.color;
    else if(vsOutput.fillMode == -2) {
        float2 uv = vsOutput.uv * 2.0 - 1.0;
        float distance = length(uv);

        if(distance <= 1.0 && distance >= (1.0 - 0.5)) output.color0SW = vsOutput.color;
        else discard;
    }



    return output;
}