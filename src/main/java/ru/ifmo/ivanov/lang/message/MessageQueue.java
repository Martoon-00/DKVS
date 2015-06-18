package ru.ifmo.ivanov.lang.message;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class MessageQueue<T extends Message> {
    private final BlockingDeque<T> queue = new LinkedBlockingDeque<>();

    public void put(T m) throws InterruptedException {
        queue.putLast(m);
    }

    public T get() throws InterruptedException {
        return queue.takeFirst();
    }

    public void clear(){
        queue.clear();
    }
}
