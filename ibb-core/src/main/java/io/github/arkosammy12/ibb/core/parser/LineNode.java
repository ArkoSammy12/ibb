package io.github.arkosammy12.ibb.core.parser;

import java.util.OptionalLong;

public class LineNode {

    private final Long lineNumber;
    private final StatementNode statementNode;

    public LineNode(Long lineNumber, StatementNode statementNode) {
        if (lineNumber != null && lineNumber < 0) {
            throw new IllegalArgumentException("Line node line number '%d' cannot be negative!".formatted(lineNumber));
        }
        this.lineNumber = lineNumber;
        this.statementNode = statementNode;
    }

    public OptionalLong getLineNumber() {
        return this.lineNumber == null ? OptionalLong.empty() : OptionalLong.of(this.lineNumber);
    }

    public StatementNode getStatementNode() {
        return this.statementNode;
    }

    @Override
    public String toString() {
        return "LineNode[lineNumber=%s, statement=%s]".formatted(this.lineNumber == null ? "" : this.lineNumber, this.statementNode);
    }

}
