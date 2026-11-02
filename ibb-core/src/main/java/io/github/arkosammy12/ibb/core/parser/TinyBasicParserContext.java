package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.lexer.Token;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.core.util.SourceStream;

final class TinyBasicParserContext {

    private final SourceStream<Token> tokenStream;

    TinyBasicParserContext(SourceStream<Token> tokenStream) {
        this.tokenStream = tokenStream;
    }

    Token poll() {
        return this.tokenStream.pollOrDefault();
    }

    Token peek() {
        return this.tokenStream.peekOrDefault();
    }

    SourcePosition peekSourcePosition() {
        return this.tokenStream.peek().map(Token::getSourcePosition).orElse(null);
    }

    boolean hasTokens() {
        return !this.tokenStream.isEmpty();
    }

}
