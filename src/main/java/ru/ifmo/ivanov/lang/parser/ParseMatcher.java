package ru.ifmo.ivanov.lang.parser;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;

public class ParseMatcher {
    private final Matcher matcher;
    private final Consumer<Function<Integer, String>> groups;

    public ParseMatcher(Matcher matcher, Consumer<Function<Integer, String>> groups) {
        this.matcher = matcher;
        this.groups = groups;
    }

    public boolean matches(){
        return matcher.matches();
    }

    public Function<Integer, String> groups() {
        return matcher::group;
    }

    public boolean lookingAt(){
        return matcher.lookingAt();
    }

    public void apply(){
        groups.accept(matcher::group);
    }
}
