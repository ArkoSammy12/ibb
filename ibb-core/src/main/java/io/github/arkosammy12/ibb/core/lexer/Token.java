package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public sealed abstract class Token permits DelimiterToken, EndOfFileToken, KeywordToken, NumberToken, OperatorToken, StringToken, VariableToken {

    private final SourcePosition sourcePosition;

    public Token(SourcePosition sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    public SourcePosition getSourcePosition() {
        return this.sourcePosition;
    }

    abstract public String getLexeme();

}
