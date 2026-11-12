package io.github.arkosammy12.ibb.core.grammar;

public enum Keyword implements Lexeme {
    PRINT("print"),
    IF("if"),
    THEN("then"),
    FOR("for"),
    TO("to"),
    STEP("step"),
    NEXT("next"),
    GOTO("goto"),
    INPUT("input"),
    LET("let"),
    GOSUB("gosub"),
    RETURN("return"),
    END("end"),
    ABS("abs"),
    ASC("asc"),
    RND("rnd"),
    SGN("sgn"),
    REM("rem");

    private final String pattern;

    Keyword(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

}