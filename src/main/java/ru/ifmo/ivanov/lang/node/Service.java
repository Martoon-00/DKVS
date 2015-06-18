package ru.ifmo.ivanov.lang.node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.ivanov.lang.message.CommandMessage;
import ru.ifmo.ivanov.lang.message.RaftMessageFormatter;
import ru.ifmo.ivanov.lang.message.SignedCommandMessage;
import ru.ifmo.ivanov.lang.misc.ColoredText;
import ru.ifmo.ivanov.lang.misc.Identifier;
import ru.ifmo.ivanov.lang.misc.LogText;
import ru.ifmo.ivanov.lang.misc.Properties;
import ru.ifmo.ivanov.lang.web.ConnectionsManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Function;


public abstract class Service {
    protected final Logger log = LogManager.getLogger(Service.class);

    protected static final Function<Integer, String> createIdentifier = k -> String.format("Node %1d", k);

    protected final Properties properties;

    private final ConnectionsManager connections;

    private Identifier identifier;

    private int ordinal;

    public Service(int ordinal) throws IOException {
        if (ordinal < 1)
            throw new IllegalArgumentException("Ordinal must be greater than zero");

        try {
            properties = new Properties();
        } catch (IOException e) {
            throw new IOException(String.format("Properties file not found at '%s'", Properties.DEFAULT_FILENAME));
        }

        this.ordinal = ordinal;
        this.identifier = new Identifier(createIdentifier.apply(ordinal) + "  ");
        connections = new ConnectionsManager(properties.getAddress(ordinal).getPort(), properties.getTimeout(), this::processMessage, new RaftMessageFormatter(false))
                .setIdentifier(identifier);
    }

    public final void start() throws IOException {
        connections.start();
        init();
    }

    protected abstract void init();

    /**
     * Launched in a single thread
     */
    private void processMessage(SignedCommandMessage message) {
        if (connections.isClosed())
            return;
        CommandMessage commandMessage = onMessage(message.getAddress(), message.getCommand(), message.getArgs());
        if (commandMessage != null) {
            if (commandMessage.getReceiverName() == null)
                send(message.getAddress(), commandMessage.getCommand(), commandMessage.getArgs());
            else
                sendTo(message.getAddress(), commandMessage.getReceiverName(), commandMessage.getCommand(), commandMessage.getArgs());
        }
    }

    protected final void send(InetSocketAddress address, String command, Object... args){
        connections.send(address, new CommandMessage(command, args));
    }

    protected final void sendTo(InetSocketAddress address, String receiversName, String command, Object... args){
        connections.send(address, new CommandMessage(command, args).setReceiverName(receiversName));
    }

    protected abstract CommandMessage onMessage(InetSocketAddress address, String command, Object... args);

    public int getOrdinal() {
        return ordinal;
    }

    public void sendByLoopback(InetSocketAddress from, String command, Object[] args) {
        connections.sendToSelf(from, command, args);
    }

    protected void info(String s) {
        log.info(LogText.format(identifier.get(), ColoredText.recognize(s)));
    }
    protected void error(String s) {
        log.error(LogText.format(identifier.get(), ColoredText.format(s, ColoredText.Format.STRANGE)));
    }

    protected Service setIdentifier(Identifier identifier) {
        this.identifier = identifier;
        connections.setIdentifier(identifier);
        return this;
    }

    public void close(){
        connections.close();
    }

}
