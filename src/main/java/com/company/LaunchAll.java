package com.company;

import ru.ifmo.ivanov.lang.client.AutoClient;
import ru.ifmo.ivanov.lang.misc.PersistentState;
import ru.ifmo.ivanov.lang.node.RaftNode;

import java.io.IOException;

public class LaunchAll {

    public static void main(String[] args) throws IOException {
//        System.out.println(("�������".hashCode() & 0x7fffffff) % 3 + 1);

        PersistentState.deleteAllDefault();

        new RaftNode(1).start();
        new RaftNode(2).start();
        new RaftNode(3).start();

        new AutoClient(1).startSession();
        new AutoClient(2).startSession();

    }

}
