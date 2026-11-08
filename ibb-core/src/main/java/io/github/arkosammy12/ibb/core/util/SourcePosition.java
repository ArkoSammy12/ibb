package io.github.arkosammy12.ibb.core.util;

public record SourcePosition(int column) {

    public SourcePosition {
        if (this.column() < 0) {
            throw new IllegalArgumentException("Source position column '%d' cannot be negative!".formatted(column));
        }
    }

}