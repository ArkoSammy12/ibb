package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

import java.util.Collection;

public sealed interface StatementNode {

    record Print(ExpressionListNode firstExpressionListNode, Collection<ExpressionListNode> extraExpressionListNodes) implements StatementNode {}

    record Goto(NumericalExpressionNode numericalExpressionNode) implements StatementNode {}

    record Input(AssignmentTargetNode firstAssignmentTargetNode, Collection<AssignmentTargetNode> extraAssignmentTargetNodes) implements StatementNode {}

    record VariableAssignment(AssignmentTargetNode assignmentTargetNode, NumericalExpressionNode numericalExpressionNode) implements StatementNode {}

    record GoSub(NumericalExpressionNode numericalExpressionNode) implements StatementNode {}

    record Return() implements StatementNode {}

    record End() implements StatementNode {}

    record Rem() implements StatementNode {}

    record Next(Variable variable) implements StatementNode {}

    record For(Variable variable, NumericalExpressionNode startingNumericalExpressionNodeValue, NumericalExpressionNode finalNumericalExpressionNodeValue) implements StatementNode {}

    record If(RelationalOperation relationalOperation, NumericalExpressionNode lhs, NumericalExpressionNode rhs, StatementNode statementNode) implements StatementNode {

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
