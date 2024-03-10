package lake.editor;

import lake.script.EditorUI;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectRef {
    Logger logger = Logger.getLogger(ProjectRef.class.getName());
    private File path;
    private Method mInitRenderer, mCreate, mUpdate, mDispose;
    private Object editorLauncherInstance;
    private int viewportTextureID;




    private boolean projectOpened;
    private boolean pauseUpdate;
    private boolean crashed;
    private Throwable throwable;



    public ProjectRef(){

    }


    public boolean openProject(File path, int width, int height) {
        if (isProjectOpened())
            closeProject();

        this.path = path;
        setProjectOpened(true);
        setLastProjectCrashed(false);

        try {

            ClassLoader classLoader = new URLClassLoader(new URL[]{path.toURI().toURL()});

            Class editorLauncherClass = classLoader.loadClass("game.EditorLauncher");
            mInitRenderer = editorLauncherClass.getDeclaredMethod("initRenderer");



            mCreate = editorLauncherClass.getDeclaredMethod("create");
            mUpdate = editorLauncherClass.getDeclaredMethod("update");
            mDispose = editorLauncherClass.getDeclaredMethod("disposeRenderer");



            Constructor<?> constructor = editorLauncherClass.getDeclaredConstructor(String.class, int.class, int.class);
            editorLauncherInstance = constructor.newInstance("", width, height);

            viewportTextureID = (Integer) mInitRenderer.invoke(editorLauncherInstance);

            logger.log(Level.INFO, "Editor Preview-Framebuffer Acquired! " + this.path.getPath());
            mCreate.invoke(editorLauncherInstance);

        }
        catch (Exception e) {
            setLastProjectThrowableLog(e);
            closeProject();
            setLastProjectCrashed(true);


        }


        return true;
    }


    public void closeProject(){
        if(isProjectOpened()){
            setProjectOpened(false);
            setCurrentProjectPaused(false);
            dispose();
            EditorUI.getRegistry().clear();
        }
    }

    public boolean isCurrentProjectPaused() {
        return pauseUpdate;
    }

    public void setCurrentProjectPaused(boolean pauseUpdate) {
        this.pauseUpdate = pauseUpdate;
    }

    public File getProjectPath(){
        return path;
    }

    public Object getRuntimeConnectorInstance(){
        return editorLauncherInstance;
    }
    public int getViewportTextureID() {
        return viewportTextureID;
    }

    public boolean isProjectOpened() {
        return projectOpened;
    }

    private void setProjectOpened(boolean projectOpened) {
        this.projectOpened = projectOpened;
    }

    public boolean getLastProjectCrashed() {
        return crashed;
    }

    private void setLastProjectCrashed(boolean crashed) {
        this.crashed = crashed;
    }

    public Throwable getLastProjectThrowableLog() {
        return throwable;
    }

    private void setLastProjectThrowableLog(Throwable throwable) {
        this.throwable = throwable;
    }

    public void clearLastProjectThrowableLog() {
        this.throwable = null;
    }

    public void update(){
        if(isCurrentProjectPaused() || getLastProjectCrashed()) return;

        try {
            mUpdate.invoke(editorLauncherInstance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            closeProject();
            setLastProjectCrashed(true);
            setLastProjectThrowableLog(e);
        }
    }

    public void dispose(){
        try {
            mDispose.invoke(getRuntimeConnectorInstance());
            logger.log(Level.INFO, "Disposed project " + this.path.getPath());
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
