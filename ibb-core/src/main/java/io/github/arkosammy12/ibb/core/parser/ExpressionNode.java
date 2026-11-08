package io.github.arkosammy12.ibb.core.parser;

public sealed interface ExpressionNode extends ExpressionListNode permits NumericExpressionNode, BooleanExpressionNode {}