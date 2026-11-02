package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.result.ParseError;
import io.github.arkosammy12.ibb.core.util.SourcePosition;

import java.util.Optional;

public final class TinyBasicLexError implements ParseError {

    private final String message;
    private final SourcePosition sourcePosition;

    public TinyBasicLexError(String message, SourcePosition sourcePosition) {
        this.message = message;
        this.sourcePosition = sourcePosition;
    }

    public TinyBasicLexError(String message) {
        this(message, null);
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public Optional<SourcePosition> getSourcePosition() {
        return Optional.ofNullable(this.sourcePosition);
    }

    @Override
    public String toString() {
        return ParseError.getString(this);
    }

}
