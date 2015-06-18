package ru.ifmo.ivanov.lang.message;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SignedCommandMessage extends CommandMessage {
    private final InetSocketAddress address;

    public SignedCommandMessage(SignedMessage message, MessageFormatter formatter) throws IOException {
        super(message, formatter);
        this.address = message.getAddress();
    }

    public SignedCommandMessage(InetSocketAddress address, String command, Object... args) {
        super(command, args);
        this.address = address;
    }

    @Override
    public SignedMessage toMessage(MessageFormatter formatter) throws IOException {
        return new SignedMessage(super.toMessage(formatter).getMessage(), address);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

}
