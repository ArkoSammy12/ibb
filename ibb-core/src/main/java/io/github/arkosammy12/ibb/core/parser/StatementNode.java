package io.github.arkosammy12.ibb.core.parser;

import java.util.Collection;

public sealed interface StatementNode {

    record Print(ExpressionListNode firstExpressionListNode, Collection<ExpressionListNode> extraExpressionListNodes) implements StatementNode {}

    record If(RelationalOperation relationalOperation, ExpressionNode lhs, ExpressionNode rhs, StatementNode statementNode) implements StatementNode {

        public enum RelationalOperation {
            LESS_THAN,
            LESS_THAN_OR_EQUALS,
            GREATER_THAN,
            GREATER_THAN_OR_EQUALS,
            EQUALS,
            NOT_EQUALS
        }

    }

    record Goto(ExpressionNode expressionNode) implements StatementNode {}

    record Input(AssignmentTargetNode firstAssignmentTargetNode, Collection<AssignmentTargetNode> extraAssignmentTargetNodes) implements StatementNode {}

    record VariableAssignment(AssignmentTargetNode assignmentTargetNode, ExpressionNode expressionNode) implements StatementNode {}

    record Gosub(ExpressionNode expressionNode) implements StatementNode {}

    record Return() implements StatementNode {}

    record End() implements StatementNode {}

    record Rem() implements StatementNode {}

}
