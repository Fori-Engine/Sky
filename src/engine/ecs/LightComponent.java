package engine.ecs;

import engine.graphics.RenderTarget;
import org.joml.Matrix4f;

@ComponentArray(mask = 1 << 3)
public class LightComponent {
    public Matrix4f view, proj;
    public Matrix4f invView, invProj;
    public boolean invertY;
    public RenderTarget renderTarget;
    public LightComponent(Matrix4f view, Matrix4f proj, boolean invertY, RenderTarget renderTarget) {
        setView(view);


        setProj(proj);
        this.invertY = invertY;
        this.renderTarget = renderTarget;

        if(invertY){
            this.proj.m11(this.proj.m11() * -1);
        }
    }

    public Matrix4f getProj() {
        return proj;
    }

    public void setProj(Matrix4f proj) {
        this.proj = proj;
        this.invProj = new Matrix4f(proj).invert();
    }

    public void setView(Matrix4f view) {
        this.view = view;
        this.invView = new Matrix4f(view).invert();
    }


}
