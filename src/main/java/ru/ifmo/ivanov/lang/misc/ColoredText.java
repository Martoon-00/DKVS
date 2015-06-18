package ru.ifmo.ivanov.lang.misc;

public class ColoredText {

    public static String format(String message, Format format) {
        return String.format("\u001B[%dm%s%s\u001B[m", format.color, format.prefix, message);
    }

    public static String recognize(String message){
        for (Format format : Format.values()) {
            if (message.startsWith(format.prefix)) {
                return String.format("\u001B[%dm%s\u001B[m", format.color, message);
            }
        }
        return message;
    }


    public enum Format {
        INFORM(34, "! "),
        REMIND(35, "&"),
        NEW_STATE(32, "$ "),
        RESPONSE(33, "@"),
        STRANGE(31, "? "),
        SLEEP(37, "^ "),
        CLI_RESPONSE(36, "#"),
        NONE(34, "");

        private final int color;
        private final String prefix;

        Format(int color, String prefix) {
            this.color = color;
            this.prefix = prefix;
        }
    }

}