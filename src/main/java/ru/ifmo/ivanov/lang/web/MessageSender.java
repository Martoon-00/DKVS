package ru.ifmo.ivanov.lang.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.Message;
import ru.ifmo.ivanov.lang.message.MessageQueue;

import java.io.*;
import java.net.Socket;

public class MessageSender implements Runnable {

    private final Logger log = LogManager.getLogger(MessageSender.class);

    private final BufferedWriter out;

    private final MessageQueue<Message> queue;

    private final Runnable closeHandler;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final Socket socket;
    
    private Thread thread;

    public MessageSender(Socket socket, MessageQueue<Message> queue, Runnable closeHandler) {
        this.queue = queue;
        this.closeHandler = closeHandler;
        this.socket = socket;

        OutputStream outputStream;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            outputStream = new ByteArrayOutputStream();
        }
        out = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Message message = queue.get();
                out.write(message.getMessage() + "\n");
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (IOException e) {
            log.trace("Exception while sending a message: ", e);
            closeHandler.run();
        }
    }
    
    public MessageSender start(){
        thread = new Thread(this::run);
        thread.start();
        return this;
    }
    
    public void close(){
        thread.interrupt();
    }
}
