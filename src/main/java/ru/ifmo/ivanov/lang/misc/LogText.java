package ru.ifmo.ivanov.lang.misc;

public class LogText {
    private final static int WHO_WIDTH = 84;

    private final static boolean COLORED = true;

    public static String format(String who, String message) {
        if (!COLORED){
            who = who.replaceAll("\\u001B\\[\\d*m", "");
            message = message.replaceAll("\\u001B\\[\\d*m", "");
        }

        int toAdd = WHO_WIDTH - who.replaceAll("\\u001B\\[\\d*m", "").length();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < toAdd; i++) {
            s.append(" ");
        }
        return s.append(who).append(" ").append(message).toString();
    }

}
