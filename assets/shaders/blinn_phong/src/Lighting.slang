#include "SceneDesc.slang"
StructuredBuffer<Scene> sceneDesc;


Texture2D inputColorTexture;
RWTexture2D outputColorTexture;
Texture2D inputPosTexture;
Texture2D inputNormalTexture;

Texture2D inputShadowMaps[MAX_LIGHTS];
SamplerState inputShadowMapsSamplers[MAX_LIGHTS];

[numthreads(16, 16, 1)]
void computeMain(uniform int light_count, int3 dispatch_ID : SV_DispatchThreadID) {

    int3 texture_xy_ss = int3(dispatch_ID.x, dispatch_ID.y, 0);
    
    float4 output_color = inputColorTexture.Load(texture_xy_ss, 0);

    Camera camera = sceneDesc[0].camera;
    float4 fragment_vs = inputPosTexture.Load(texture_xy_ss, 0);
    float4 fragment_ws = mul(camera.invView, fragment_vs);
    
    for(int light_index = 0; light_index < light_count; light_index++) {


        Light light = sceneDesc[0].lights[light_index];

        float4 shadow_map_uv_ts = mul(light.proj, mul(light.view, fragment_ws));
        shadow_map_uv_ts /= shadow_map_uv_ts.w;
        shadow_map_uv_ts = shadow_map_uv_ts * 0.5 + 0.5;

        float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
        float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

        float3 light_dir_ws = normalize(light_pos_ws - fragment_ws.xyz);
        float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);

        float3 normal = inputNormalTexture.Load(texture_xy_ss, 0).xyz;


        

        //Shadows
        {

            if(shadow_map_uv_ts.x >= 0 && shadow_map_uv_ts.x <= 1 && shadow_map_uv_ts.y >= 0 && shadow_map_uv_ts.y <= 1) {

                float4 lpFromShadowMap = inputShadowMaps[light_index].Sample(inputShadowMapsSamplers[light_index], shadow_map_uv_ts.xy);
                float4 lpFragment = mul(light.view, fragment_ws);

                if(lpFragment.z < lpFromShadowMap.z) {
                    output_color.xyz *= 0.1;
                }
            }
        }

        //Lighting BRDF
        {

            float intensity = 0;
            {
                float ambient = 0.2;
                float diffuse = 0.2 * max(dot(light_dir_ws, normal), 0.0);

                float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);
                float3 halfway_dir_ws = normalize(light_dir_ws + camera_dir_ws);

                float specular = 0.6 * pow(dot(halfway_dir_ws, normal), 256);

                intensity = ambient + diffuse + specular;
            }


            output_color.xyz *= intensity;
        }





    }


    outputColorTexture[texture_xy_ss.xy] = output_color;
}