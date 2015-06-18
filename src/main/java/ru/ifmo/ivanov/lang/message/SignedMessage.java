package ru.ifmo.ivanov.lang.message;

import com.sun.istack.internal.Nullable;

import java.net.InetSocketAddress;

public class SignedMessage extends Message {
    private final InetSocketAddress address;

    public SignedMessage(@Nullable String message, InetSocketAddress address) {
        super(message);
        this.address = address;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
