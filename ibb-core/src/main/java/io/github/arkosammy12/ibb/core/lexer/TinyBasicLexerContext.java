package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.util.SourceCharacter;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.core.util.SourceStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

final class TinyBasicLexerContext {

    private final SourceStream<SourceCharacter> sourceCharacterStream;
    private final Collection<Token> tokens = new ArrayList<>();
    private boolean lexingStringToken = false;

    TinyBasicLexerContext(SourceStream<SourceCharacter> sourceCharacterStream) {
        this.sourceCharacterStream = sourceCharacterStream;
    }

    void setLexingStringToken(boolean value) {
        this.lexingStringToken = value;
    }

    SourceCharacter poll() {
        this.discardWhitespaceIfNecessary();
        return this.sourceCharacterStream.pollOrDefault();
    }

    SourceCharacter peek() {
        this.discardWhitespaceIfNecessary();
        return this.sourceCharacterStream.peekOrDefault();
    }

    boolean hasCharacters() {
        return !this.sourceCharacterStream.isEmpty();
    }

    void addToken(Token token) {
        this.tokens.add(token);
    }

    SourceStream<Token> getTokenStream() {
        SourcePosition lastPosition = this.tokens.stream().reduce((_, second) -> second).map(Token::getSourcePosition).orElseGet(() -> new SourcePosition(0, 0));
        return new SourceStream<>(this.tokens, new EndOfFileToken(lastPosition));
    }

    private void discardWhitespaceIfNecessary() {
        if (this.lexingStringToken) {
            return;
        }
        Optional<SourceCharacter> sourceCharacter;
        while ((sourceCharacter = this.sourceCharacterStream.peek()).isPresent() && sourceCharacter.get().character() == ' ') {
            this.sourceCharacterStream.poll();
        }
    }

}
