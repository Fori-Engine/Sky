#include "SceneDesc.slang"

struct Transforms {
    float4x4 transforms[MAX_TRANSFORMS];
}

StructuredBuffer<Scene> scene_desc;
StructuredBuffer<Transforms> transforms;

struct VSOutput {
    float4 pos : SV_POSITION;
    float4 pos_vs;
    float4 color;
    float3 normal_ws;
    float2 uv_ts;
    float1 entity_index;
    float3x3 tbn_ws_inv;
}

struct Vertex
{
    float3 pos_ws;
    float1 entity_index;
    float4 color;
    float3 normal_ws;
    float2 uv_ts;
    float3 tangent_ws;
}



[shader("vertex")]
VSOutput vertexMain(Vertex vertex, uniform int render_mode, uniform int light_index) {
    VSOutput output;
    float4x4 view, proj;


    
    if(render_mode == 0) {
        view = scene_desc[0].camera.view;
        proj = scene_desc[0].camera.proj;
    }
    if(render_mode == 1) {
        view = scene_desc[0].lights[light_index].view;
        proj = scene_desc[0].lights[light_index].proj;
    }
    
    float4x4 transform = transforms[0].transforms[int(vertex.entity_index)];

    output.normal_ws = normalize(mul(float3x3(transform), vertex.normal_ws));
    output.pos_vs = mul(view, mul(transform, float4(vertex.pos_ws, 1.0)));
    output.pos = mul(proj, output.pos_vs);
    output.color = vertex.color;
    output.uv_ts = vertex.uv_ts;
    output.entity_index = vertex.entity_index;

    float3 tangent_ws = normalize(mul(float3x3(transform), vertex.tangent_ws));
    float3 bitangent_ws = cross(output.normal_ws, tangent_ws);

    output.tbn_ws_inv = transpose(float3x3(tangent_ws, bitangent_ws, output.normal_ws));

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
    float4 color1 : SV_Target1;
    float4 color2 : SV_Target2;
}


Texture2D color_map_texture;
SamplerState color_map_sampler;

Texture2D normal_map_texture;
SamplerState normal_map_sampler;

[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, uniform int render_mode, uniform int light_index) {
    FSOutput output;

    if (render_mode == 0) {

        float3 normal_ts = normal_map_texture.Sample(normal_map_sampler, vsOutput.uv_ts).xyz * 2.0 - 1.0;
        float3 normal_ws = normalize(mul(vsOutput.tbn_ws_inv, normalize(normal_ts)));

        output.color0 = float4(vsOutput.color.xyz * color_map_texture.Sample(color_map_sampler, vsOutput.uv_ts).xyz, 1.0);
        output.color1 = float4(vsOutput.pos_vs.xyz, 1.0);
        output.color2 = float4(normal_ws, 1.0);
    }
    if(render_mode == 1) {
        output.color0 = float4(vsOutput.pos.z, vsOutput.pos.z, vsOutput.pos.z, 1.0);
    }



    return output;
}