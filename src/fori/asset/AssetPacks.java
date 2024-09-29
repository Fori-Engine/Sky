package fori.asset;

import fori.Logger;

import java.util.HashMap;

public class AssetPacks {
    private static HashMap<String, AssetPack> assetPackMap = new HashMap<>();
    private AssetPacks(){}
    public static <T> Asset<T> getAsset(String path) {

        String[] tokens = path.split(":");

        String assetPackKey = tokens[0];
        String assetPath = tokens[1];

        AssetPack assetPack = assetPackMap.get(assetPackKey);

        String error = "";

        if(assetPack == null){
            error = "Failed to locate AssetPack for key [" + assetPackKey + "]";
            Logger.meltdown(AssetPacks.class, error);
            throw new RuntimeException(error);
        }

        Asset<T> asset = assetPack.getAsset(assetPath);
        if(asset == null){
            error = "Failed to locate Asset [" + assetPath + "] in AssetPack [" + assetPackKey + "]";
            Logger.meltdown(AssetPacks.class, error);
            throw new RuntimeException(error);
        }

        return asset;
    }

    public static void open(String assetPackKey, AssetPack assetPack){
        assetPackMap.put(assetPackKey, assetPack);
    }

}
