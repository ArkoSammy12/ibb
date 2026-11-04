package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.grammar.Operator;
import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class OperatorToken extends Token {

    private final Operator operator;
    private final String lexeme;

    public OperatorToken(SourcePosition sourcePosition, Operator operator, String lexeme) {
        super(sourcePosition);
        this.operator = operator;
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public Operator getOperator() {
        return this.operator;
    }

}