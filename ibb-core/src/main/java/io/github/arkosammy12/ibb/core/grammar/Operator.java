package io.github.arkosammy12.ibb.core.grammar;

public enum Operator implements Lexeme {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE_FORWARD("/"),
    DIVIDE_BACKWARD("\\"),
    EXPONENT("^"),
    MOD("mod"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUALS(">="),
    NOT_EQUALS_CONVERGENT("><"),
    NOT_EQUALS_DIVERGENT("<>"),
    EQUALS("="),
    NOT("not"),
    AND("and"),
    OR("or");

    private final String pattern;

    Operator(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

}