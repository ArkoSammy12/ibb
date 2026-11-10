package io.github.arkosammy12.ibb.core.parser;

public sealed interface ExpressionListNode permits ExpressionListNode.PrintZoneModifier, ExpressionNode, StringNode {

    record PrintZoneModifier(ExpressionNode expressionNode) implements ExpressionListNode {}

}