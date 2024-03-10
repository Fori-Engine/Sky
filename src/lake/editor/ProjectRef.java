package lake.editor;

import lake.script.EditorUI;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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


    public boolean openProject(File path) {
        if (isProjectOpened())
            closeProject();

        this.path = path;
        setProjectOpened(true);
        setCrashed(false);

        try {

            ClassLoader classLoader = new URLClassLoader(new URL[]{path.toURI().toURL()});

            Class editorLauncherClass = classLoader.loadClass("game.EditorLauncher");
            mInitRenderer = editorLauncherClass.getDeclaredMethod("initRenderer");
            mCreate = editorLauncherClass.getDeclaredMethod("create");
            mUpdate = editorLauncherClass.getDeclaredMethod("update");
            mDispose = editorLauncherClass.getDeclaredMethod("disposeRenderer");


            Constructor<?> constructor = editorLauncherClass.getDeclaredConstructor(String.class, int.class, int.class);
            editorLauncherInstance = constructor.newInstance("", 1000, 600);

            viewportTextureID = (Integer) mInitRenderer.invoke(editorLauncherInstance);

            logger.log(Level.INFO, "Editor Preview-Framebuffer Acquired! " + this.path.getPath());
            mCreate.invoke(editorLauncherInstance);

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return true;
    }


    public void closeProject(){
        if(isProjectOpened()){
            setProjectOpened(false);
            setPauseUpdate(false);
            dispose();
            EditorUI.getRegistry().clear();
        }
    }

    public boolean isPauseUpdate() {
        return pauseUpdate;
    }

    public void setPauseUpdate(boolean pauseUpdate) {
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

    public boolean isCrashed() {
        return crashed;
    }

    private void setCrashed(boolean crashed) {
        this.crashed = crashed;
    }

    public Throwable getThrowableLog() {
        return throwable;
    }

    private void setThrowableLog(Throwable throwable) {
        this.throwable = throwable;
    }

    public void clearThrowableLog() {
        this.throwable = null;
    }

    public void invokeUpdate(){
        if(isPauseUpdate() || isCrashed()) return;

        try {
            mUpdate.invoke(editorLauncherInstance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            closeProject();
            setCrashed(true);
            setThrowableLog(e);
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
