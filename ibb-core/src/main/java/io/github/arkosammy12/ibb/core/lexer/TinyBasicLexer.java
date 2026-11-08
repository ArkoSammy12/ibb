package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.exception.TinyBasicParseException;
import io.github.arkosammy12.ibb.core.grammar.*;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourceCharacter;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.core.util.SourceStream;

import java.util.Collection;
import java.util.Optional;
import java.util.Stack;

public final class TinyBasicLexer {

    public ParseResult<TinyBasicLexValue, TinyBasicLexError> lex(String line) {
        return this.lex(SourceStream.fromLine(line));
    }

    public ParseResult<TinyBasicLexValue, TinyBasicLexError> lex(SourceStream<SourceCharacter> sourceCharacterSourceStream) {
        try {
            TinyBasicLexerContext context = new TinyBasicLexerContext(sourceCharacterSourceStream);
             while (context.hasCharacters()) {
                SourceCharacter sourceCharacter = context.poll();
                if (this.lexToken(context, sourceCharacter, Keyword.class, KeywordToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Operator.class, OperatorToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Delimiter.class, DelimiterToken::new)) {
                    continue;
                }
                if (this.lexToken(context, sourceCharacter, Variable.class, VariableToken::new)) {
                    continue;
                }
                if (isDigit(sourceCharacter.character())) {
                    context.addToken(this.lexNumber(context, sourceCharacter));
                    continue;
                }
                if (sourceCharacter.character() == '"') {
                    context.addToken(this.lexString(context, sourceCharacter));
                    continue;
                }
                if (sourceCharacter.character() == '@') {
                    context.addToken(new ArraySigilToken(sourceCharacter.sourcePosition()));
                    continue;
                }
                if (sourceCharacter.character() == '\'') {
                    context.addToken(new KeywordToken(sourceCharacter.sourcePosition(), Keyword.REM, "'"));
                    continue;
                }
                if (isWhitespace(sourceCharacter.character())) {
                    continue;
                }
                return new ParseResult.Error<>(new TinyBasicLexError("Unknown token '%s'!".formatted(sourceCharacter.character()), sourceCharacter.sourcePosition()));
            }
            return new ParseResult.Ok<>(new TinyBasicLexValue(context.getTokenStream()));
        } catch (TinyBasicParseException tinyBasicParseException) {
            return new ParseResult.Error<>(new TinyBasicLexError(tinyBasicParseException.getMessage(), tinyBasicParseException.getSourcePosition().orElse(null)));
        }
    }

    private <T extends Enum<T> & Lexeme> boolean lexToken(TinyBasicLexerContext context, SourceCharacter firstLetterCharacter, Class<T> lexemeClass, TokenFunction<T> tokenFunction) {
        if (firstLetterCharacter.character() == null) {
            return false;
        }
        if (!Lexeme.isPatternBeginningOfKeyword(lexemeClass, String.valueOf(firstLetterCharacter.character()))) {
            return false;
        }
        Stack<SourceCharacter> sourceCharacterStack = new Stack<>();
        StringBuilder sb = new StringBuilder();
        sb.append(firstLetterCharacter.character());
        while (context.hasCharacters()) {
            SourceCharacter sourceCharacter = context.peek();
            if (sourceCharacter.character() == null) {
                break;
            }
            if (Lexeme.isPatternBeginningOfKeyword(lexemeClass, sb + String.valueOf(sourceCharacter.character()))) {
                sourceCharacterStack.push(context.poll());
                sb.append(sourceCharacter.character());
            } else {
                break;
            }
        }
        String lexeme = sb.toString();
        Optional<T> token = Lexeme.getMatchingLexeme(lexemeClass, lexeme);

        if (token.isPresent()) {
            context.addToken(tokenFunction.createToken(firstLetterCharacter.sourcePosition(), token.get(), lexeme));
            return true;
        } else {
            // If our stack is non-empty, we could have consumed a pattern that matches a built-in pattern,
            // so we pop characters from the stack and push them back to the character queue until we come across a
            // known pattern, or if the stack runs out, in which case none of the previous characters matched a pattern
            // for the current pattern enum
            while (!sourceCharacterStack.isEmpty() && !sb.isEmpty()) {
                sb.deleteCharAt(sb.length() - 1);
                context.push(sourceCharacterStack.pop());
                String backtrackedLexeme = sb.toString();
                Optional<T> backtrackedToken = Lexeme.getMatchingLexeme(lexemeClass, backtrackedLexeme);
                if (backtrackedToken.isPresent()) {
                    context.addToken(tokenFunction.createToken(firstLetterCharacter.sourcePosition(), backtrackedToken.get(), backtrackedLexeme));
                    return true;
                }
            }
            return false;
        }
    }

    private StringToken lexString(TinyBasicLexerContext context, SourceCharacter leftDoubleQuoteCharacter) throws TinyBasicParseException {
        StringBuilder sb = new StringBuilder();
        sb.append(leftDoubleQuoteCharacter.character());
        while (context.hasCharacters()) {
            SourceCharacter sourceCharacter = context.poll();
            sb.append(sourceCharacter.character());
            if (sourceCharacter.character() == '"') {
                return new StringToken(leftDoubleQuoteCharacter.sourcePosition(), sb.toString());
            }
        }
         throw new TinyBasicParseException("Unclosed string literal '%s'!".formatted(sb.toString()), leftDoubleQuoteCharacter.sourcePosition());
    }

    private NumberToken lexNumber(TinyBasicLexerContext context, SourceCharacter numberCharacter) {
        StringBuilder lexemeBuilder = new StringBuilder();
        lexemeBuilder.append(numberCharacter.character());
        while (isDigit(context.peek().character())) {
            lexemeBuilder.append(context.poll().character());
        }
        return new NumberToken(numberCharacter.sourcePosition(), Integer.parseInt(lexemeBuilder.toString()));
    }

    private static boolean isDigit(Character character) {
        return character != null && Character.isDigit(character);
    }

    private static boolean isWhitespace(Character character) {
        return character != null && Character.isWhitespace(character);
    }

    private interface TokenFunction<T extends Enum<T> & Lexeme> {

        Token createToken(SourcePosition sourcePosition, T tokenClass, String lexeme);

    }

}