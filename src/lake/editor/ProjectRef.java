package lake.editor;

import lake.script.EditorUI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
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
    private Object projectLock = new Object();
    private boolean shouldReloadProject;

    private boolean registerWatchService;
    private Throwable throwable;

    public ProjectRef(){
        Thread fileWatcher = new Thread(() -> {
            try {


                while(true) {


                    Path path;

                    synchronized (projectLock){
                        if(getProjectPath() == null) continue;
                        path = getProjectPath().toPath();
                    }

                    WatchService watchService = FileSystems.getDefault().newWatchService();



                    WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

                    WatchKey key;

                    boolean quit = false;



                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            System.out.println(getProjectPath().getPath());

                            //So....this is necessary because in the small second before IntelliJ regenerating compiled files
                            //the files don't exist and openProject() fails

                            Thread.sleep(2000);




                            synchronized (projectLock) {
                                setShouldReloadProject(true);



                                if(registerWatchService){
                                    System.out.println("Changing WatchService");
                                    watchService.close();
                                    quit = true;
                                    registerWatchService = false;
                                    break;
                                }


                            }
                        }

                        key.reset();

                        if(quit){
                            break;
                        }

                    }










                }




            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        });
        fileWatcher.start();

    }


    public boolean openProject(File path, int width, int height) {
        if (isProjectOpened())
            closeProject();

        this.path = path;
        setProjectOpened(true);
        setLastProjectCrashed(false);

        synchronized (projectLock){
            registerWatchService = true;
        }

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
            e.printStackTrace();
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

    public Object getProjectRefLock(){
        return projectLock;
    }

    public void update(){
        if(isCurrentProjectPaused() || getLastProjectCrashed()) return;




        try {
            mUpdate.invoke(editorLauncherInstance);
        }
        catch (InvocationTargetException | IllegalAccessException e) {
            closeProject();
            setLastProjectCrashed(true);
            setLastProjectThrowableLog(e);
        }
    }

    public boolean getShouldReloadProject() {
        return shouldReloadProject;
    }

    public void setShouldReloadProject(boolean shouldReloadProject) {
        this.shouldReloadProject = shouldReloadProject;
    }

    public void dispose(){
        try {
            System.out.println(getRuntimeConnectorInstance().getClass().getSimpleName());
            mDispose.invoke(getRuntimeConnectorInstance());


            logger.log(Level.INFO, "Disposed project " + this.path.getPath());
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
