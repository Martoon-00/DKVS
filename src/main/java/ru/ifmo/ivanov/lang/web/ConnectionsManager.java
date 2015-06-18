package ru.ifmo.ivanov.lang.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.*;
import ru.ifmo.ivanov.lang.misc.ColoredText;
import ru.ifmo.ivanov.lang.misc.Identifier;
import ru.ifmo.ivanov.lang.misc.LogText;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ConnectionsManager {
    private final Logger log = LogManager.getLogger(ConnectionsManager.class);

    private AtomicBoolean started = new AtomicBoolean(false);

    private final MessageQueue<SignedMessage> inputQueue = new MessageQueue<>();

    private final Map<InetSocketAddress, SocketConnectionManager> inputConnections = new HashMap<>();
    private final Map<InetSocketAddress, SocketConnectionManager> outputConnections = new HashMap<>();
    private ConnectionReceiver connectionReceiver;


    private boolean closed = false;

    private final int port;

    private final long timeout;

    private final Consumer<SignedCommandMessage> onMessage;

    private final MessageFormatter formatter;

    private Identifier identifier = null;

    public ConnectionsManager(int port, int timeout, Consumer<SignedCommandMessage> onMessage, MessageFormatter formatter) {
        this.port = port;
        this.timeout = timeout;
        this.onMessage = onMessage;
        this.formatter = formatter;
    }

    public ConnectionsManager start() throws IOException {
        if (!started.compareAndSet(false, true)) return this;

        if (port >= 0) {
            connectionReceiver = new ConnectionReceiver(port, socket -> {
                final InetSocketAddress address = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
                final SocketConnectionManager manager = new SocketConnectionManager(socket, inputQueue, () -> {
                });
                inputConnections.compute(address, (addr, curManager) -> {
                    if (curManager != null) {
                        curManager.close();
                    }
                    return manager;
                });
                manager.start();
            }).start();
        }

        new Thread(() -> {
            while (!closed && !Thread.interrupted()) {
                try {
                    SignedMessage message = inputQueue.get();
                        log.info(LogText.format(identifier == null ? Identifier.DEFAULT : identifier.get(), String.format("<---<---<--".replaceAll("<", "\u001B[33m<\u001B[m") + "  %10s" + "<%s>".replaceAll("(<|>)", "\u001B[32m$1\u001B[m"), "", ColoredText.recognize(message.getMessage()))));
                    try {
                        onMessage.accept(new SignedCommandMessage(message, formatter));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    break;
                }
            }
        }).start();
        return this;
    }

    public void send(InetSocketAddress address, CommandMessage commandMessage) {
        Message message;
        try {
            message = commandMessage.toMessage(formatter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getConnection(address, (int) timeout).send(message);
            String receiver = commandMessage.getReceiverName() == null ? "???" : commandMessage.getReceiverName() + ": ";
            log.info(LogText.format(identifier == null ? Identifier.DEFAULT : identifier.get(), String.format("-->--->--->".replaceAll(">", "\u001B[34m>\u001B[m") + "  %s%" + (10 - receiver.length()) + "s" + "<%s>".replaceAll("(<|>)", "\u001B[32m$1\u001B[m"), receiver, "", ColoredText.recognize(message.getMessage()))));
    }

    private SocketConnectionManager getConnection(InetSocketAddress address, int timeout) {
        SocketConnectionManager result = inputConnections.get(address);
        if (result != null) return result;

        outputConnections.computeIfPresent(address, (addr, conn) -> conn.isClosed() ? null : conn);
        return outputConnections.computeIfAbsent(address, addr -> new SocketConnectionManager(address, timeout, inputQueue, () -> {
            /*try {
                  inputQueue.put(new SignedMessage(null, address));
            } catch (InterruptedException e) {
                  Thread.interrupted();
            } */
        })).start();
    }

    public ConnectionsManager setIdentifier(Identifier identifier) {
        this.identifier = identifier;
        return this;
    }

    public void sendToSelf(InetSocketAddress from, String command, Object... args) {
        try {
            inputQueue.put(new SignedCommandMessage(from, command, args).toMessage(formatter));
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing a command", e);
        }
    }

    public void close() {
        if (!closed) {
            closed = true;
            connectionReceiver.close();
            inputConnections.values().forEach(SocketConnectionManager::close);
            outputConnections.values().forEach(SocketConnectionManager::close);
            inputQueue.clear();
            // no every message will be removed, but it's not required
        }

    }

    public boolean isClosed() {
        return closed;
    }
}
