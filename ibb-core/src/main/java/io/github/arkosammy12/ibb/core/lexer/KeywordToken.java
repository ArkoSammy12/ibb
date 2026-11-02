package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.grammar.Keyword;
import io.github.arkosammy12.ibb.core.util.SourcePosition;

public final class KeywordToken extends Token {

    private final Keyword keyword;
    private final String lexeme;

    public KeywordToken(SourcePosition sourcePosition, Keyword keyword, String lexeme) {
        super(sourcePosition);
        this.lexeme = lexeme;
        this.keyword = keyword;
    }

    @Override
    public String getLexeme() {
        return this.lexeme;
    }

    public Keyword getKeyword() {
        return this.keyword;
    }

}
