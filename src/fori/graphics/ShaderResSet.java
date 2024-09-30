package fori.graphics;

import java.util.ArrayList;

public class ShaderResSet {
    public final int set;

    private ArrayList<ShaderRes> shaderResources = new ArrayList<>();

    public ShaderResSet(int set, ShaderRes... shaderResources) {
        this.set = set;

        for(ShaderRes shaderRes : shaderResources){
            shaderRes.set = ShaderResSet.this;
            ShaderResSet.this.shaderResources.add(shaderRes);

        }
    }





    public ArrayList<ShaderRes> getShaderResources() {
        return shaderResources;
    }
}
