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

    float attenuationConstant;
    float attenuationLinear;
    float attenuationQuadratic;
}

struct Scene {
    Camera camera;
    Light lights[MAX_LIGHTS];
}