package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public sealed interface NumericalExpressionNode extends ExpressionListNode {

    UnaryOperator getUnaryOperator();

    record VariableNode(UnaryOperator unaryOperator, Variable variable) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record ArrayReferenceNode(UnaryOperator unaryOperator, NumericalExpressionNode numericalExpressionNode) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record NumberNode(UnaryOperator unaryOperator, int value) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SubExpressionNode(UnaryOperator unaryOperator, NumericalExpressionNode numericalExpressionNode) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record OperationExpressionNode(UnaryOperator unaryOperator, BinaryOperation binaryOperation, NumericalExpressionNode lhs, NumericalExpressionNode rhs) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AbsFunctionNode(UnaryOperator unaryOperator, NumericalExpressionNode numericalExpressionNode) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AscFunctionNode(UnaryOperator unaryOperator, String contents) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record RndFunctionNode(UnaryOperator unaryOperator, NumericalExpressionNode numericalExpressionNode) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SgnFunctionNode(UnaryOperator unaryOperator, NumericalExpressionNode numericalExpressionNode) implements NumericalExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    enum UnaryOperator {
        PLUS,
        MINUS,
        NONE
    }

    enum BinaryOperation {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO
    }

}
