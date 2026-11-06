package io.github.arkosammy12.ibb.interpreter.exception;

import java.util.OptionalInt;

public abstract sealed class TinyBasicInterpreterException extends Exception permits TinyBasicArrayIndexOutOfBoundsException, TinyBasicDivisionByZeroException, TinyBasicNegativeTargetException, TinyBasicNextWithoutForException, TinyBasicReturnWithoutGoSubException, TinyBasicRndArgumentBelowOneException, TinyBasicUndefinedTargetException {

    private final Integer lineNumber;
    private final int statementIndex;

    public TinyBasicInterpreterException(Integer lineNumber, int statementIndex) {
        if (lineNumber != null && lineNumber < 0) {
            throw new IllegalArgumentException("The line number '%d' cannot be negative!".formatted(lineNumber));
        }
        if (statementIndex < 0) {
            throw new IllegalArgumentException("The statement index '%d' cannot be negative!".formatted(statementIndex));
        }
        this.lineNumber = lineNumber;
        this.statementIndex = statementIndex;
    }

    public OptionalInt getLineNumber() {
        return this.lineNumber == null ? OptionalInt.empty() : OptionalInt.of(this.lineNumber);
    }

    public int getStatementIndex() {
        return this.statementIndex;
    }

}
