package fori.graphics;

import fori.graphics.vulkan.VulkanComputeCommandList;
import fori.graphics.vulkan.VulkanGraphicsCommandList;

import java.util.Optional;

public abstract class CommandList extends Disposable {
    protected int frameIndex;
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

    public static ComputeCommandList newComputeCommandList(Disposable disposable, int framesInFlight) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanComputeCommandList(disposable, framesInFlight);
        return null;
    }


    public void startRecording(Semaphore[] waitSemaphores, int frameIndex) {
        this.waitSemaphores = waitSemaphores;
        this.frameIndex = frameIndex;
    }
    public abstract void endRecording();
    public abstract void run(Optional<Fence[]> submissionFences);
    public abstract void waitForFinish();

    public Semaphore[] getWaitSemaphores() {
        return waitSemaphores;
    }

    public Semaphore[] getFinishedSemaphores() {
        return finishedSemaphores;
    }
}
