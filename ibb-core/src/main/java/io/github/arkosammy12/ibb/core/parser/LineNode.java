package io.github.arkosammy12.ibb.core.parser;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class LineNode {

    private final Integer lineNumber;
    private final StatementNode firstStatementNode;
    private final Collection<StatementNode> extraStatementNodes;

    public LineNode(Integer lineNumber, StatementNode firstStatementNode, Collection<StatementNode> extraStatementNodes) {
        if (lineNumber != null && lineNumber < 0) {
            throw new IllegalArgumentException("Line node line number '%d' cannot be negative!".formatted(lineNumber));
        }
        this.lineNumber = lineNumber;
        this.firstStatementNode = firstStatementNode;
        this.extraStatementNodes = extraStatementNodes;
    }

    public OptionalInt getLineNumber() {
        return this.lineNumber == null ? OptionalInt.empty() : OptionalInt.of(this.lineNumber);
    }

    public StatementNode getFirstStatementNode() {
        return this.firstStatementNode;
    }

    @Override
    public String toString() {
        return "LineNode[lineNumber=%s, firstStatement=%s, extraStatements=[%s]]".formatted(this.lineNumber == null ? "" : this.lineNumber, this.firstStatementNode, this.extraStatementNodes.stream().map(StatementNode::toString).collect(Collectors.joining(", ")));
    }

}
