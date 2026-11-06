package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicArrayIndexOutOfBoundsException extends TinyBasicInterpreterException {

    private final short index;

    public TinyBasicArrayIndexOutOfBoundsException(Integer lineNumber, int statementIndex, short index) {
        super(lineNumber, statementIndex);
        this.index = index;
    }

    public short getIndex() {
        return this.index;
    }

}
