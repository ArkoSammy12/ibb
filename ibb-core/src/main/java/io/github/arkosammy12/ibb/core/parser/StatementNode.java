package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.grammar.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public sealed interface StatementNode {

    record Assignment(AssignmentTargetNode assignmentTargetNode, ExpressionNode expressionNode) implements StatementNode {}

    record Goto(ExpressionNode targetExpression) implements StatementNode {}

    record GoSub(ExpressionNode targetExpression) implements StatementNode {}

    record Return() implements StatementNode {}

    record If(ExpressionNode conditionalExpression, StatementNode statement) implements StatementNode {}

    record Next(Variable loopVariable) implements StatementNode {}

    record Rem() implements StatementNode {}

    record End() implements StatementNode {}

    record EmptyPrint() implements StatementNode {}

    record Print(Element firstPrintElement, Collection<Element> extraPrintElements) implements StatementNode {

        public Print {
            extraPrintElements = List.copyOf(extraPrintElements);
        }

        public record Element(ExpressionListNode expressionListNode, FormattingDelimiter suffixDelimiter) {}

        public enum FormattingDelimiter {
            COMMA,
            SEMICOLON,
            NONE
        }

    }

    final class Input implements StatementNode {

        private final String prompt;
        private final AssignmentTargetNode firstAssignmentTargetNode;
        private final Collection<AssignmentTargetNode> extraAssignmentTargetNodes;

        public Input(String prompt, AssignmentTargetNode firstAssignmentTargetNode, Collection<AssignmentTargetNode> extraAssignmentTargetNodes) {
            this.prompt = prompt;
            this.firstAssignmentTargetNode = firstAssignmentTargetNode;
            this.extraAssignmentTargetNodes = List.copyOf(extraAssignmentTargetNodes);
        }

        public Optional<String> getPrompt() {
            return Optional.ofNullable(this.prompt);
        }

        public AssignmentTargetNode getFirstAssignmentTargetNode() {
            return this.firstAssignmentTargetNode;
        }

        public Collection<AssignmentTargetNode> getExtraAssignmentTargetNodes() {
            return this.extraAssignmentTargetNodes;
        }

        @Override
        public String toString() {
            return "Input[prompt=%s, firstAssignmentTargetNode=%s, extraAssignmentTargetNodes=[%s]]".formatted(this.prompt == null ? "" : this.prompt, this.firstAssignmentTargetNode.toString(), this.extraAssignmentTargetNodes.stream().map(AssignmentTargetNode::toString).collect(Collectors.joining(", ")));
        }

    }

    final class For implements StatementNode {

        private final Variable loopVariable;
        private final ExpressionNode startValueExpression;
        private final ExpressionNode endValueExpression;
        private final ExpressionNode stepValueExpression;

        public For(Variable loopVariable, ExpressionNode startValueExpression, ExpressionNode endValueExpression, ExpressionNode stepValueExpression) {
            this.loopVariable = loopVariable;
            this.startValueExpression = startValueExpression;
            this.endValueExpression = endValueExpression;
            this.stepValueExpression = stepValueExpression;
        }

        public Variable getLoopVariable() {
            return this.loopVariable;
        }

        public ExpressionNode getStartValueExpression() {
            return this.startValueExpression;
        }

        public ExpressionNode getEndValueExpression() {
            return this.endValueExpression;
        }

        public Optional<ExpressionNode> getStepValueExpression() {
            return Optional.ofNullable(this.stepValueExpression);
        }

    }

}