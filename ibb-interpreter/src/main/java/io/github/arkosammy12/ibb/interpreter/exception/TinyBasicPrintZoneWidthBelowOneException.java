package io.github.arkosammy12.ibb.interpreter.exception;

public final class TinyBasicPrintZoneWidthBelowOneException extends TinyBasicInterpreterException {

    private final short printZoneWidth;

    public TinyBasicPrintZoneWidthBelowOneException(Integer lineNumber, int statementIndex, short printZoneWidth) {
        if (printZoneWidth >= 1) {
            throw new IllegalArgumentException("The provided print zone width '%d' cannot be 1 or greater!".formatted(printZoneWidth));
        }
        super(lineNumber, statementIndex);
        this.printZoneWidth = printZoneWidth;
    }

    public short getPrintZoneWidth() {
        return this.printZoneWidth;
    }

}
