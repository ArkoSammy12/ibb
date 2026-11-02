package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class NumberToken extends Token {

    private final long value;

    public NumberToken(SourcePosition sourcePosition, long value) {
        super(sourcePosition);
        this.value = value;
    }

    @Override
    public String getLexeme() {
        return String.valueOf(this.value);
    }

    public long getValue() {
        return this.value;
    }

}
