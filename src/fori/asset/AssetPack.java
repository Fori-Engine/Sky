package fori.asset;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import fori.FileReader;

import fori.Logger;
import fori.ExceptionUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class AssetPack {

    private AssetMap assetMap;
    public AssetPack(AssetMap assetMap) {
        this.assetMap = assetMap;
    }

    public <T> Asset<T> getAsset(String path) {
        return (Asset<T>) assetMap.get(path);
    }


    public static AssetPack openLocal(File path){

        AssetMap assetMap = new AssetMap();
        AssetPack assetPack = new AssetPack(assetMap);
        System.out.println("Indexing AssetMap...");
        generateAssetMap(assetMap, path);

        return assetPack;
    }

    private static void configure(Kryo kryo){
        kryo.register(AssetMap.class);
        kryo.register(byte[].class);
        kryo.register(String[].class);
        kryo.register(TextureData.class);
        kryo.register(Object[].class);
        kryo.register(Asset.class);
    }

    public static AssetPack openPack(File path){
        Kryo kryo = new Kryo();
        configure(kryo);

        Input input = null;
        try {
            input = new Input(new InflaterInputStream(new FileInputStream(path)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(Logger.error(AssetPack.class, ExceptionUtil.exceptionToString(e)));
        }
        AssetMap assetMap = kryo.readObject(input, AssetMap.class);
        input.close();

        return new AssetPack(assetMap);
    }
    public static void buildPack(File dir, File outputPath){

        Kryo kryo = new Kryo();
        configure(kryo);



        AssetMap assetMap = new AssetMap();
        System.out.println("Indexing AssetMap...");
        generateAssetMap(assetMap, dir);


        Output output = null;
        try {
            output = new Output(new DeflaterOutputStream(new FileOutputStream(outputPath)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(Logger.error(AssetPack.class, ExceptionUtil.exceptionToString(e)));
        }


        kryo.writeObject(output, assetMap);

        output.flush();
        output.close();

        System.out.println("File Size: " + assetMap.sizeBytes + " bytes");
    }
    private static void generateAssetMap(AssetMap assetMap, File path){
        for(File asset : path.listFiles()){
            if(!asset.isDirectory()){
                System.out.println("\t" + asset.getPath());

                Asset target = null;
                int sizeBytes = 0;

                if(asset.getName().endsWith(".png")){
                    IntBuffer w = BufferUtils.createIntBuffer(1);
                    IntBuffer h = BufferUtils.createIntBuffer(1);
                    IntBuffer channelsInFile = BufferUtils.createIntBuffer(1);
                    ByteBuffer texture = STBImage.stbi_load(asset.getPath(), w, h, channelsInFile, 4);
                    int size = texture.remaining();
                    byte[] bytes = new byte[texture.remaining()];
                    sizeBytes += size;

                    texture.limit(size);
                    texture.get(bytes);
                    texture.limit(texture.capacity()).rewind();

                    target = new Asset<>(new TextureData(bytes, w.get(), h.get()));
                }
                if(asset.getName().endsWith(".glsl")){
                    target = new Asset<>(FileReader.readFile(asset.getPath()));
                    sizeBytes += target.asset.toString().getBytes().length;
                }
                if(asset.getName().endsWith(".fnt")){
                    target = new Asset<>(FileReader.readFile(asset.getPath()));
                    sizeBytes += target.asset.toString().getBytes().length;
                }

                System.out.println("\t\t(" + sizeBytes + ") bytes");
                assetMap.sizeBytes += sizeBytes;




                assetMap.put(asset.getPath().replace(File.separatorChar, '/'), target);
            }
            else {
                generateAssetMap(assetMap, asset);
            }


        }
    }


}
