float3 reinhard_hdr(float3 hdr) {
    return hdr / (hdr + float3(1.0));
}