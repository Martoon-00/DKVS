package ru.ifmo.ivanov.lang.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.CommandMessage;
import ru.ifmo.ivanov.lang.message.RaftMessageFormatter;
import ru.ifmo.ivanov.lang.message.SignedCommandMessage;
import ru.ifmo.ivanov.lang.misc.Identifier;
import ru.ifmo.ivanov.lang.misc.Properties;
import ru.ifmo.ivanov.lang.web.ConnectionsManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class Client {

    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Properties properties;
    protected final ConnectionsManager manager;
    private final int id;
    private Identifier identifier;

    public Client(int id) throws IOException {
        this.id = id;
        identifier = new Identifier(String.format("\u001B[33mClient %d\u001B[m", this.id));
        this.manager = new ConnectionsManager(-1, Integer.MAX_VALUE, this::markReceived, new RaftMessageFormatter(false))
                .setIdentifier(getIdentifier())
                .start();
        properties = new Properties();
    }

    private void markReceived(SignedCommandMessage message){
        onReceive(message.getAddress(), message.getCommand(), message.getArgs());
    }

    protected abstract void onReceive(InetSocketAddress address, String command, Object... args);

    protected final void send(InetSocketAddress address, String command, Object... args){
        CommandMessage commandMessage = new CommandMessage(command, args);
        Integer nodeNum = properties.getNodeNum(address);
        if (nodeNum != null) commandMessage.setReceiverName("Node " + nodeNum);

        manager.send(address, commandMessage);
    }

    public void sendByLoopback(String command, Object... args){
        manager.sendToSelf(null, command, args);
    }

    protected Identifier getIdentifier(){
        return identifier;
    }

    public Client setIdentifier(Identifier identifier) {
        manager.setIdentifier(this.identifier = identifier);
        return this;
    }

    public int getId() {
        return id;
    }

    public abstract void startSession() throws IOException;
}
