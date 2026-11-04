package io.github.arkosammy12.ibb.core.grammar;

import java.util.Locale;
import java.util.Optional;

public interface Lexeme {

    String getPattern();

    static <T extends Enum<T> & Lexeme> boolean isPatternBeginningOfKeyword(Class<T> enumClass, String pattern) {
        for (T lexeme : enumClass.getEnumConstants()) {
            if (lexeme.getPattern().startsWith(pattern.toLowerCase(Locale.ROOT))) {
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