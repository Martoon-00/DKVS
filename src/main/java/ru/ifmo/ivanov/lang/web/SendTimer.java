package ru.ifmo.ivanov.lang.web;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SendTimer {
    private final Timer timer = new Timer(true);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<InetSocketAddress, TimerTask> tasks = new HashMap<>();

    private final Consumer<InetSocketAddress> onZero;

    public SendTimer(Consumer<InetSocketAddress> onZero) {
        this.onZero = onZero;
    }

    public void start(InetSocketAddress address, long timeout){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                onZero.accept(address);
            }
        };
        tasks.compute(address, (addr, oldTask) -> {
            if (oldTask != null)
                oldTask.cancel();
            return task;
        });
        timer.schedule(task, timeout);
    }

}
