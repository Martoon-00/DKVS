package ru.ifmo.ivanov.lang.misc;

import java.util.function.Supplier;

public class Identifier {
    public static final String DEFAULT = "\u001B[31m???\u001B[m";

    private final Supplier<String> identifierProducer;

    public Identifier(String identifier) {
        identifierProducer = () -> identifier;
    }

    public Identifier(Supplier<String> identifierProducer) {
        this.identifierProducer = identifierProducer;
    }

    public String get() {
        return identifierProducer.get();
    }

    @Override
    public String toString() {
        return get();
    }

}
