package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

public interface AssignmentTargetNode {

    record VariableTarget(Variable variable) implements AssignmentTargetNode {}

    record ArrayElementTarget(NumericalExpressionNode numericalExpressionNode) implements AssignmentTargetNode {}

}
