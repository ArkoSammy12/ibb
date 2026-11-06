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

    record NumberNode(UnaryOperator unaryOperator, short value) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record ArrayReferenceNode(UnaryOperator unaryOperator, NumericExpressionNode arrayIndexExpression) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AbsFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode argumentExpression) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record AscFunctionNode(UnaryOperator unaryOperator, String argumentString) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record RndFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode argumentExpression) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SgnFunctionNode(UnaryOperator unaryOperator, NumericExpressionNode argumentExpression) implements NumericExpressionNode {

        @Override
        public UnaryOperator getUnaryOperator() {
            return this.unaryOperator();
        }

    }

    record SubExpressionNode(UnaryOperator unaryOperator, NumericExpressionNode argumentExpression) implements NumericExpressionNode {

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

        public enum Operator {
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