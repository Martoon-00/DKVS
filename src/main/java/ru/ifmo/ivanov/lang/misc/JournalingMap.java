package ru.ifmo.ivanov.lang.misc;

import ru.ifmo.ivanov.lang.parser.ParseRule;
import ru.ifmo.ivanov.lang.parser.Parser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class JournalingMap {
    private final static String DEFAULT_FILENAME = "dkvs_%d.log";

    private final Map<String, String> map = new HashMap<>();

    private Writer out;

    private final Parser parser = new Parser()
            .addRule(new ParseRule("set *([^ ]+) *([^\n]+)\n*", f -> map.put(f.apply(1), f.apply(2))))
            .addRule(new ParseRule("delete *([^\n]+)\n*", f -> map.remove(f.apply(1))));

    private static final String setFormat = "set %s %s\n";
    private static final String deleteFormat = "delete %s\n";

    public JournalingMap(int nodeNumber) throws IOException {
        this(String.format(DEFAULT_FILENAME, nodeNumber));
    }

    public JournalingMap(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                parser.apply(in);
            }
        }

        out = new BufferedWriter(new FileWriter(fileName, true));
    }

    public String get(String s) {
        return map.get(s);
    }

    public void set(String k, String v) throws IOException {
        out.write(String.format(setFormat, k, v));
        out.flush();
        map.put(k, v);
    }

    public boolean delete(String k) throws IOException {
        out.write(String.format(deleteFormat, k));
        out.flush();
        return map.remove(k) != null;
    }
}
