package ru.ifmo.ivanov.lang.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.Message;
import ru.ifmo.ivanov.lang.message.MessageQueue;
import ru.ifmo.ivanov.lang.message.SignedMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketConnectionManager {

    private final Logger log = LogManager.getLogger(SocketConnectionManager.class);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final Socket socket;

    private final InetSocketAddress address;
    private int timeout;

    private final MessageQueue<SignedMessage> inputQueue;
    private final MessageQueue<Message> outputQueue = new MessageQueue<>();

    private final Runnable onCloseAction;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    private MessageSender messageSender;
    private MessageReceiver messageReceiver;

    public SocketConnectionManager(Socket socket, MessageQueue<SignedMessage> inputQueue, Runnable onCloseAction) {
        this.socket = socket;
        this.address = null;
        this.inputQueue = inputQueue;
        this.onCloseAction = onCloseAction;
    }

    public SocketConnectionManager(InetSocketAddress address, int timeout, MessageQueue<SignedMessage> inputQueue, Runnable onCloseAction) {
        this.socket = new Socket();
        this.address = address;
        this.timeout = timeout;
        this.inputQueue = inputQueue;
        this.onCloseAction = onCloseAction;
    }

    private void run() {
        if (address != null) {
            try {
                socket.connect(address, timeout);
            } catch (IOException e) {
                log.trace("Connection failed", e);
                close();
            }
        }

        messageReceiver = new MessageReceiver(socket, inputQueue, this::close).start();
        messageSender = new MessageSender(socket, outputQueue, this::close).start();
    }

    public SocketConnectionManager start() {
        if (started.compareAndSet(false, true)) {
            executor.submit(this::run);
        }
        return this;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (messageSender != null)
                messageSender.close();
            if (messageReceiver != null)
                messageReceiver.close();
            try {
                socket.close();
            } catch (Throwable e) {
                log.trace("Problems while closing a socket.", e);
            }
            executor.submit(onCloseAction::run);
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void send(Message message) {
        try {
            outputQueue.put(message);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}
