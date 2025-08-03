package fori.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RenderGraph extends Disposable {

    private List<Pass> passes = new ArrayList<>();
    private Pass presenting;

    public RenderGraph(Disposable parent) {
        super(parent);
    }

    public void present(Pass pass) {
        if(pass.isRoot) {
            presenting = pass;
        }
        else throw new RuntimeException("Pass is not a root pass");
    }

    public Pass getPresenting() {
        return presenting;
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
}
