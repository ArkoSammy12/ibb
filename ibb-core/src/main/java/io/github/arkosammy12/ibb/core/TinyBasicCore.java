package io.github.arkosammy12.ibb.core;

import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexError;
import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexValue;
import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexer;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParseValue;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParser;
import io.github.arkosammy12.ibb.core.result.ParseResult;

public final class TinyBasicCore {

    private TinyBasicCore() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    public static ParseResult<TinyBasicParseValue, TinyBasicLexError> parse(String line) {
        return (ParseResult<TinyBasicParseValue, TinyBasicLexError>) switch (new TinyBasicLexer().lex(line)) {
            case ParseResult.Error<?, ?> error -> error;
            case ParseResult.Ok<TinyBasicLexValue, TinyBasicLexError> ok -> new TinyBasicParser().parse(ok.value().tokenStream());
        };
    }

}
