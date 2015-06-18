package ru.ifmo.ivanov.lang.message;

import java.io.IOException;

public class CommandMessage {
    private final String command;
    private final Object[] args;

    /**
     * For ConnectionManager to display receiver in logs
     */
    private String receiverName;

    public CommandMessage(Message message, MessageFormatter formatter) throws IOException {
        MessageFormatter.Message parsed = formatter.parse(message.getMessage());
        command = parsed.getCommand();
        args = parsed.getArgs().toArray();
    }

    public CommandMessage(String command, Object... args) {
        this.command = command;
        this.args = args;
    }

    public Message toMessage(MessageFormatter formatter) throws IOException {
        return new Message(formatter.encode(command, args));
    }

    public String getCommand() {
        return command;
    }

    public Object[] getArgs() {
        return args;
    }

    public CommandMessage setReceiverName(String receiverName) {
        this.receiverName = receiverName;
        return this;
    }

    public String getReceiverName() {
        return receiverName;
    }

}
