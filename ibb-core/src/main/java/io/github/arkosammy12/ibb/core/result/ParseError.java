package io.github.arkosammy12.ibb.core.result;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

import java.util.Optional;

public interface ParseError {

    String getMessage();

    Optional<SourcePosition> getSourcePosition();

    static String getString(ParseError parseError) {
        return "ParseError[sourcePosition=%s, message=%s]".formatted(parseError.getSourcePosition().map(SourcePosition::toString).orElse(""), parseError.getMessage());
    }

}