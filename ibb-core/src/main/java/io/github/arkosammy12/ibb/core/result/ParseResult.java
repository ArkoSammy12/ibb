package io.github.arkosammy12.ibb.core.result;

import java.util.function.Function;

public sealed interface ParseResult<T extends ParseValue, E extends ParseError> {

    record Ok<T extends ParseValue, E extends ParseError>(T value) implements ParseResult<T, E> {}

    record Error<T extends ParseValue, E extends ParseError>(E error) implements ParseResult<T, E> {}

    default <R extends ParseValue> ParseResult<R, E> map(Function<? super T, ? extends R> mapper)  {
        return switch (this) {
            case Ok<T, E> ok -> new Ok<>(mapper.apply(ok.value()));
            case Error<T, E> error -> new Error<>(error.error());
        };
    }

    default <R extends ParseValue> ParseResult<R, E> flatMap(Function<? super T, ? extends ParseResult<R, E>> mapper) {
        return switch (this) {
            case Ok<T, E> ok -> mapper.apply(ok.value());
            case Error<T, E> error -> new Error<>(error.error());
        };
    }

}
