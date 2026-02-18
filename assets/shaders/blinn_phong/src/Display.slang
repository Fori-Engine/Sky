struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color; 
    float2 uv;
    float fill_mode;
    float texture_index;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
    float fill_mode;
    float texture_index;
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
    output.fill_mode = vertex.fill_mode;
    output.texture_index = vertex.texture_index;

    return output;
}

struct FSOutput {
    float4 color0SW : SV_Target0;
}

Texture2D input_textures[2];
SamplerState input_samplers[2];



[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {


    FSOutput output;
    if(vsOutput.fill_mode >= 0) {
        output.color0SW = input_textures[int(vsOutput.texture_index)].Sample(input_samplers[int(vsOutput.texture_index)], vsOutput.uv) * vsOutput.color;
    }
    else if(vsOutput.fill_mode == -1) output.color0SW = vsOutput.color;
    else if(vsOutput.fill_mode == -2) {
        float2 uv = vsOutput.uv * 2.0 - 1.0;
        float distance = length(uv);

        if(distance <= 1.0 && distance >= (1.0 - 0.5)) output.color0SW = vsOutput.color;
        else discard;
    }



    return output;
}