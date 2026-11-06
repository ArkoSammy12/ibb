package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicDivisionByZeroException extends TinyBasicInterpreterException {

    private final short dividend;

    public TinyBasicDivisionByZeroException(Integer lineNumber, int statementIndex, short dividend) {
        super(lineNumber, statementIndex);
        this.dividend = dividend;
    }

    public short getDividend() {
        return this.dividend;
    }

}
