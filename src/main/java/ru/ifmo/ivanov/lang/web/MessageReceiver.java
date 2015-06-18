package ru.ifmo.ivanov.lang.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.MessageQueue;
import ru.ifmo.ivanov.lang.message.SignedMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MessageReceiver implements Runnable {
    private final Logger log = LogManager.getLogger(MessageReceiver.class);

    private final Socket socket;
    private final BufferedReader reader;

    private final MessageQueue<SignedMessage> queue;

    private final Runnable closeHandler;

    private Thread thread;

    public MessageReceiver(Socket socket, MessageQueue<SignedMessage> queue, Runnable closeHandler) {
        this.socket = socket;
        this.queue = queue;
        this.closeHandler = closeHandler;

        InputStream inputStream;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            closeHandler.run();
            inputStream = new ByteArrayInputStream(new byte[0]);
        }
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                final String message = reader.readLine();
                if (message == null) break;

                queue.put(new SignedMessage(message, new InetSocketAddress(socket.getInetAddress(), socket.getPort())));
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (IOException e) {
            log.trace("Exception while receiving a message: ", e);
            closeHandler.run();
        }
    }

    public MessageReceiver start() {
        thread = new Thread(this::run);
        thread.start();
        return this;
    }

    public void close() {
        thread.interrupt();
    }
}
