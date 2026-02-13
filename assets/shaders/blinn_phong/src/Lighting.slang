#include "SceneDesc.slang"
StructuredBuffer<Scene> scene_desc;


Texture2D input_color_texture;
RWTexture2D output_color_texture;
Texture2D input_pos_vs_texture;
Texture2D input_normal_ws_texture;

Texture2D input_shadow_maps[MAX_LIGHTS];
SamplerState input_shadow_maps_samplers[MAX_LIGHTS];

[numthreads(16, 16, 1)]
void computeMain(uniform int light_count, int3 dispatch_ID : SV_DispatchThreadID) {

    int3 texture_xy_ss = int3(dispatch_ID.x, dispatch_ID.y, 0);

    if(true) {
    
        float4 output_color = input_color_texture.Load(texture_xy_ss, 0);

        Camera camera = scene_desc[0].camera;
        float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);


        float4 fragment_vs = input_pos_vs_texture.Load(texture_xy_ss, 0);
        float4 fragment_ws = mul(camera.invView, fragment_vs);

        float ambient = 0.05;
        float intensity = ambient;
    
    
        for(int light_index = 0; light_index < light_count; light_index++) {
            Light light = scene_desc[0].lights[light_index];

            float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
            float3 light_dir_ws = normalize(light_pos_ws - fragment_ws.xyz);
            float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);


            float3 normal_ws = normalize(input_normal_ws_texture.Load(texture_xy_ss, 0).xyz);
            float4 adjusted_fragment_ws = fragment_ws + float4(normal_ws, 1.0) * 0.001 * abs(1 - dot(light_dir_ws, normal_ws));


            float4 fragment_shadow_map_ps = mul(light.proj, mul(light.view, adjusted_fragment_ws));
            fragment_shadow_map_ps /= fragment_shadow_map_ps.w;
            float4 shadow_map_uv_ts = fragment_shadow_map_ps * 0.5 + 0.5;

            float shadow_map_texel_w, shadow_map_texel_h, shadow_map_num_levels;
            input_shadow_maps[light_index].GetDimensions(0, shadow_map_texel_w, shadow_map_texel_h, shadow_map_num_levels);
            shadow_map_texel_w = 1 / shadow_map_texel_w;
            shadow_map_texel_h = 1 / shadow_map_texel_h;
            float shadow_samples = 0;

            if(shadow_map_uv_ts.x >= 0 && shadow_map_uv_ts.x <= 1 && shadow_map_uv_ts.y >= 0 && shadow_map_uv_ts.y <= 1) {
                int pcf_kernel_w = 1;

                //Shadows
                {
                    for(int x = -pcf_kernel_w; x <= pcf_kernel_w; x++) {
                        for(int y = -pcf_kernel_w; y <= -pcf_kernel_w; y++) {
                            float shadow_map_depth = input_shadow_maps[light_index].Sample(input_shadow_maps_samplers[light_index], shadow_map_uv_ts.xy + float2(x, y) * float2(shadow_map_texel_w, shadow_map_texel_h)).z;

                            shadow_samples += (shadow_map_depth < fragment_shadow_map_ps.z) ? 0.5 : 0.0;
                        }
                    }

                    shadow_samples /= pow(2 * pcf_kernel_w + 1, 2);
               
                }

                //Lighting BRDF
                {
                    {
                        float diffuse = 0.15 * max(dot(light_dir_ws, normal_ws), 0.0);
            
                        float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);
                        float3 halfway_dir_ws = normalize(light_dir_ws + camera_dir_ws);

                        float specular = 0.8 * pow(dot(halfway_dir_ws, normal_ws), 512);

                        intensity = ambient + diffuse + specular - shadow_samples;
                    }
                }
            }


        }

        output_color.xyz *= intensity;
        output_color_texture[texture_xy_ss.xy] = output_color;
    }
    else 
        output_color_texture[texture_xy_ss.xy] = float4(0.0);

}