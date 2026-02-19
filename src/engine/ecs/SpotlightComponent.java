package engine.ecs;

import engine.graphics.RenderTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@ComponentArray(mask = 1 << 3)
public class SpotlightComponent {
    public Matrix4f view, proj;
    public Matrix4f invView, invProj;
    public boolean invertY;
    public RenderTarget renderTarget;
    public float attenuationConstant = 1.0f;
    public float attenuationLinear = 0.14f;
    public float attenuationQuadratic = 0.07f;
    public Vector3f color = new Vector3f(0, 1, 1);

    public SpotlightComponent(Matrix4f view, Matrix4f proj, boolean invertY, RenderTarget renderTarget) {
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
