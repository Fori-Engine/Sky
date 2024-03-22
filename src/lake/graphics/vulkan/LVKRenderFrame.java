package lake.graphics.vulkan;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackGet;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 * */
public class LVKRenderFrame {

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;
    private ArrayList<LVKFrameUniforms> uniformBuffers = new ArrayList<>();


    public static class LVKFrameUniforms {
        private LVKGenericBuffer buffer;
        private long pMemory;

        public float r = 1;





        public LVKFrameUniforms(LVKGenericBuffer buffer, long pMemory) {
            this.buffer = buffer;
            this.pMemory = pMemory;
        }

        public LVKGenericBuffer getBuffer() {
            return buffer;
        }

        public long getpMemory() {
            return pMemory;
        }
    }




    public LVKRenderFrame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }

    public long imageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public LongBuffer pImageAvailableSemaphore() {
        return stackGet().longs(imageAvailableSemaphore);
    }

    public long renderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return stackGet().longs(renderFinishedSemaphore);
    }

    public List<LVKFrameUniforms> uniformBuffers(){ return uniformBuffers;}

    public long fence() {
        return fence;
    }

    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}