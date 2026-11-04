package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

import java.util.Collection;
import java.util.List;

public sealed interface StatementNode {

    record VariableAssignment(AssignmentTargetNode assignmentTargetNode, NumericExpressionNode numericExpressionNode) implements StatementNode {}

    record Goto(NumericExpressionNode numericExpressionNode) implements StatementNode {}

    record GoSub(NumericExpressionNode numericExpressionNode) implements StatementNode {}

    record Return() implements StatementNode {}

    record If(BooleanExpressionNode conditionalExpressionNode, StatementNode statementNode) implements StatementNode {}

    record For(Variable variable, NumericExpressionNode startingNumericExpressionNodeValue, NumericExpressionNode finalNumericExpressionNodeValue) implements StatementNode {}

    record Next(Variable variable) implements StatementNode {}

    record Rem() implements StatementNode {}

    record End() implements StatementNode {}

    record Print(NumericExpressionListNode firstNumericExpressionListNode, Collection<NumericExpressionListNode> extraNumericExpressionListNodes) implements StatementNode {

        public Print {
            extraNumericExpressionListNodes = List.copyOf(extraNumericExpressionListNodes);
        }

    }

    record Input(AssignmentTargetNode firstAssignmentTargetNode, Collection<AssignmentTargetNode> extraAssignmentTargetNodes) implements StatementNode {

        public Input {
            extraAssignmentTargetNodes = List.copyOf(extraAssignmentTargetNodes);
        }

    }

}
