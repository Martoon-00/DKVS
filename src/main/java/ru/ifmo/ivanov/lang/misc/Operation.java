package ru.ifmo.ivanov.lang.misc;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class Operation {
    public static final String PING = "ping";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String DELETE = "delete";

    String command;
    String[] args;
    OperationResult result;
    OperationId opId;
    WriteableAdress author;

    public Operation() {
    }

    public Operation(OperationId opId, InetSocketAddress author, String command, String... args) {
        this.command = command;
        this.args = args;
        this.opId = opId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public OperationId getOpId() {
        return opId;
    }

    public void setOpId(OperationId opId) {
        this.opId = opId;
    }

    public WriteableAdress getAuthor() {
        return author;
    }

    public void setAuthor(WriteableAdress author) {
        this.author = author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operation)) return false;

        Operation operation = (Operation) o;

        return opId.equals(operation.opId);

    }

    @Override
    public int hashCode() {
        return opId.hashCode();
    }

    @Override
    public String toString() {
        return command + Arrays.stream(args).reduce("", (a, b) -> a + " " + b);
    }
}
