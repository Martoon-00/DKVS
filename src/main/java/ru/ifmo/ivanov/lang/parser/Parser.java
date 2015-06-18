package ru.ifmo.ivanov.lang.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Parser {
    private final ArrayList<ParseRule> rules = new ArrayList<>();

    public Parser addRule(ParseRule parseRule) {
        rules.add(parseRule);
        return this;
    }

    /**
     * Applies parser to string and does in action if the whole string matches regexp.
     * @param s
     */
    public boolean apply(String s){
        return rules.stream().map(rule -> rule.matcher(s)).filter(ParseMatcher::matches).limit(1).peek(ParseMatcher::apply).count() == 1;
    }

    /**
     * Applies parser to stream, extracting substring satisfying specified regexp.
     * If a some part of text doesn't fit to regexp, it and remaining part till newline would be skipped.
     * @param inputStream
     * @throws IOException
     */
    public void apply(InputStream inputStream) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        final StringBuilder sb = new StringBuilder();

        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line).append("\n");
            final ParseMatcher[] candidates = rules.stream().map(rule -> rule.matcher(sb)).filter(ParseMatcher::lookingAt).toArray(ParseMatcher[]::new);
            if (candidates.length == 1) {
                final ParseMatcher candidate = candidates[0];
                candidate.apply();
                sb.delete(0, candidate.groups().apply(0).length());
            } else if (candidates.length == 0) {
                sb.delete(0, sb.length());
            }

        }
    }
}
