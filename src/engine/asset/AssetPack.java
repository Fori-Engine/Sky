package engine.asset;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import engine.FileSystem;

import engine.Logger;
import engine.ExceptionUtil;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
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
    }

    private static void generateAssetMap(AssetMap assetMap, File path){

        try(MemoryStack stack = MemoryStack.stackPush()) {

            for (File assetFile : path.listFiles()) {
                if (!assetFile.isDirectory()) {
                    Asset target = null;
                    int sizeBytes = 0;

                    if (assetFile.getName().endsWith(".png") || assetFile.getName().endsWith(".jpg") || assetFile.getName().endsWith("jpeg")) {
                        IntBuffer w = stack.callocInt(1);
                        IntBuffer h = stack.callocInt(1);
                        IntBuffer channelsInFile = stack.callocInt(1);
                        ByteBuffer texture = STBImage.stbi_load(assetFile.getPath(), w, h, channelsInFile, 4);
                        int size = texture.remaining();
                        byte[] bytes = new byte[texture.remaining()];
                        sizeBytes = size;

                        texture.limit(size);
                        texture.get(bytes);
                        texture.limit(texture.capacity()).rewind();

                        target = new Asset<>(assetFile.getName(), new TextureData(bytes, w.get(0), h.get(0)));

                        MemoryUtil.memFree(texture);
                    } else if (assetFile.getName().endsWith(".glsl")) {
                        target = new Asset<>(assetFile.getName(), FileSystem.readString(assetFile.toPath()));
                        sizeBytes = target.asset.toString().getBytes().length;
                    } else if (assetFile.getName().endsWith(".fnt")) {
                        target = new Asset<>(assetFile.getName(), FileSystem.readString(assetFile.toPath()));
                        sizeBytes = target.asset.toString().getBytes().length;
                    } else if (assetFile.getName().endsWith(".obj")) {
                        byte[] bytes = FileSystem.readBytes(assetFile.toPath());
                        target = new Asset<>(assetFile.getName(), bytes);
                        sizeBytes = bytes.length;
                    }
                    else if(assetFile.getName().endsWith(".spv")) {
                        byte[] bytes = FileSystem.readBytes(assetFile.toPath());
                        target = new Asset<>(assetFile.getName(), bytes);
                        sizeBytes = bytes.length;
                    }
                    assetMap.sizeBytes += sizeBytes;
                    assetMap.put(assetFile.getPath().replace(File.separatorChar, '/'), target);
                } else {
                    generateAssetMap(assetMap, assetFile);
                }


            }
        }
    }


}
