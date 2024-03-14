package lake.editor;

import java.io.File;

public class ScriptCore {
    private final LakeEditor lakeEditor;

    public ScriptCore(LakeEditor lakeEditor){
        this.lakeEditor = lakeEditor;

    }

    public void update(ProjectRef projectRef){

        SceneViewport sceneViewport = (SceneViewport) lakeEditor.getPanels().get("sceneViewport");

        if(projectRef.isProjectOpened()) {
            projectRef.update();

            synchronized (projectRef.getProjectRefLock()){

                File projectPath = projectRef.getProjectPath();

                if(projectRef.getShouldReloadProject()){


                    if(projectRef.openProject(projectPath, sceneViewport.getWidth(), sceneViewport.getHeight())){
                        sceneViewport.useFramebuffer2D(projectRef.getViewportTextureID());
                        projectRef.setShouldReloadProject(false);
                    }

                }

            }

        }

        if(projectRef.getLastProjectCrashed()){
            sceneViewport.disconnect();
        }







    }
}
