package io.github.arkosammy12.ibb.core.result;

public sealed interface ParseResult<T extends ParseValue, E extends ParseError> {

    record Ok<T extends ParseValue, E extends ParseError>(T value) implements ParseResult<T, E> {}

    record Error<T extends ParseValue, E extends ParseError>(E error) implements ParseResult<T, E> {}

}