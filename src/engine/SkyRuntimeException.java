package engine;

public class SkyRuntimeException extends RuntimeException {
    public SkyRuntimeException(String msg, Exception e) {
        super(msg);
        Logger.meltdown(SkyRuntimeException.class, msg + "\n" + ExceptionUtil.exceptionToString(e));
    }
    public SkyRuntimeException(Exception e) {
        super("A fatal error occurred and the engine exited");
        Logger.meltdown(SkyRuntimeException.class, "\n" + ExceptionUtil.exceptionToString(e));
    }
    public SkyRuntimeException(String msg) {
        super(msg);
        Logger.meltdown(SkyRuntimeException.class, "\n" + ExceptionUtil.exceptionToString(this));
    }
}
