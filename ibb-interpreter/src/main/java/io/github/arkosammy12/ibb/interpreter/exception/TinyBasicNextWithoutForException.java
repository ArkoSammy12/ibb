package io.github.arkosammy12.ibb.interpreter.exception;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public final class TinyBasicNextWithoutForException extends TinyBasicInterpreterException {

    private final Variable variable;

    public TinyBasicNextWithoutForException(Integer lineNumber, int statementIndex, Variable variable) {
        super(lineNumber, statementIndex);
        this.variable = variable;
    }

    public Variable getVariable() {
        return this.variable;
    }

}
