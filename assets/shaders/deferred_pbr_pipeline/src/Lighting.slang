#include "SceneDesc.slang"
StructuredBuffer<Scene> sceneDesc;


Texture2D inputColorTexture;
RWTexture2D outputColorTexture;
Texture2D inputPosTexture;
Texture2D inputNormalTexture;

Texture2D inputShadowMaps[1];
SamplerState inputShadowMapsSamplers[1];

[numthreads(32, 32, 1)]
void computeMain(int3 dispatchID : SV_DispatchThreadID) {

    int3 textureXY = int3(dispatchID.x, dispatchID.y, 0);
    
    float4 outputColor = inputColorTexture.Load(textureXY, 0);

    float4 wpFromCamera = mul(sceneDesc[0].camera.invView, inputPosTexture.Load(textureXY, 0));
    

    Light light = sceneDesc[0].lights[0];
    Camera camera = sceneDesc[0].camera;

    float4 shadowMapUV = mul(light.proj, mul(light.view, wpFromCamera));
    shadowMapUV /= shadowMapUV.w;
    shadowMapUV = shadowMapUV * 0.5 + 0.5;

    float3 lightPos = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
    float3 cameraPos = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

    float3 lightDir = normalize(lightPos - wpFromCamera.xyz);
    float3 normal = inputNormalTexture.Load(textureXY, 0).xyz;



    //Shadows
    {
    
        float4 lpFromShadowMap = inputShadowMaps[0].Sample(inputShadowMapsSamplers[0], shadowMapUV.xy);
        float4 wpFromShadowMap = mul(light.invView, lpFromShadowMap);


        if(wpFromShadowMap.z <= wpFromCamera.z - 0.005) {
            outputColor.xyz *= 0.1;
        }
    }

    //Lighting BRDF
    {

        float intensity = 0;
        {
            float ambient = 0.1;
            float diffuse = 0.20 * max(dot(lightDir, normal), 0.0);

            float3 cameraDir = normalize(cameraPos - wpFromCamera.xyz);
            float3 halfwayDir = normalize(lightDir + cameraDir);

            float specular = 0.9 * pow(dot(halfwayDir, normal), 256);
            
            intensity = ambient + diffuse + specular;
        }


        outputColor.xyz *= intensity;
    }


    outputColorTexture[textureXY.xy] = outputColor;
}