package fori.graphics;

public class VertexAttributes {
    public enum Type {
        PositionFloat3(3),
        TransformIndexFloat1(1),
        UVFloat2(2),
        ColorFloat4(4);

        public int size;
        Type(int size) {
            this.size = size;
        }
    }

    public static int getSize(Type[] attributes){
        int i = 0;

        for (int j = 0; j < attributes.length; j++) {
            i += attributes[j].size;
        }

        return i;

    }


}
