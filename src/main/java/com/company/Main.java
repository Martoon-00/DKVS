package com.company;

import ru.ifmo.ivanov.lang.client.AutoClient;
import ru.ifmo.ivanov.lang.client.ConsoleClient;
import ru.ifmo.ivanov.lang.misc.PersistentState;
import ru.ifmo.ivanov.lang.node.RaftNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String[] command = in.readLine().split("\\s+");
        switch (command[0]) {
            case "dkvs_node":
                new RaftNode((Integer.parseInt(command[1]))).start();
                break;
            case "client_auto":
                if (command.length > 2)
                    new AutoClient(Integer.parseInt(command[1]), Integer.parseInt(command[2])).startSession();
                else
                    new AutoClient(Integer.parseInt(command[1])).startSession();
                break;
            case "client_console":
                new ConsoleClient(Integer.parseInt(command[1])).startSession();
                break;
            case "clear":
                PersistentState.deleteAllDefault();
                break;
            default:
                System.err.println("Unrecognised command");
        }
    }
}
