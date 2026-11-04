package io.github.arkosammy12.ibb.core.util;

public record SourcePosition(int row, int column) {

    public SourcePosition {
        if (this.row() < 0) {
            throw new IllegalArgumentException("Source position row '%d' cannot be negative!".formatted(row));
        }
        if (this.column() < 0) {
            throw new IllegalArgumentException("Source position column '%d' cannot be negative!".formatted(column));
        }
    }

    public SourcePosition(int column) {
        this(0, column);
    }

}