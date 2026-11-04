package io.github.arkosammy12.ibb.core.exception;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

import java.util.Optional;

public class TinyBasicParseException extends Exception {

    private final String message;
    private final SourcePosition sourcePosition;

    public TinyBasicParseException(String message, SourcePosition sourcePosition) {
        super(message);
        this.message = message;
        this.sourcePosition = sourcePosition;
    }

    public Optional<SourcePosition> getSourcePosition() {
        return Optional.ofNullable(this.sourcePosition);
    }

}