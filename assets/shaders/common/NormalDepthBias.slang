#pragma once 
#include "SceneDesc.slang"

public struct NormalDepthBiasEval {

    float4 get_normal_biased_fragment_ws(Light light, float4 fragment_ws, float3 light_dir_ws, float3 normal_ws) {
        return fragment_ws + float4(normal_ws, 1.0) * light.shadowNormalOffsetBias * saturate(1 - dot(light_dir_ws, normal_ws));
    }
}