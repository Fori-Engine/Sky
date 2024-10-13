package noir.citizens;

import fori.Input;
import fori.Time;
import fori.ecs.Engine;
import fori.ecs.EntitySystem;
import fori.ecs.MessageQueue;
import fori.graphics.PlatformWindow;

public class InputSystem extends EntitySystem {
    private PlatformWindow window;

    public InputSystem(PlatformWindow window) {
        this.window = window;
    }

    @Override
    public void update(Engine ecs, MessageQueue messageQueue) {


        ecs.view(MeshComponent.class, meshComponent -> {
            if(window.isKeyPressed(Input.KEY_RIGHT)) meshComponent.transform.translate(2 * Time.deltaTime(), 0, 0);
            if(window.isKeyPressed(Input.KEY_LEFT)) meshComponent.transform.translate(-2 * Time.deltaTime(), 0, 0);
            if(window.isKeyPressed(Input.KEY_UP)) meshComponent.transform.translate(0, 0, -2 * Time.deltaTime());
            if(window.isKeyPressed(Input.KEY_DOWN)) meshComponent.transform.translate(0, 0, 2 * Time.deltaTime());

            if(window.isKeyPressed(Input.KEY_PERIOD)) meshComponent.transform.rotate((float) Math.toRadians(30 * Time.deltaTime()), 0, 0, 1);
            if(window.isKeyPressed(Input.KEY_COMMA)) meshComponent.transform.rotate((float) Math.toRadians(-30 * Time.deltaTime()), 0, 0, 1);

        });

    }
}
