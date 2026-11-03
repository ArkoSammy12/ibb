package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

import java.util.Collection;

public sealed interface StatementNode {

    record Print(ExpressionListNode firstExpressionListNode, Collection<ExpressionListNode> extraExpressionListNodes) implements StatementNode {}

    record Goto(ExpressionNode expressionNode) implements StatementNode {}

    record Input(AssignmentTargetNode firstAssignmentTargetNode, Collection<AssignmentTargetNode> extraAssignmentTargetNodes) implements StatementNode {}

    record VariableAssignment(AssignmentTargetNode assignmentTargetNode, ExpressionNode expressionNode) implements StatementNode {}

    record GoSub(ExpressionNode expressionNode) implements StatementNode {}

    record Return() implements StatementNode {}

    record End() implements StatementNode {}

    record Rem() implements StatementNode {}

    record Next(Variable variable) implements StatementNode {}

    record For(Variable variable, ExpressionNode startingExpressionNodeValue, ExpressionNode finalExpressionNodeValue) implements StatementNode {}

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

}
