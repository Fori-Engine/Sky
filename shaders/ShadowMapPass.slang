#define MAX_LIGHTS 10
#define MAX_TRANSFORMS 10

struct Camera {
    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;
}

struct Light {
    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;
}

struct Scene {
    Camera camera;
    Light lights[MAX_LIGHTS];
}

StructuredBuffer<Scene> sceneDesc;


Texture2D inputTexture;
Texture2D inputShadowMaps[1];
SamplerState inputShadowMapsSamplerStates[1];

Texture2D inputPosTexture;
RWTexture2D outputTexture;


[numthreads(2, 2, 2)]
void computeMain(int3 dispatchThreadID : SV_DispatchThreadID) {
    int3 inputTextureUV = int3(dispatchThreadID.x, dispatchThreadID.y, 0);
    float4 color = inputTexture.Load(inputTextureUV, 0);
    float4 wpFromCamera = mul(sceneDesc[0].camera.invView, inputPosTexture.Load(inputTextureUV, 0));

    
    Light light = sceneDesc[0].lights[0];
    float4 shadowMapUV = mul(light.proj, mul(light.view, wpFromCamera));
    shadowMapUV /= shadowMapUV.w;
    shadowMapUV = shadowMapUV * 0.5 + 0.5;

    float4 lpFromShadowMap = inputShadowMaps[0].Sample(inputShadowMapsSamplerStates[0], shadowMapUV.xy);
    float4 wpFromShadowMap = mul(light.invView, lpFromShadowMap);


    if(wpFromShadowMap.z < wpFromCamera.z - 0.005) {
        color.xyz *= 0.1;
    }


    outputTexture[inputTextureUV.xy] = color;
}