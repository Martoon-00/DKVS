package ru.ifmo.ivanov.lang.message;

/**
 * Created by asus on 15.05.2015.
 */
public class Message {
//    private static final String DEAD_MESSAGE = "\u0010-$-dead-$-\u0011";

    private final String message;

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return message;
    }

}
