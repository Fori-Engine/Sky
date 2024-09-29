package fori.graphics;

import java.util.ArrayList;
import java.util.Arrays;

public class ShaderResSet {
    private int set;
    private ArrayList<ShaderRes> shaderResources = new ArrayList<>();

    public ShaderResSet(int set, ShaderRes... shaderResources) {
        this.set = set;
        this.shaderResources.addAll(Arrays.asList(shaderResources));
    }

    public ArrayList<ShaderRes> getShaderResources() {
        return shaderResources;
    }
}
