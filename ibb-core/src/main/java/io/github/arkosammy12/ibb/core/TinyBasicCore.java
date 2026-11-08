package io.github.arkosammy12.ibb.core;

import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexError;
import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexValue;
import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexer;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParseValue;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParser;
import io.github.arkosammy12.ibb.core.result.ParseError;
import io.github.arkosammy12.ibb.core.result.ParseResult;

import java.util.function.Supplier;

public final class TinyBasicCore {

    private TinyBasicCore() {
        throw new AssertionError();
    }

    public static ParseResult<? extends TinyBasicParseValue, ? extends ParseError> parseTinyBasic(String line) {
        return parseTinyBasic(line, TinyBasicLexer::new, TinyBasicParser::new);
    }

    @SuppressWarnings("unchecked")
    public static ParseResult<? extends TinyBasicParseValue, ? extends ParseError> parseTinyBasic(String line, Supplier<TinyBasicLexer> lexer, Supplier<TinyBasicParser> parser) {
        return (ParseResult<TinyBasicParseValue, TinyBasicLexError>) switch (lexer.get().lex(line)) {
            case ParseResult.Error<?, ?> error -> error;
            case ParseResult.Ok<TinyBasicLexValue, TinyBasicLexError> ok -> parser.get().parse(ok.value().tokenStream());
        };
    }

}