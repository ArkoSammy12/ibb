package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class ArraySigilToken extends Token  {

    public ArraySigilToken(SourcePosition sourcePosition) {
        super(sourcePosition);
    }

    @Override
    public String getLexeme() {
        return "@";
    }

}
