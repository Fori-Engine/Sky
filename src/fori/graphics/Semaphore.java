package fori.graphics;

public abstract class Semaphore extends Disposable {
    public Semaphore(Disposable parent) {
        super(parent);
    }

    public abstract void dispose();
}
