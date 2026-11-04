package io.github.arkosammy12.ibb.core.grammar;

import java.util.Locale;
import java.util.Optional;

public sealed interface Lexeme permits Delimiter, Keyword, Operator, Variable {

    String getPattern();

    static <T extends Enum<T> & Lexeme> boolean isPatternBeginningOfKeyword(Class<T> enumClass, String pattern) {
        String lowercasedPattern = pattern.toLowerCase(Locale.ROOT);
        for (T lexeme : enumClass.getEnumConstants()) {
            if (lexeme.getPattern().startsWith(lowercasedPattern)) {
                return true;
            }
        }
        return false;
    }

    static <T extends Enum<T> & Lexeme> Optional<T> getMatchingLexeme(Class<T> enumClass, String pattern) {
        for (T lexeme : enumClass.getEnumConstants()) {
            if (lexeme.getPattern().equalsIgnoreCase(pattern)) {
                return Optional.of(lexeme);
            }
        }
        return Optional.empty();
    }

}