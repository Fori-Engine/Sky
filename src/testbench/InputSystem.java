package testbench;

import fori.Input;
import fori.Scene;
import fori.Surface;
import fori.Time;
import fori.ecs.EntitySystem;
import fori.ecs.MeshComponent;
import fori.ecs.MessageQueue;

public class InputSystem extends EntitySystem {
    private Surface surface;

    public InputSystem(Surface surface) {
        this.surface = surface;
    }

    @Override
    public void update(Scene scene, MessageQueue messageQueue) {

        /*


        scene.view(MeshComponent.class, (entity, meshComponent) -> {

            if(entity.getTag().equals("Colt9")) {

                if (window.isKeyPressed(Input.KEY_RIGHT)) meshComponent.transform.translate(2 * Time.deltaTime(), 0, 0);
                if (window.isKeyPressed(Input.KEY_LEFT)) meshComponent.transform.translate(-2 * Time.deltaTime(), 0, 0);
                if (window.isKeyPressed(Input.KEY_UP)) meshComponent.transform.translate(0, 0, -2 * Time.deltaTime());
                if (window.isKeyPressed(Input.KEY_DOWN)) meshComponent.transform.translate(0, 0, 2 * Time.deltaTime());

                if (window.isKeyPressed(Input.KEY_PERIOD))
                    meshComponent.transform.rotate((float) Math.toRadians(30 * Time.deltaTime()), 0, 1, 0);
                if (window.isKeyPressed(Input.KEY_COMMA))
                    meshComponent.transform.rotate((float) Math.toRadians(-30 * Time.deltaTime()), 0, 1, 0);

                if (window.isKeyPressed(Input.KEY_Z)) meshComponent.transform.scale((float) (0.99));
                if (window.isKeyPressed(Input.KEY_X)) meshComponent.transform.scale((float) (1.01));

            }
        });

         */



    }
}
