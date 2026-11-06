package io.github.arkosammy12.ibb.interpreter.util;

import io.github.arkosammy12.ibb.core.parser.LineNode;
import io.github.arkosammy12.ibb.core.parser.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class TinyBasicLineList {

    private LineNode immediateModeLine = null;
    private final List<NumberedLineNode> lines = new ArrayList<>();

    public void putLine(LineNode lineNode) {
        OptionalInt lineNumberOptional = lineNode.getLineNumber();
        if (lineNumberOptional.isEmpty()) {
            this.immediateModeLine = lineNode;
            return;
        }
        int lineNumber = lineNumberOptional.getAsInt();
        if (lineNumber < 0) {
            throw new IllegalArgumentException("The provided line number '%d' cannot be negative!".formatted(lineNumber));
        }
        if (this.lines.isEmpty()) {
            this.lines.add(new NumberedLineNode(lineNumber, lineNode));
        } else {
            int insertPosition = 0;
            for (int i = lines.size() - 1; i >= 0; i--) {
                if (lineNumber >= this.lines.get(i).lineNumber()) {
                    insertPosition = i + 1;
                    break;
                }
            }
            int existingLineIndex = Math.max(0, insertPosition - 1);
            if (lineNumber == this.lines.get(existingLineIndex).lineNumber()) {
                this.lines.set(existingLineIndex, new NumberedLineNode(lineNumber, lineNode));
            } else {
                this.lines.add(insertPosition, new NumberedLineNode(lineNumber, lineNode));
            }
        }
    }

    public void removeLine(int lineNumber) {
        if (lineNumber < 0) {
            this.immediateModeLine = null;
        } else {
            this.lines.removeIf(numberedLineNode -> numberedLineNode.lineNumber() == lineNumber);
        }
    }

    public void clear() {
        this.immediateModeLine = null;
        this.lines.clear();
    }

    public StatementResult getStatement(int lineIndex, int statementIndex) {
        if (statementIndex < 0) {
            throw new IllegalArgumentException("The provided statement index '%d' cannot be negative!".formatted(statementIndex));
        }
        LineNode lineNode = null;
        int lineNumber = -1;
        if (lineIndex < 0) {
            lineNode = immediateModeLine;
        } else if (lineIndex < this.lines.size()) {
            NumberedLineNode numberedLineNode = this.lines.get(lineIndex);
            lineNode = numberedLineNode.lineNode();
            lineNumber = numberedLineNode.lineNumber();
        }
        if (lineNode == null) {
            return StatementResult.Missing.END_OF_PROGRAM;
        }
        if (statementIndex == 0) {
            return new StatementResult.Ok(lineNumber, lineNode.getFirstStatementNode());
        } else {
            int extraStatementIndex = statementIndex - 1;
            List<StatementNode> extraStatements = lineNode.getExtraStatementNodes();
            if (extraStatementIndex < extraStatements.size()) {
                return new StatementResult.Ok(lineNumber, extraStatements.get(extraStatementIndex));
            } else {
                return StatementResult.Missing.END_OF_LINE;
            }
        }
    }

    public int getIndexForLineNumber(int lineNumber) {
        if (lineNumber < 0) {
            throw new IllegalStateException("The provided line number '%d' cannot be negative!".formatted(lineNumber));
        }
        for (int i = 0; i < this.lines.size(); i++) {
            if (this.lines.get(i).lineNumber() == lineNumber) {
                return i;
            }
        }
        return -1;
    }

    record NumberedLineNode(int lineNumber, LineNode lineNode) {}

}
