package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class NumberToken extends Token {

    private final int value;

    public NumberToken(SourcePosition sourcePosition, int value) {
        super(sourcePosition);
        this.value = value;
    }

    @Override
    public String getLexeme() {
        return String.valueOf(this.value);
    }

    public int getValue() {
        return this.value;
    }

}
