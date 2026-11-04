package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public sealed interface NumericExpressionNode extends ExpressionNode, NumericExpressionListNode {

    UnaryOperator getUnaryOperator();

    record VariableNode(UnaryOperator unaryOperator, Variable variable) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record NumberNode(UnaryOperator unaryOperator, int value) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record ArrayReferenceNode(UnaryOperator unaryOperator, NumericExpressionNode numericExpressionNode) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AbsFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode numericExpressionNode) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AscFunctionNode(UnaryOperator unaryOperator, String contents) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record RndFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode numericExpressionNode) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SgnFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode numericExpressionNode) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SubExpressionNode(UnaryOperator unaryOperator, NumericExpressionNode numericExpressionNode) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record BinaryOperationExpressionNode(UnaryOperator unaryOperator, Operator operator, NumericExpressionNode lhs, NumericExpressionNode rhs) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

        enum Operator {
            ADD,
            SUBTRACT,
            MULTIPLY,
            DIVIDE,
            MODULO
        }

    }

    enum UnaryOperator {
        PLUS,
        MINUS,
        NONE
    }

}
