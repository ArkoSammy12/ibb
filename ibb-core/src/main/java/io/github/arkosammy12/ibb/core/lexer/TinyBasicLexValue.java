package io.github.arkosammy12.ibb.core.lexer;

import io.github.arkosammy12.ibb.core.result.ParseValue;
import io.github.arkosammy12.ibb.core.util.SourceStream;

public record TinyBasicLexValue(SourceStream<Token> tokenStream) implements ParseValue {}
