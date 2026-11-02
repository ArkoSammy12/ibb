package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.exception.TinyBasicParseException;
import io.github.arkosammy12.ibb.core.grammar.*;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourceCharacter;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.core.util.SourceStream;
import io.github.arkosammy12.ibb.core.util.TriFunction;

import java.util.Collection;
import java.util.Optional;

public final class TinyBasicLexer {

    public ParseResult<TinyBasicLexValue, TinyBasicLexError> lex(String line) {
        return this.lex(SourceStream.fromLine(line));
    }

    public ParseResult<TinyBasicLexValue, TinyBasicLexError> lex(Collection<String> lines) {
        return this.lex(SourceStream.fromLines(lines));
    }

    public ParseResult<TinyBasicLexValue, TinyBasicLexError> lex(SourceStream<SourceCharacter> sourceCharacterSourceStream) {
        try {
            TinyBasicLexerContext context = new TinyBasicLexerContext(sourceCharacterSourceStream);
            while (context.hasCharacters()) {
                SourceCharacter sourceCharacter = context.poll();
                if (isDigit(sourceCharacter.character())) {
                    context.addToken(this.lexNumber(context, sourceCharacter));
                    continue;
                }
                if (sourceCharacter.character() == '"') {
                    context.addToken(this.lexString(context, sourceCharacter));
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Keyword.class, KeywordToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Variable.class, VariableToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Operator.class, OperatorToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Delimiter.class, DelimiterToken::new)) {
                    continue;
                }
                return new ParseResult.Error<>(new TinyBasicLexError("Unknown token '%s'!".formatted(sourceCharacter.character()), sourceCharacter.sourcePosition()));
            }
            return new ParseResult.Ok<>(new TinyBasicLexValue(context.getTokenStream()));
        } catch (TinyBasicParseException tinyBasicParseException) {
            return new ParseResult.Error<>(new TinyBasicLexError(tinyBasicParseException.getMessage(), tinyBasicParseException.getSourcePosition().orElse(null)));
        }
    }

    private <T extends Enum<T> & Lexeme> boolean lexToken(TinyBasicLexerContext context, SourceCharacter firstLetterCharacter, Class<T> lexemeClass, TriFunction<SourcePosition, T, String, Token> tokenFunction) {
        if (firstLetterCharacter.character() == null) {
            return false;
        }
        if (!Lexeme.isPatternBeginningOfKeyword(lexemeClass, String.valueOf(firstLetterCharacter.character()))) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(firstLetterCharacter.character());
        while (context.hasCharacters()) {
            SourceCharacter sourceCharacter = context.peek();
            if (sourceCharacter.character() == null) {
                break;
            }
            if (Lexeme.isPatternBeginningOfKeyword(lexemeClass, stringBuilder + String.valueOf(sourceCharacter.character()))) {
                context.poll();
                stringBuilder.append(sourceCharacter.character());
            } else {
                break;
            }
        }
        String lexeme = stringBuilder.toString();
        Optional<T> token = Lexeme.getMatchingLexeme(lexemeClass, lexeme);
        if (token.isEmpty()) {
            return false;
        } else {
            context.addToken(tokenFunction.apply(firstLetterCharacter.sourcePosition(), token.get(), lexeme));
            return true;
        }
    }

    private StringToken lexString(TinyBasicLexerContext context, SourceCharacter leftDoubleQuoteCharacter) throws TinyBasicParseException {
        context.setLexingStringToken(true);
        StringBuilder sb = new StringBuilder();
        sb.append(leftDoubleQuoteCharacter.character());
        while (context.hasCharacters()) {
            SourceCharacter sourceCharacter = context.poll();
            sb.append(sourceCharacter.character());
            if (sourceCharacter.character() == '"') {
                context.setLexingStringToken(false);
                return new StringToken(leftDoubleQuoteCharacter.sourcePosition(), sb.toString());
            }
        }
         throw new TinyBasicParseException("Unclosed string literal '%s'".formatted(sb.toString()), leftDoubleQuoteCharacter.sourcePosition());
    }

    private NumberToken lexNumber(TinyBasicLexerContext context, SourceCharacter numberCharacter) {
        StringBuilder lexemeBuilder = new StringBuilder();
        lexemeBuilder.append(numberCharacter.character());
        while (isDigit(context.peek().character())) {
            lexemeBuilder.append(context.poll().character());
        }
        return new NumberToken(numberCharacter.sourcePosition(), Long.parseLong(lexemeBuilder.toString()));
    }

    private static boolean isDigit(Character character) {
        return character != null && Character.isDigit(character);
    }

}
