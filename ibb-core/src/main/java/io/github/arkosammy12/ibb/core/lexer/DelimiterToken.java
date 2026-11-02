package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.grammar.Delimiter;
import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class DelimiterToken extends Token {

    private final Delimiter delimiter;
    private final String lexeme;

    public DelimiterToken(SourcePosition sourcePosition, Delimiter delimiter, String lexeme) {
        super(sourcePosition);
        this.delimiter = delimiter;
        this.lexeme = lexeme;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public Delimiter getDelimiter() {
        return this.delimiter;
    }

}
