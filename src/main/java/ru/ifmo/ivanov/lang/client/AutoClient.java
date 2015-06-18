package ru.ifmo.ivanov.lang.client;

import ru.ifmo.ivanov.lang.misc.Operation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class AutoClient extends RaftClient {

    private final Random random = new Random();
    private final int maxOperationDelayTime;

    public AutoClient(int id, int maxOperationDelayTime) throws IOException {
        super(id);
        this.maxOperationDelayTime = maxOperationDelayTime;
    }

    public AutoClient(int id) throws IOException {
        this(id, 3000);
    }

    public static void main(String[] args) throws IOException {
        new AutoClient(Integer.parseInt(args[0])).startSession();
    }


    @Override
    public void startSession() {
        Supplier<String> argGen = () -> String.valueOf(random.nextInt(10));
        List<Runnable> operations = Arrays.asList(
                () -> sendNewOperation(Operation.PING),
                () -> sendNewOperation(Operation.DELETE, argGen.get()),
                () -> sendNewOperation(Operation.SET, argGen.get(), argGen.get()),
                () -> sendNewOperation(Operation.GET, argGen.get())
        );

        try {
            while (!Thread.interrupted()) {
                operations.get(random.nextInt(operations.size())).run();
                Thread.sleep(random.nextInt(maxOperationDelayTime));
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    @Override
    protected void onReceive(Operation op) {
    }
}
