package ru.ifmo.ivanov.lang.misc;

import ru.ifmo.ivanov.lang.parser.ParseRule;
import ru.ifmo.ivanov.lang.parser.Parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Properties {
    public static final String DEFAULT_FILENAME = "dkvs.properties";

    private final ArrayList<InetSocketAddress> adresses = new ArrayList<>();
    private final Map<InetSocketAddress, Integer> adressToNodeNum = new HashMap<>();

    private int timeout = Integer.MAX_VALUE;

    @SuppressWarnings("FieldCanBeLocal")
    private Parser parser = new Parser()
            .addRule(new ParseRule("node *. *(\\d+) *= *([^: ]*) *: *(\\d+) *\n", f -> {
                final int index = Integer.parseInt(f.apply(1));
                while (adresses.size() <= index)
                    adresses.add(null);
                adresses.set(index, new InetSocketAddress(f.apply(2), Integer.parseInt(f.apply(3))));
            }))
            .addRule(new ParseRule("timeout *= *(\\d+) *\n", f -> timeout = Integer.parseInt(f.apply(1))));


    public Properties() throws IOException {
        this(DEFAULT_FILENAME);
    }

    public Properties(String fileName) throws IOException {
        try (InputStream in = new FileInputStream(fileName)) {
            parser.apply(in);
        }
        for (int i = 1; i < adresses.size(); i++) {
            adressToNodeNum.put(adresses.get(i), i);
        }
    }

    public InetSocketAddress getAddress(int i) {
        return adresses.get(i);
    }

    public int getNodesNum() {
        return adresses.size() - 1;
    }

    public int getTimeout() {
        return timeout;
    }

    public Iterable<InetSocketAddress> getNodesIter(){
        return adresses.subList(1, adresses.size())::iterator;
    }

    public Integer getNodeNum(InetSocketAddress address) {
        return adressToNodeNum.get(address);
    }
}
