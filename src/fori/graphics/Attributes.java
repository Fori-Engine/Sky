package fori.graphics;

public class Attributes {
    public enum Type {
        PositionFloat3(3),
        NormalFloat(3),
        RenderQueuePosFloat1(1),
        TransformIndexFloat1(1),
        UVFloat2(2),
        MaterialBaseIndexFloat1(1),
        ColorFloat4(4);

        public int size;
        Type(int size) {
            this.size = size;
        }
    }

    public static Type[] getDefaultAttributes(){
        return new Type[]{Type.PositionFloat3, Type.TransformIndexFloat1, Type.UVFloat2, Type.MaterialBaseIndexFloat1};
    }

    public static int getSize(Type[] attributes){
        int i = 0;

        for (int j = 0; j < attributes.length; j++) {
            i += attributes[j].size;
        }

        return i;

    }


}
