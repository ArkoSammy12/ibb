package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.grammar.Variable;
import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class VariableToken extends Token {

    private final Variable variable;
    private final String lexeme;

    public VariableToken(SourcePosition sourcePosition, Variable variable, String lexeme) {
        super(sourcePosition);
        this.variable = variable;
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public Variable getVariable() {
        return this.variable;
    }

}
