package lake.graphics;

import java.io.File;

public abstract class Texture2D implements Disposable {
    private String path;
    private int width, height;
    public enum Filter {
        LINEAR,
        NEAREST
    }


    public void setProperties(String path, int width, int height) {
        if(!new File(path).exists()) throw new RuntimeException("The file " + path + " does not exist!");

        this.path = path;
        this.width = width;
        this.height = height;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
