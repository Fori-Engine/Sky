package fori.graphics;

import fori.Surface;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkInstance;

public class AWTSurface extends Surface {
    public AWTSurface(Ref parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);
    }

    @Override
    public boolean getKeyPressed(int key) {
        return false;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return false;
    }

    @Override
    public boolean getMousePressed(int button) {
        return false;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return false;
    }

    @Override
    public Vector2f getMousePos() {
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {

    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        return null;
    }

    @Override
    public long getVulkanSurface(VkInstance instance) {
        return 0;
    }

    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        return false;
    }

    @Override
    public void display() {

    }

    @Override
    public void update() {

    }

    @Override
    public boolean shouldClose() {
        return false;
    }

    @Override
    public void dispose() {

    }
}
