package fori.graphics;

import fori.graphics.vulkan.VulkanGraphicsCommandList;

public abstract class CommandList extends Disposable {
    protected int frameIndex;
    protected RenderTarget renderTarget;
    protected int framesInFlight;
    protected Semaphore[] waitSemaphores;
    protected Semaphore[] finishedSemaphores;
    public CommandList(Disposable parent, int framesInFlight) {
        super(parent);
        this.framesInFlight = framesInFlight;
    }

    public static GraphicsCommandList newGraphicsCommandList(Disposable disposable, int framesInFlight) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanGraphicsCommandList(disposable, framesInFlight);

        return null;
    }


    public void startRecording(Semaphore[] waitSemaphores, RenderTarget renderTarget, int frameIndex) {
        this.waitSemaphores = waitSemaphores;
        this.renderTarget = renderTarget;
        this.frameIndex = frameIndex;
    }
    public abstract void endRecording();
    public abstract void run();

    public void setWaitOn(Semaphore... waitSemaphores) {
        this.waitSemaphores = waitSemaphores;
    }


    public Semaphore[] getWaitSemaphores() {
        return waitSemaphores;
    }

    public Semaphore[] getFinishedSemaphores() {
        return finishedSemaphores;
    }
}
