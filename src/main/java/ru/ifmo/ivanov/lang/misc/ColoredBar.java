package ru.ifmo.ivanov.lang.misc;

import java.util.function.Function;

public class ColoredBar {
    private char fill = '*';
    private char empty = ' ';
    private boolean direction;
    private final int width;
    private final Function<Double, Double> f;
    private final Integer[] colors;

    public ColoredBar(int width, Function<Double, Double> f, Integer... colors) {
        this.width = width;
        this.f = f;
        this.colors = colors;
    }

    public ColoredBar setFill(char fill) {
        this.fill = fill;
        return this;
    }

    public ColoredBar setEmpty(char empty) {
        this.empty = empty;
        return this;
    }

    public ColoredBar setDirection(boolean right) {
        this.direction = right;
        return this;
    }

    public String get(int k) {
        StringBuilder s = new StringBuilder();
        k = Math.max(0, Math.min(width, k));
        for (int j = width; j > 0; j--) {
            int i = direction ? width + 1 - j : j;
            if (i <= k)
                s.append(paint(String.valueOf(fill), i));
            else
                s.append(empty);
        }
        return s.toString();
    }

    public String paint(String s, int k) {
        int colorIndex = (int) Math.floor(f.apply((double) (k - 1) / (width - 1)) * colors.length);
        colorIndex = Math.max(0, Math.min(colors.length - 1, colorIndex));
        return String.format("\u001B[%dm", colors[colorIndex] + 30) + s + "\u001B[m";
    }

}
