package ru.ifmo.ivanov.lang.message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class MessageFormatter {
    private final Logger log = LogManager.getLogger(MessageFormatter.class);

    private final ArrayList<Format> formats = new ArrayList<>();

    private final boolean pretty;

    public MessageFormatter(boolean pretty) {
        this.pretty = pretty;
    }

    public MessageFormatter() {
        this(false);
    }

    public MessageFormatter addFormat(Format format){
        formats.add(format);
        return this;
    }

    public Message parse(String message) throws IOException {
        for (Format format : formats) {
            if (message.startsWith(format.command)){
                    ArrayList<Object> ans = new ArrayList<>();
                    JsonParser jsonParser = new JsonFactory().createJsonParser(message.substring(format.command.length()));
                    ObjectMapper objectMapper = new ObjectMapper();
                    for (Class<?> arg : format.args) {
                        ans.add(objectMapper.readValue(jsonParser, arg));
                    }
                    return new Message(format.command, ans);
            }
        }
        throw new IOException("No viable alternative for command");
    }

    public String encode(String command, Object... item) throws IOException {
        for (Format format : formats) {
            if (command.startsWith(format.command)) {
                if (format.args.size() != item.length)
                    throw new IllegalArgumentException(String.format("Illegal number of arguments in '%s'. Expected %d, gained %d.", command, format.args.size(), item.length));

                ObjectMapper objectMapper = new ObjectMapper();
                ObjectWriter writer = pretty ? objectMapper.defaultPrettyPrintingWriter() : objectMapper.writer();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(command.getBytes());
                ArrayList<Class<?>> args = format.args;
                for (int i = 0; i < args.size(); i++) {
                    if (item[i] != null && args.get(i) != item[i].getClass()) {
                        throw new IllegalArgumentException(String.format("Illegal %d argument in '%s': expected '%s', gained '%s'", i, command, args.get(i).getName(), item[i].getClass().getName()));
                    }
                    out.write(pretty ? '\n' : ' ');
                    writer.writeValue(out, item[i]);
                }
                return new String(out.toByteArray());
            }
        }
        throw new IllegalArgumentException(String.format("Command '%s' doesn't exist", command));
    }

    public static class Format {
        private final String command;
        private final ArrayList<Class<?>> args;

        public Format(String command, Class<?>... args) {
            this.command = command;
            this.args = new ArrayList<>(Arrays.asList(args));
        }
    }

    public static class Message {
        private final String command;
        private final ArrayList<Object> args;

        public Message(String command, ArrayList<Object> args) {
            this.command = command;
            this.args = args;
        }

        public String getCommand() {
            return command;
        }

        public ArrayList<Object> getArgs() {
            return args;
        }
    }
}
