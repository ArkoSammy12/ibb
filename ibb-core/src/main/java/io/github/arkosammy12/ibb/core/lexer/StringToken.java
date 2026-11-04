package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class StringToken extends Token {

    private final String lexeme;
    private final String contents;

    public StringToken(SourcePosition sourcePosition, String lexeme) {
        super(sourcePosition);
        this.lexeme = lexeme;
        if (this.lexeme.length() < 2) {
            throw new IllegalArgumentException("String token '%s' is too short to be a valid string!".formatted(this.lexeme));
        }
        String contents = lexeme;
        if (contents.charAt(0) == '"') {
            contents = contents.substring(1);
        } else {
            throw new IllegalArgumentException("String token '%s' does not begin with a '\"'!".formatted(this.lexeme));
        }
        if (contents.charAt(contents.length() - 1) == '"') {
            contents = contents.substring(0, contents.length() - 1);
        } else {
            throw new IllegalArgumentException("String token '%s' does not end with a '\"'!".formatted(this.lexeme));
        }
        this.contents = contents;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public String getContents() {
        return this.contents;
    }

}