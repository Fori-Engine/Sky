#include "SceneDesc.slang"
StructuredBuffer<Scene> sceneDesc;


Texture2D inputColorTexture;
RWTexture2D outputColorTexture;
Texture2D inputPosTexture;
Texture2D inputNormalTexture;

Texture2D inputShadowMaps[MAX_LIGHTS];
SamplerState inputShadowMapsSamplers[MAX_LIGHTS];

[numthreads(16, 16, 1)]
void computeMain(uniform int lightCount, int3 dispatchID : SV_DispatchThreadID) {

    int3 textureXY = int3(dispatchID.x, dispatchID.y, 0);
    
    float4 outputColor = inputColorTexture.Load(textureXY, 0);

    Camera camera = sceneDesc[0].camera;
    float4 wpFromCamera = mul(sceneDesc[0].camera.invView, inputPosTexture.Load(textureXY, 0));
    
    for(int lightIndex = 0; lightIndex < lightCount; lightIndex++) {

        Light light = sceneDesc[0].lights[lightIndex];

        float4 shadowMapUV = mul(light.proj, mul(light.view, wpFromCamera));
        shadowMapUV /= shadowMapUV.w;
        shadowMapUV = shadowMapUV * 0.5 + 0.5;

        float3 lightPos = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
        float3 cameraPos = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

        float3 lightDir = normalize(lightPos - wpFromCamera.xyz);
        float3 cameraDir = normalize(cameraPos - wpFromCamera.xyz);

        float3 normal = inputNormalTexture.Load(textureXY, 0).xyz;


        

        //Shadows
        {

            float4 lpFromShadowMap = inputShadowMaps[lightIndex].Sample(inputShadowMapsSamplers[lightIndex], shadowMapUV.xy);
            float4 wpFromShadowMap = mul(light.invView, lpFromShadowMap);

            float3 planeNormal = normalize(lightPos - wpFromShadowMap.xyz);

            float d = -dot(planeNormal, wpFromCamera.xyz);
            wpFromCamera.xyz += d * planeNormal;

            //Using planeNormal for the equation of a plane representing the 
            //shadow map texel, we compute d, the adaptive bias to slide that texel
            //to the world space pos BUT
            //only apply it if the actual normal of the fragment (from pov camera) is facing away from
            //the light



            if(wpFromShadowMap.z <= wpFromCamera.z + 0.001) {
                outputColor.xyz *= 1;
            }
        }

        //Lighting BRDF
        {

            float intensity = 0;
            {
                float ambient = 0.2;
                float diffuse = 0.2 * max(dot(lightDir, normal), 0.0);

                float3 cameraDir = normalize(cameraPos - wpFromCamera.xyz);
                float3 halfwayDir = normalize(lightDir + cameraDir);

                float specular = 0.6 * pow(dot(halfwayDir, normal), 256);

                intensity = ambient + diffuse + specular;
            }


            outputColor.xyz *= intensity;
        }





    }


    outputColorTexture[textureXY.xy] = outputColor;
}