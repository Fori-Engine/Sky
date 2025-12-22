package fori.graphics;

import java.util.*;

public class RenderGraph extends Disposable {

    private List<Pass> passes = new ArrayList<>();
    private Pass targetPass;

    public RenderGraph(Disposable parent) {
        super(parent);
    }

    public void setTargetPass(Pass targetPass) {
        this.targetPass = targetPass;
    }

    public Pass getTargetPass() {
        return targetPass;
    }

    public void addPasses(Pass... passes) {
        this.passes.addAll(Arrays.asList(passes));
    }

    public List<Pass> getPasses() {
        return passes;
    }

    @Override
    public void dispose() {

    }

    public List<Pass> walk(Pass targetPass) {
        LinkedList<Pass> passes = new LinkedList<>();
        tracePasses(passes, targetPass);
        passes.add(targetPass);

        return passes;
    }


    private List<Pass> getAllDependencyWriters(Pass thisPass, ResourceDependency resourceDependency) {

        List<Pass> writers = new ArrayList<>();

        for(Pass otherPass : passes) {
            if(otherPass != thisPass) {
                for (ResourceDependency otherResourceDependency : otherPass.getResourceDependencies()) {
                    if((otherResourceDependency.getType() & ResourceDependencyTypes.RenderTargetWrite) != 0 ||
                        (otherResourceDependency.getType() & ResourceDependencyTypes.FragmentShaderWrite) != 0 ||
                        (otherResourceDependency.getType() & ResourceDependencyTypes.ComputeShaderWrite) != 0) {
                        if(otherResourceDependency.getDependency() == resourceDependency.getDependency()) {
                            writers.add(otherPass);
                            break;
                        }
                    }
                }


            }
        }

        return writers;
    }
    private void tracePasses(LinkedList<Pass> passes, Pass thisPass) {
        for(ResourceDependency resourceDependency : thisPass.getResourceDependencies()) {

            if((resourceDependency.getType() & ResourceDependencyTypes.RenderTargetRead) != 0 ||
                    (resourceDependency.getType() & ResourceDependencyTypes.FragmentShaderRead) != 0 ||
                    (resourceDependency.getType() & ResourceDependencyTypes.ComputeShaderRead) != 0) {

                List<Pass> writers = getAllDependencyWriters(thisPass, resourceDependency);

                for(Pass writer : writers) {
                    if(!passes.contains(writer)) {
                        tracePasses(passes, writer);
                        passes.add(writer);
                    }
                    else {
                        passes.remove(writer);
                        passes.add(writer);
                    }
                }


            }

        }

    }
}
