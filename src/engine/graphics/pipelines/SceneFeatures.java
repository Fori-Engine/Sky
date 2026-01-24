package engine.graphics.pipelines;

import engine.ecs.Scene;

public class SceneFeatures extends Features {
    private Scene scene;
    protected SceneFeatures(boolean mandatory) {
        super(mandatory);
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }
}
