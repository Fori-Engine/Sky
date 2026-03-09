package engine.physx;

import org.joml.Vector3f;
import physx.common.PxVec3;
import physx.physics.PxScene;

public class Physx {
    private static PxScene scene;

    public static PxScene getScene() {
        return scene;
    }

    public static void setScene(PxScene scene) {
        Physx.scene = scene;
    }

    public static void copyTo(Vector3f vec3, PxVec3 pxVec3) {
        vec3.x = pxVec3.getX();
        vec3.y = pxVec3.getY();
        vec3.z = pxVec3.getZ();
    }

    public static void copyTo(PxVec3 pxVec3, Vector3f vec3) {
        pxVec3.setX(vec3.x);
        pxVec3.setY(vec3.y);
        pxVec3.setZ(vec3.z);
    }


}
