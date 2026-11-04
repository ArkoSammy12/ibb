package io.github.arkosammy12.ibb.core.parser;

public sealed interface BooleanExpressionNode extends ExpressionNode {

    record RelationalOperationNode(UnaryOperator unaryOperator, RelationalOperation relationalOperation, NumericExpressionNode lhs, NumericExpressionNode rhs) implements BooleanExpressionNode {}

    record UnaryBooleanExpression(UnaryOperator unaryOperator, BooleanExpressionNode booleanExpressionNode) implements BooleanExpressionNode {}

    record BinaryBooleanOperation(BinaryOperator binaryOperator, BooleanExpressionNode lhs, BooleanExpressionNode rhs) implements BooleanExpressionNode {}

    enum UnaryOperator {
        NONE,
        NOT
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