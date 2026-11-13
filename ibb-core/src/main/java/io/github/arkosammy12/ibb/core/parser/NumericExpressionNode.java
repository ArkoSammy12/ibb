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

    record BitNotFunctionNode(ExpressionNode expressionNode) implements NumericExpressionNode {}

    record BitAndFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record BitOrFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record BitXorFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record ShlFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record ShrFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record SarFunctionNode(ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

    record UnaryNumericExpression(UnaryOperator unaryOperator, ExpressionNode expressionNode) implements NumericExpressionNode {}

    record BinaryNumericOperation(BinaryOperator binaryOperator, ExpressionNode lhs, ExpressionNode rhs) implements NumericExpressionNode {}

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