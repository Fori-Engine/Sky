package lake.graphics.opengl;

/***
 * A fast array-based implementation for looking up Textures in existing slots
 */
public class FastTextureLookup {
    public GLTexture2D[] textures;
    public int capacity;
    public FastTextureLookup(int capacity) {
        this.capacity = capacity;
        textures = new GLTexture2D[capacity];
    }
    public boolean hasTexture(GLTexture2D texture){
        for(GLTexture2D t : textures){
            if(t == texture) return true;
        }
        return false;
    }
    public void registerTexture(GLTexture2D texture, Integer glSlot){
        textures[glSlot] = texture;
    }
    public int getTexture(GLTexture2D texture){
        for (int i = 0; i < textures.length; i++) {
            GLTexture2D t = textures[i];
            if (t == texture) {
                return i;
            }
        }
        return 0;
    }

    public void clear(){
        for (int i = 0; i < capacity; i++) {
            textures[i] = null;
        }
    }
}
