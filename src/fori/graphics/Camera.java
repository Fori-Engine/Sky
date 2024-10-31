package fori.graphics;

import org.joml.Matrix4f;

public class Camera {
    private Matrix4f proj, view;
    private boolean invertY;
    public static final int SIZE = SizeUtil.MATRIX_SIZE_BYTES * 2;

    public Camera(Matrix4f view, Matrix4f proj, boolean invertY) {
        this.view = view;
        this.proj = proj;
        this.invertY = invertY;

        if(invertY){
            this.proj.m11(this.proj.m11() * -1);
        }
    }

    public Matrix4f getProj() {
        return proj;
    }

    public void setProj(Matrix4f proj) {
        this.proj = proj;
    }

    public Matrix4f getView() {
        return view;
    }

    public void setView(Matrix4f view) {
        this.view = view;
    }
}
