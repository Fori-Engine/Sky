package fori.ui;

public abstract class Event {
    public long createTime;

    public Event() {
        createTime = System.currentTimeMillis();
    }
}
