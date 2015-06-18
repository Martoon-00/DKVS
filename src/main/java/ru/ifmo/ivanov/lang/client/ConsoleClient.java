package ru.ifmo.ivanov.lang.client;

import ru.ifmo.ivanov.lang.misc.Operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConsoleClient extends RaftClient {
    private Map<String, Integer> argsNum = new HashMap<>();

    {
        argsNum.put(Operation.PING, 0);
        argsNum.put(Operation.GET, 1);
        argsNum.put(Operation.SET, 2);
        argsNum.put(Operation.DELETE, 1);
    }

    public static void main(String[] args) throws IOException {
        new ConsoleClient(Integer.parseInt(args[0])).startSession();
    }

    public ConsoleClient(int id) throws IOException {
        super(id);
    }

    public void startSession() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        for (String line; (line = in.readLine()) != null; ) {
            String[] splited = line.split(" +");
            String command = splited[0].toLowerCase();
            String[] args = new String[splited.length - 1];
            System.arraycopy(splited, 1, args, 0, args.length);

            if (niceCommand(command, args)) {
                sendNewOperation(command, args);
            }
        }
    }

    private boolean niceCommand(String command, String[] args) {
        if (!argsNum.containsKey(command)) {
            System.err.println(String.format("Unrecognized command '%s'", command));
            return false;
        }
        if (argsNum.get(command) != args.length) {
            System.err.println(String.format("Illegal number of arguments: expected %d, specified %d.", argsNum.get(command), args.length));
            return false;
        }
        return true;
    }


    protected void onReceive(Operation operation) {
        System.out.println(String.format("\n      %s\n", operation.getResult()));
    }
}
