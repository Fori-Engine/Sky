package fori.asset.wicktf;

import java.io.*;
import java.nio.ByteBuffer;

public class Demo {
    public static void main(String[] args) throws IOException {
        //WickTF Header


        //First int -> Vertex Count
        //Next int -> Index Count

        //Next String -> Material Name
        //Next float[] -> vertices (with interleaved positions, texcoords)
        //Next int[] indices

        int vertexCount = 0, indexCount = 0;

        float[] vertices = new float[]{
                0, 0,
                0, 0,

                0, 300,
                0, 1,

                300, 300,
                1, 1,

                300, 0,
                1, 0
        };

        int[] indices = new int[]{0, 1, 2, 2, 3, 0};

        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream("model.wick"));

        //Magic Header
        dataOutputStream.write(0x0F);
        dataOutputStream.write(vertexCount);
        dataOutputStream.write(indexCount);
        dataOutputStream.writeUTF("MyTexture.png");

        for(float vertexPart : vertices){
            dataOutputStream.writeFloat(vertexPart);
        }

        for(int index : indices){
            dataOutputStream.writeInt(index);
        }

        dataOutputStream.close();



    }

}
