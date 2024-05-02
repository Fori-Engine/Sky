package lake.graphics.vulkan;

import java.nio.LongBuffer;

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
    private LVKFrameUniforms uniforms;


    public static class LVKFrameUniforms {
        private LVKGenericBuffer buffer;
        private long pMemory;
        public static int TOTAL_SIZE_BYTES = 3 * 16 * Float.BYTES;
        public static int MATRIX_SIZE_BYTES = 16 * Float.BYTES;

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

    public LVKFrameUniforms getUniforms() {
        return uniforms;
    }

    public void setUniforms(LVKFrameUniforms uniforms) {
        this.uniforms = uniforms;
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


    public long fence() {
        return fence;
    }

    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}