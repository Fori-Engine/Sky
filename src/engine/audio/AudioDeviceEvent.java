package engine.audio;

public class AudioDeviceEvent {
    public static final long DeviceChanged = 1;
    public static final long SourcePlayStart = 1 << 1;
    public static final long SourcePlayEnd = 1 << 2;

    public AudioDeviceEvent(long flags) {
        setFlag(flags);
    }

    private long flags;

    public void setFlag(long flag) {
        this.flags |= flag;
    }

    public boolean hasFlag(long flag) {
        return (flags & flag) != 0;
    }

    public long getFlags() {
        return flags;
    }
}
