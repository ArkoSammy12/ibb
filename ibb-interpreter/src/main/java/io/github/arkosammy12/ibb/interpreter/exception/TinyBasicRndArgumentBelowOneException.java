package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicRndArgumentBelowOneException extends TinyBasicInterpreterException {

    private final short argument;

    public TinyBasicRndArgumentBelowOneException(Integer lineNumber, int statementIndex, short argument) {
        super(lineNumber, statementIndex);
        this.argument = argument;
    }

    public short getArgument() {
        return this.argument;
    }

}
