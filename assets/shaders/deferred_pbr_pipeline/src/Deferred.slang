#include "SceneDesc.slang"
StructuredBuffer<Scene> sceneDesc;


Texture2D inputTexture;
RWTexture2D outputTexture;
Texture2D inputPosTexture;

Texture2D inputShadowMaps[1];
SamplerState inputShadowMapsSamplers[1];

[numthreads(32, 32, 1)]
void computeMain(int3 dispatchID : SV_DispatchThreadID) {

    int3 inputTextureUV = int3(dispatchID.x, dispatchID.y, 0);
    
    float4 outputColor = inputTexture.Load(inputTextureUV, 0);

    float4 wpFromCamera = mul(sceneDesc[0].camera.invView, inputPosTexture.Load(inputTextureUV, 0));
    

    Light light = sceneDesc[0].lights[0];
    float4 shadowMapUV = mul(light.proj, mul(light.view, wpFromCamera));
    shadowMapUV /= shadowMapUV.w;
    shadowMapUV = shadowMapUV * 0.5 + 0.5;


    //Shadows
    {
    
        float4 lpFromShadowMap = inputShadowMaps[0].Sample(inputShadowMapsSamplers[0], shadowMapUV.xy);
        float4 wpFromShadowMap = mul(light.invView, lpFromShadowMap);


        if(wpFromShadowMap.z <= wpFromCamera.z - 0.005) {
            outputColor.xyz *= 0.1;
        }
    }
   


    outputTexture[inputTextureUV.xy] = outputColor;
}