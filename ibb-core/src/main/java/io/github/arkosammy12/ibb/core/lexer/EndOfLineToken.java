package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class EndOfLineToken extends Token {

    public EndOfLineToken(SourcePosition sourcePosition) {
        super(sourcePosition);
    }

    @Override
    public String getLexeme() {
        return "End-of-Line";
    }

}