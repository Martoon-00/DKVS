package ru.ifmo.ivanov.lang.judge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.misc.LogItem;
import ru.ifmo.ivanov.lang.misc.LogText;
import ru.ifmo.ivanov.lang.misc.Operation;
import ru.ifmo.ivanov.lang.misc.Properties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class Judge {
    private final Logger log = LogManager.getLogger(Judge.class);

    public final static boolean ACTIVE = false;

    public static Properties properties;

    static {
        try {
            properties = new Properties(Properties.DEFAULT_FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final static double SLEEP_PROBABILITY = 0.005;
    public final static Function<Integer, Integer> SLEEP_MAX_DURATION = k -> 2 * k;

    public final static long MAX_OPERATION_DELAY = 15000;

    private static Map<Integer, Integer> leadersOnTerms = new ConcurrentHashMap<>();

    private static Map<Operation, Long> operationsStartTime = new ConcurrentHashMap<>();

    private static Map<Integer, Commit> commits = new ConcurrentHashMap<>();

    private void error(String text) {
        log.error(LogText.format("\u001B[34mJudge\u001B[m", String.format("\u001B[31m%s\u001B[m", text)));
    }

    private void warn(String text) {
        log.warn(LogText.format("\u001B[34mJudge\u001B[m", String.format("\u001B[33m%s\u001B[m", text)));
    }


    public boolean checkNeedRest(Consumer<String> info, String comment) {
        if (!ACTIVE)
            return false;

        if (Math.random() < Judge.SLEEP_PROBABILITY) {
            int sleepTime = new Random().nextInt(Judge.SLEEP_MAX_DURATION.apply(properties.getTimeout()));
            info.accept(String.format("^ Go to sleep on %d ms %s", sleepTime, comment));
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            info.accept("^ Awakened");
            return true;
        }
        return false;
    }


    public void registerLeader(int term, int nodeNum) {
        if (!ACTIVE)
            return;

        if (leadersOnTerms.containsKey(term)) {
            error(String.format("Another leader on term %d; was %d, now %d", term, leadersOnTerms.get(term), nodeNum));
        } else {
            leadersOnTerms.put(term, nodeNum);
        }
    }


    static {
        if (ACTIVE) {
            new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(10000);
                        for (Map.Entry<Operation, Long> operationLongEntry : operationsStartTime.entrySet()) {
                            long delay = System.currentTimeMillis() - operationLongEntry.getValue();
                            if (delay > MAX_OPERATION_DELAY) {
                                new Judge().warn(String.format("Delayed operation %s %s %s (%d ms.)", operationLongEntry.getKey().getOpId(), operationLongEntry.getKey().getCommand(),
                                        Arrays.toString(operationLongEntry.getKey().getArgs()), delay));
                            }
                        }

                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }).start();
        }
    }

    public void registerOperation(Operation op) {
        if (!ACTIVE)
            return;

        if (operationsStartTime.containsKey(op))
            error(String.format("Operation with id %s registered again", op.getOpId()));
        operationsStartTime.put(op, System.currentTimeMillis());
    }

    public void closeOperation(Operation op) {
        if (!ACTIVE)
            return;

        if (operationsStartTime.remove(op) == null) {
            error(String.format("Closed operation with id %s doesn't exist", op.getOpId()));
        }
    }

    public void registerCommit(LogItem item, int nodeId) {
        if (!ACTIVE)
            return;

        Commit lastCommit = commits.get(item.getIndex());
        if (lastCommit == null) {
            commits.put(item.getIndex(), new Commit(item, nodeId));
            return;
        }
        if (!lastCommit.item.getOperation().equals(item.getOperation())) {
            error(String.format("Different commits at same index. Was [%s] by Node %d, now [%s] by Node %d", lastCommit.item.getOperation(), lastCommit.nodeId, item.getOperation(), nodeId));
        }
    }

    private class Commit {
        private final LogItem item;
        private final int nodeId;

        public Commit(LogItem item, int nodeId) {
            this.item = item;
            this.nodeId = nodeId;
        }
    }
}
