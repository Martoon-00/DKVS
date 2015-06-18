package ru.ifmo.ivanov.lang.parser;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ParseRule {
    private final Pattern pattern;
    private final Consumer<Function<Integer, String>> groups;

    public ParseRule(String regexp, Consumer<Function<Integer, String>> groups) {
        this.pattern = Pattern.compile(regexp);
        this.groups = groups;
    }

    public ParseMatcher matcher(CharSequence s){
        return new ParseMatcher(pattern.matcher(s), groups);
    }

}
