package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.grammar.Keyword;
import io.github.arkosammy12.ibb.core.util.SourceCharacter;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.core.util.SourceStream;

import java.util.ArrayList;
import java.util.Collection;

final class TinyBasicLexerContext {

    private final SourceStream<SourceCharacter> sourceCharacterStream;
    private final Collection<Token> tokens = new ArrayList<>();

    TinyBasicLexerContext(SourceStream<SourceCharacter> sourceCharacterStream) {
        this.sourceCharacterStream = sourceCharacterStream;
    }

    SourceCharacter poll() {
        SourceCharacter sourceCharacter = this.sourceCharacterStream.pollOrDefault();
        return sourceCharacter == null ? new SourceCharacter(null, new SourcePosition(0, 0)) : sourceCharacter;
    }

    SourceCharacter peek() {
        SourceCharacter sourceCharacter = this.sourceCharacterStream.peekOrDefault();
        return sourceCharacter == null ? new SourceCharacter(null, new SourcePosition(0, 0)) : sourceCharacter;
    }

    void push(SourceCharacter sourceCharacter) {
        this.sourceCharacterStream.push(sourceCharacter);
    }

    boolean hasCharacters() {
        return !this.sourceCharacterStream.isEmpty();
    }

    void addToken(Token token) {
        if (token instanceof KeywordToken keywordToken && keywordToken.getKeyword() == Keyword.REM) {
            while (!this.sourceCharacterStream.isEmpty()) {
                this.sourceCharacterStream.poll();
            }
        } else {
            this.tokens.add(token);
        }
    }

    SourceStream<Token> getTokenStream() {
        SourcePosition lastPosition = this.tokens.stream().reduce((_, second) -> second).map(Token::getSourcePosition).orElseGet(() -> new SourcePosition(0, 0));
        return new SourceStream<>(this.tokens, new EndOfLineToken(lastPosition));
    }

}