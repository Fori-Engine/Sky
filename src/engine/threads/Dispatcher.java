package engine.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Dispatcher {
    private List<Future> futures = new ArrayList<>();
    private ExecutorService executorService;

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void submit(Runnable runnable) {
        futures.add(executorService.submit(runnable));
    }

    public void tick() {
        futures.removeIf(Future::isDone);
    }
    public boolean isWaiting() {
        for(Future future : futures) {
            if(!future.isDone()) return true;
        }
        return false;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
