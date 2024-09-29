package fori.asset;

import java.io.File;

public class KryoTest {
    public static void main(String[] args) {


        System.out.println("Packaging...");
        AssetPack.buildPack(new File("assets"), new File("assets.pkg"));





        //AssetPack assetPack = AssetPack.openLocal(new File("assets"));//AssetPack.openPack(new File("assets.pkg"));











    }

}
