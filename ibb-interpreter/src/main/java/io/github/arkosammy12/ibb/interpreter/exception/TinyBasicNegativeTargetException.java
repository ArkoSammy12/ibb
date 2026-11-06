package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicNegativeTargetException extends TinyBasicInterpreterException {

    private final JumpType jumpType;
    private final short targetLineNumber;

    public TinyBasicNegativeTargetException(Integer lineNumber, int statementIndex, JumpType jumpType, short targetLineNumber) {
        if (targetLineNumber >= 0) {
            throw new IllegalArgumentException("The provided target line number '%d' cannot be zero or positive!".formatted(targetLineNumber));
        }
        super(lineNumber, statementIndex);
        this.jumpType = jumpType;
        this.targetLineNumber = targetLineNumber;
    }

    public JumpType getJumpType() {
        return this.jumpType;
    }

    public short getTargetLineNumber() {
        return this.targetLineNumber;
    }

}
