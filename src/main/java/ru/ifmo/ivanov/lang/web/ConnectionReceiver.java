package ru.ifmo.ivanov.lang.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ConnectionReceiver implements Runnable {
    private final Logger log = LogManager.getLogger(ConnectionReceiver.class);

    private ServerSocket serverSocket;

    private final int port;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Consumer<Socket> onSocketGained;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public ConnectionReceiver(int port, Consumer<Socket> onSocket) {
        this.onSocketGained = onSocket;
        this.port = port;
        createServerSocket();
    }

    private boolean createServerSocket() {
        try {
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            log.warn("Cannot create listening socket: ", e);
            return false;
        }
    }

    @Override
    public void run() {
        while (!isClosed.get()) {
            try {
                if (serverSocket == null){
                    createSocketWithDelay(1000);
                    break;
                }
                final Socket socket = serverSocket.accept();
                executor.submit(() -> giveSocket(socket));
            } catch (IOException e) {
                log.trace("Exception while accepting socket... ", e);
                createSocketWithDelay(1000);
            }
        }
    }

    private void createSocketWithDelay(int delay) {
        try {
            serverSocket.close();
        } catch (IOException e1) {
            // ignore
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        createServerSocket();
    }

    private void giveSocket(Socket socket) {
        onSocketGained.accept(socket);
    }

    public ConnectionReceiver start() {
        executor.submit(this);
        return this;
    }

    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                if (serverSocket != null){
                    serverSocket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while closing server socket", e);
            }
        }
    }
}
