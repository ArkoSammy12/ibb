package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public sealed interface ExpressionNode extends ExpressionListNode {

    PrefixOperator getPrefixOperator();

    record VariableNode(PrefixOperator prefixOperator, Variable variable) implements ExpressionNode {

        @Override
        public PrefixOperator getPrefixOperator() {
            return this.prefixOperator();
        }

    }

    record NumberNode(PrefixOperator prefixOperator, long value) implements ExpressionNode {

        @Override
        public PrefixOperator getPrefixOperator() {
            return this.prefixOperator();
        }

    }

    record OperatorPrefixedExpressionNode(PrefixOperator prefixOperator, ExpressionNode expressionNode) implements ExpressionNode {

        @Override
        public PrefixOperator getPrefixOperator() {
            return this.prefixOperator();
        }

    }

    record OperationExpressionNode(PrefixOperator prefixOperator, Operation operation, ExpressionNode lhs, ExpressionNode rhs) implements ExpressionNode {

        @Override
        public PrefixOperator getPrefixOperator() {
            return this.prefixOperator();
        }

        enum Operation {
            ADD,
            SUBTRACT,
            MULTIPLY,
            DIVIDE
        }

    }

    enum PrefixOperator {
        PLUS,
        MINUS,
        NONE
    }

}
