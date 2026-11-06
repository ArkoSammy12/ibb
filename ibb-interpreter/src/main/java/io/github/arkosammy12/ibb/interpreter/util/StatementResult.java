package io.github.arkosammy12.ibb.interpreter.util;

import io.github.arkosammy12.ibb.core.parser.StatementNode;

public sealed interface StatementResult {

    record Ok(int lineNumber, StatementNode statementNode) implements StatementResult {}

    enum Missing implements StatementResult {
        END_OF_LINE,
        END_OF_PROGRAM
    }

}
