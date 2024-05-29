package lake.asset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class KryoTest {
    public static void main(String[] args) {


        System.out.println("Packaging...");
        AssetPack.buildPack(new File("assets"), new File("assets.pkg"));





        //AssetPack assetPack = AssetPack.openLocal(new File("assets"));//AssetPack.openPack(new File("assets.pkg"));











    }

}
