package io.github.arkosammy12.ibb.core.grammar;

public enum Keyword implements Lexeme {
    PRINT("print"),
    IF("if"),
    THEN("then"),
    GOTO("goto"),
    INPUT("input"),
    LET("let"),
    GOSUB("gosub"),
    RETURN("return"),
    END("end");

    private final String pattern;

    Keyword(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

}
