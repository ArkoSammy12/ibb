package io.github.arkosammy12.ibb.core.parser;

public sealed interface BooleanExpressionNode extends ExpressionNode {

    record RelationalOperationNode(RelationalOperation relationalOperation, ExpressionNode lhs, ExpressionNode rhs) implements BooleanExpressionNode {}

    record UnaryBooleanExpression(UnaryOperator unaryOperator, ExpressionNode expressionNode) implements BooleanExpressionNode {}

    record BinaryBooleanOperation(BinaryOperator binaryOperator, ExpressionNode lhs, ExpressionNode rhs) implements BooleanExpressionNode {}

    enum UnaryOperator {
        NONE,
        NOT,
    }

    enum BinaryOperator {
        OR,
        AND
    }

    enum RelationalOperation {
        LESS_THAN,
        LESS_THAN_OR_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        EQUALS,
        NOT_EQUALS
    }

}