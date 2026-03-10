package engine.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.vecmath.Quat4f;

public class TypeUtil {
    private TypeUtil() {}

    public static void copy(Vector3f src, javax.vecmath.Vector3f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
    }

    public static void copy(javax.vecmath.Vector3f src, Vector3f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
    }

    public static void copy(Quaternionf src, javax.vecmath.Quat4f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
        dst.w = src.w;
    }

    public static void copy(javax.vecmath.Quat4f src, Quaternionf dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
        dst.w = src.w;
    }

    public static javax.vecmath.Vector3f vec3(Vector3f src) {
        return new javax.vecmath.Vector3f(src.x, src.y, src.z);
    }



    public static Quat4f quat4(Quaternionf src) {
        return new Quat4f(src.x, src.y, src.z, src.w);
    }


}
