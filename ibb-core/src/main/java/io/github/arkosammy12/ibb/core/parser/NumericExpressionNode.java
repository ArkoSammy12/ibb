package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public sealed interface NumericExpressionNode extends ExpressionNode {

    record VariableNode(Variable variable) implements NumericExpressionNode {}

    record NumberNode(short value) implements NumericExpressionNode {}

    record ArrayReferenceNode(ExpressionNode arrayIndexExpression) implements NumericExpressionNode {}

    record AbsFunctionNode(ExpressionNode argumentExpression) implements NumericExpressionNode { }

    record AscFunctionNode(String argumentString) implements NumericExpressionNode {}

    record RndFunctionNode(ExpressionNode argumentExpression) implements NumericExpressionNode {}

    record SgnFunctionNode(ExpressionNode argumentExpression) implements NumericExpressionNode {}

    record UnaryNumericExpression(UnaryOperator unaryOperator, ExpressionNode argumentExpression) implements NumericExpressionNode {}

    record BinaryNumericExpression(BinaryOperator binaryOperator, ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    enum BinaryOperator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO,
        POWER,
    }

    enum UnaryOperator {
        PLUS,
        MINUS,
        NONE
    }

}