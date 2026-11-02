package io.github.arkosammy12.ibb.core.result;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

import java.util.Optional;

public interface ParseError {

    String getMessage();

    Optional<SourcePosition> getSourcePosition();

    static String getString(ParseError parseError) {
        return "ParseError[sourcePosition=%s, message=%s]".formatted(parseError.getSourcePosition().map(SourcePosition::toString).orElse(""), parseError.getMessage());
    }

    static String getMessage(ParseError parseError) {
        return "%s%s".formatted(parseError.getSourcePosition().map(sourcePosition -> {
            if (sourcePosition.getRow().isPresent()) {
                return "(%d:%d) ".formatted(sourcePosition.getRow().getAsInt() + 1, sourcePosition.getColumn() + 1);
            } else {
                return "(%d) ".formatted(sourcePosition.getColumn() + 1);
            }
        }).orElse(""), parseError.getMessage());
    }

}
