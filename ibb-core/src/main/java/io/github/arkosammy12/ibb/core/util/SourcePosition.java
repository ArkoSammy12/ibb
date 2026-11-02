package io.github.arkosammy12.ibb.core.util;

import java.util.OptionalInt;

public final class SourcePosition {

    private final int column;
    private final Integer row;

    public SourcePosition(Integer row, int column) {
        if (row != null && row < 0) {
            throw new IllegalArgumentException("Source position row '%d' cannot be negative!".formatted(row));
        }
        if (column < 0) {
            throw new IllegalArgumentException("Source position column '%d' cannot be negative!".formatted(column));
        }
        this.row = row;
        this.column = column;
    }

    public SourcePosition(int column) {
        this(null, column);
    }

    public OptionalInt getRow() {
        return this.row == null ? OptionalInt.empty() : OptionalInt.of(this.row);
    }

    public int getColumn() {
        return this.column;
    }

    @Override
    public String toString() {
        return "SourcePosition[column=%s, row=%s]".formatted(this.row == null ? "" : this.row, this.column);
    }

}
