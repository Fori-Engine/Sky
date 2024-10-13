package noir.citizens;

import fori.ecs.Component;
import fori.graphics.Camera;

public class CameraComponent extends Component {
    public Camera camera;

    public CameraComponent(Camera camera) {
        this.camera = camera;
    }
}
