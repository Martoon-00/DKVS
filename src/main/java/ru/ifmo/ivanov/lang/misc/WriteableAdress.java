package ru.ifmo.ivanov.lang.misc;

import java.net.InetSocketAddress;

public class WriteableAdress {
    private int port;
    private String adress;

    public WriteableAdress() {
    }

    public WriteableAdress(InetSocketAddress inetSocketAddress) {
        port = inetSocketAddress.getPort();
        adress = inetSocketAddress.getHostString();
    }

    public InetSocketAddress toInetSocketAdress() {
        return new InetSocketAddress(adress, port);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }
}
