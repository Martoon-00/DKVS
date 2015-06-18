package ru.ifmo.ivanov.lang.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.judge.Judge;
import ru.ifmo.ivanov.lang.message.RaftMessageFormatter;
import ru.ifmo.ivanov.lang.misc.*;
import ru.ifmo.ivanov.lang.node.Reminder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RaftClient {
    private final Logger log = LogManager.getLogger(RaftClient.class);

    public static final int RESEND_TIMEOUT = 5000;

    private Client client;

    private int lastKnownLeader = 1;
    private int opId = 0;

    private Map<Integer, Reminder> reminders = new ConcurrentHashMap<>();

    private static ColoredBar bar = new ColoredBar(20, k -> (3 + Math.log(k) / Math.log(2)) / 4, 2, 3, 1, 5).setFill('*').setEmpty('^');

    public RaftClient(int id) throws IOException {
        client = new MegaClient(id)
                .setIdentifier(new Identifier(() ->
                        String.format("[waiting ops: %2s %s] \u001B[33mClient %d\u001B[m",
                                bar.paint(String.valueOf(reminders.size()), reminders.size()),
                                bar.get(reminders.size()),
                                id
                        )
                ));

    }

    public abstract void startSession() throws IOException;

    protected abstract void onReceive(Operation result);

    protected void sendNewOperation(String command, String... args) {
        Operation operation = new Operation(new OperationId(client.getId(), opId++), null, command, args);
        info(String.format("! Request \"%s\"", operation));
        new Judge().registerOperation(operation);
        send(operation);
    }

    private void send(Operation operation) {
        client.send(client.properties.getAddress(lastKnownLeader), RaftMessageFormatter.APPLY_OPERATION, operation);
        rememberToRequestAgain(operation, true);
    }

    private void rememberToRequestAgain(Operation op, boolean tryAnotherLeader) {
        if (tryAnotherLeader)
            lastKnownLeader = 1;
        reminders.computeIfAbsent(op.getOpId().getId(), arg -> new Reminder(RESEND_TIMEOUT, () -> client.sendByLoopback(RaftMessageFormatter.REMIND_REQUESTED_APPLYING, op))).restart();
    }

    protected void info(String s) {
        log.info(LogText.format(client.getIdentifier().get(), ColoredText.recognize(s)));
    }


    private class MegaClient extends Client {

        public MegaClient(int id) throws IOException {
            super(id);
        }

        @Override
        protected void onReceive(InetSocketAddress address, String command, Object... args) {
            if (command.equals(RaftMessageFormatter.APPLY_OPERATION_RESPONSE)) {
                Operation op = (Operation) args[1];

                if (op.getResult() == null) {
                    if (args[0] != null && (int) args[0] != -1 && (int)args[0] != lastKnownLeader) {
                        lastKnownLeader = (int) args[0];
                        RaftClient.this.send(op);
                    }
                    else
                        rememberToRequestAgain(op, false);
                } else {
                    Reminder reminder = reminders.remove(op.getOpId().getId());
                    if (reminder != null) { // else is not first time received this response
                        reminder.destroy();
                        new Judge().closeOperation(op);
                        info(String.format("# Response \"%s\" [%s]", op.getResult(), op));
                        RaftClient.this.onReceive(op);
                    }
                }
            } else if (command.equals(RaftMessageFormatter.REMIND_REQUESTED_APPLYING)) {
                RaftClient.this.send((Operation) args[0]);
            }
        }

        @Override
        public void startSession() throws IOException {
            RaftClient.this.startSession();
        }
    }
}
