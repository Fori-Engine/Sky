package engine.graphics.pipelines;

public abstract class Features {
    protected boolean mandatory;

    protected Features(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
