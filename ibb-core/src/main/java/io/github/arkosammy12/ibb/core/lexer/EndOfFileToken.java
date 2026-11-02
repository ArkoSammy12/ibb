package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class EndOfFileToken extends Token {

    public EndOfFileToken(SourcePosition sourcePosition) {
        super(sourcePosition);
    }

    @Override
    public String getLexeme() {
        return "";
    }

}
