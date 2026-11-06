package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicReturnWithoutGoSubException extends TinyBasicInterpreterException {

    public TinyBasicReturnWithoutGoSubException(Integer lineNumber, int statementIndex) {
        super(lineNumber, statementIndex);
    }

}
