package engine.graphics;

public class Session {
    private static boolean isDiscrete;

    private Session(){}

    public static boolean isDiscrete() {
        return isDiscrete;
    }

    public static void setDiscrete(boolean discrete) {
        isDiscrete = discrete;
    }
}
