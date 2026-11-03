package io.github.arkosammy12.ibb.core.grammar;

public enum Delimiter implements Lexeme {
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    COMMA(","),
    COLON(":");

    private final String pattern;

    Delimiter(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

}
