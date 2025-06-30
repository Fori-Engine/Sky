package fori.ecs;

public class NativeResComponent {
    protected NativeResReleaseCallback releaseCallback;

    public void release() {
        releaseCallback.release();
    }

}
