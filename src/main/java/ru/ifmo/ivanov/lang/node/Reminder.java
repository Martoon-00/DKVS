package ru.ifmo.ivanov.lang.node;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Not thread-safe.
 */
public class Reminder {
    private final Timer timer = new Timer(true);
    private final Runnable remind;
    private TimerTask lastTask = null;
    private final long timeout;

    public Reminder(long timeout, Runnable remind) {
        this.remind = remind;
        this.timeout = timeout;
    }

    public Reminder restart() {
        if (lastTask != null)
            lastTask.cancel();

        lastTask = new TimerTask() {
            @Override
            public void run() {
                remind.run();
            }
        };
        timer.schedule(lastTask, timeout);
        return this;
    }

    public void destroy() {
        timer.cancel();
    }
}
