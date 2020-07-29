package top.canyie.dreamland.manager.utils;


/**
 * @author canyie
 */
public class ForegroundThread extends Thread {
    protected ForegroundThread(String name) {
        super(name);
    }
    public ForegroundThread(String name, Runnable action) {
        super(action, name);
    }

    @Override public final void run() {
        if (isDaemon()) {
            throw new RuntimeException("Foreground thread " + this + " cannot be a daemon thread.");
        }
        Threads.setForeground();
        execute();
    }

    protected void execute() {
        super.run();
    }
}
