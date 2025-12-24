package fori.ecs;

import fori.graphics.RenderTarget;
import org.joml.Matrix4f;

public class LightComponent {
    public Matrix4f view, proj;
    public Matrix4f invView, invProj;
    public boolean invertY;
    public RenderTarget renderTarget;
    public LightComponent(Matrix4f view, Matrix4f proj, boolean invertY, RenderTarget renderTarget) {
        this.view = view;
        this.proj = proj;
        this.invView = new Matrix4f(view).invert();
        this.invProj = new Matrix4f(proj).invert();
        this.invertY = invertY;
        this.renderTarget = renderTarget;

        if(invertY){
            this.proj.m11(this.proj.m11() * -1);
        }
    }


}
