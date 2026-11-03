package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.exception.TinyBasicParseException;
import io.github.arkosammy12.ibb.core.grammar.Delimiter;
import io.github.arkosammy12.ibb.core.grammar.Keyword;
import io.github.arkosammy12.ibb.core.grammar.Operator;
import io.github.arkosammy12.ibb.core.lexer.*;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourceStream;

import java.util.ArrayList;
import java.util.Collection;

public final class TinyBasicParser {

    public ParseResult<TinyBasicParseValue, TinyBasicParseError> parse(SourceStream<Token> tokenStream) {
        TinyBasicParserContext context = new TinyBasicParserContext(tokenStream);
        try {
            if (context.hasTokens()) {
                return new ParseResult.Ok<>(new TinyBasicParseValue(this.parseLine(context)));
            }
            return new ParseResult.Error<>(new TinyBasicParseError("The line cannot be empty!"));
        } catch (TinyBasicParseException tinyBasicParseException) {
            return new ParseResult.Error<>(new TinyBasicParseError(tinyBasicParseException.getMessage(), tinyBasicParseException.getSourcePosition().orElse(null)));
        }
    }

    private LineNode parseLine(TinyBasicParserContext context) throws TinyBasicParseException {
        Token lineNumberToken = context.peek();
        Integer lineNumber = null;
        if (lineNumberToken instanceof NumberToken numberToken) {
            context.poll();
            if (numberToken.getValue() >= 0) {
                lineNumber = numberToken.getValue();
            } else {
                throw new TinyBasicParseException("The line number '%d' cannot be negative!".formatted(numberToken.getValue()), numberToken.getSourcePosition());
            }
        }
        StatementNode statementNode = this.parseStatement(context);
        Token trailingToken = context.peek();
        if (!(trailingToken instanceof EndOfLineToken)) {
            throw new TinyBasicParseException("Trailing line token '%s'!".formatted(trailingToken.getLexeme()), trailingToken.getSourcePosition());
        }
        return new LineNode(lineNumber, statementNode);
    }

    private StatementNode parseStatement(TinyBasicParserContext context) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> this.parseVariableAssignment(context, new AssignmentTargetNode.VariableTarget(variableToken.getVariable()));
            case ArraySigilToken _ -> this.parseVariableAssignment(context, new AssignmentTargetNode.ArrayElementTarget(this.parseParenthesizedExpression(context, "array reference expression")));
            case KeywordToken keywordToken -> switch (keywordToken.getKeyword()) {
                case PRINT -> new StatementNode.Print(this.parseExpressionList(context));
                case IF -> {
                    ExpressionNode lhs = this.parseExpression(context);
                    StatementNode.If.RelationalOperation relationalOperation = switch (context.poll()) {
                        case OperatorToken operatorToken -> switch (operatorToken.getOperator()) {
                            case LESS_THAN ->  StatementNode.If.RelationalOperation.LESS_THAN;
                            case LESS_THAN_OR_EQUALS -> StatementNode.If.RelationalOperation.LESS_THAN_OR_EQUALS;
                            case GREATER_THAN -> StatementNode.If.RelationalOperation.GREATER_THAN;
                            case GREATER_THAN_OR_EQUALS -> StatementNode.If.RelationalOperation.GREATER_THAN_OR_EQUALS;
                            case EQUALS -> StatementNode.If.RelationalOperation.EQUALS;
                            case NOT_EQUALS_CONVERGENT, NOT_EQUALS_DIVERGENT -> StatementNode.If.RelationalOperation.NOT_EQUALS;
                            default -> throw new TinyBasicParseException("Expected relational operator, found '%s'!".formatted(operatorToken.getLexeme()), operatorToken.getSourcePosition());
                        };
                        case Token token -> throw new TinyBasicParseException("Expected relational operator, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
                    };
                    ExpressionNode rhs = this.parseExpression(context);
                    Token thenToken = context.poll();
                    if (!(thenToken instanceof KeywordToken thenKeywordToken && thenKeywordToken.getKeyword() == Keyword.THEN)) {
                        throw new TinyBasicParseException("Expected 'then' to begin 'if' statement body, found '%s'!".formatted(thenToken.getLexeme()), thenToken.getSourcePosition());
                    }
                    StatementNode statementNode = this.parseStatement(context);
                    yield new StatementNode.If(relationalOperation, lhs, rhs, statementNode);
                }
                case GOTO -> new StatementNode.Goto(this.parseExpression(context));
                case INPUT -> new StatementNode.Input(this.parseAssignmentTargetList(context));
                case LET -> this.parseVariableAssignment(context, this.parseAssignmentTarget(context));
                case GOSUB -> new StatementNode.Gosub(this.parseExpression(context));
                case RETURN -> new StatementNode.Return();
                case END -> new StatementNode.End();
                default -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(keywordToken.getLexeme()), keywordToken.getSourcePosition());
            };
            case EndOfLineToken endOfLineToken -> throw new TinyBasicParseException("Expected start of statement!", endOfLineToken.getSourcePosition());
            case Token token -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private StatementNode.VariableAssignment parseVariableAssignment(TinyBasicParserContext context, AssignmentTargetNode assignmentTargetNode) throws TinyBasicParseException {
        Token middleToken = context.poll();
        if (!(middleToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS)) {
            throw new TinyBasicParseException("Expected assignment operator '=', found '%s!".formatted(middleToken.getLexeme()), middleToken.getSourcePosition());
        }
        ExpressionNode expressionNode = this.parseExpression(context);
        return new StatementNode.VariableAssignment(assignmentTargetNode, expressionNode);
    }

    private Collection<ExpressionListNode> parseExpressionList(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<ExpressionListNode> expressionListNodes = new ArrayList<>();
        expressionListNodes.add(this.parseExpressionListNode(context));
        while (context.peek() instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COMMA) {
            context.poll();
            expressionListNodes.add(this.parseExpressionListNode(context));
        }
        return expressionListNodes;
    }

    private ExpressionListNode parseExpressionListNode(TinyBasicParserContext context) throws TinyBasicParseException {
        Token token = context.peek();
        if (token instanceof StringToken stringToken) {
            context.poll();
            return new StringNode(stringToken.getContents());
        }
        return this.parseExpression(context);
    }

    private Collection<AssignmentTargetNode> parseAssignmentTargetList(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<AssignmentTargetNode> assignmentTargetNodes = new ArrayList<>();
        assignmentTargetNodes.add(this.parseAssignmentTarget(context));
        while (context.peek() instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COMMA) {
            context.poll();
            assignmentTargetNodes.add(this.parseAssignmentTarget(context));
        }
        return assignmentTargetNodes;
    }

    private ExpressionNode parseExpression(TinyBasicParserContext context) throws TinyBasicParseException {
        Token token = context.peek();
        ExpressionNode.PrefixOperator prefixOperator = ExpressionNode.PrefixOperator.NONE;
        if (token instanceof OperatorToken operatorToken) {
            switch (operatorToken.getOperator()) {
                case PLUS -> {
                    context.poll();
                    prefixOperator = ExpressionNode.PrefixOperator.PLUS;
                }
                case MINUS -> {
                    context.poll();
                    prefixOperator = ExpressionNode.PrefixOperator.MINUS;
                }
            }
        }
        ExpressionNode lhs = this.parseTerm(context, prefixOperator);
        return this.parseExpressionRest(context, lhs);
    }

    private ExpressionNode parseExpressionRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token rhsOperator = context.peek();
        ExpressionNode.OperationExpressionNode.Operation operation = switch (rhsOperator) {
            case OperatorToken rhsOperatorToken -> switch (rhsOperatorToken.getOperator()) {
                case PLUS -> ExpressionNode.OperationExpressionNode.Operation.ADD;
                case MINUS -> ExpressionNode.OperationExpressionNode.Operation.SUBTRACT;
                default -> null;
            };
            default -> null;
        };
        if (operation == null) {
            return lhs;
        }
        context.poll();
        ExpressionNode rhs = this.parseTerm(context, ExpressionNode.PrefixOperator.NONE);
        return this.parseExpressionRest(context, new ExpressionNode.OperationExpressionNode(ExpressionNode.PrefixOperator.NONE, operation, lhs, rhs));
    }

    private ExpressionNode parseTerm(TinyBasicParserContext context, ExpressionNode.PrefixOperator prefixOperator) throws TinyBasicParseException {
        ExpressionNode lhs = this.parseFactor(context, prefixOperator);
        return this.parseTermRest(context, lhs);
    }

    private ExpressionNode parseTermRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token rhsOperator = context.peek();
        ExpressionNode.OperationExpressionNode.Operation operation = switch (rhsOperator) {
            case OperatorToken rhsOperatorToken -> switch (rhsOperatorToken.getOperator()) {
                case TIMES -> ExpressionNode.OperationExpressionNode.Operation.MULTIPLY;
                case DIVIDE -> ExpressionNode.OperationExpressionNode.Operation.DIVIDE;
                default -> null;
            };
            default -> null;
        };
        if (operation == null) {
            return lhs;
        }
        context.poll();
        ExpressionNode rhs = this.parseFactor(context, ExpressionNode.PrefixOperator.NONE);
        return this.parseTermRest(context, new ExpressionNode.OperationExpressionNode(ExpressionNode.PrefixOperator.NONE, operation, lhs, rhs));
    }

    private ExpressionNode parseFactor(TinyBasicParserContext context, ExpressionNode.PrefixOperator prefixOperator) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> new ExpressionNode.VariableNode(prefixOperator, variableToken.getVariable());
            case NumberToken numberToken -> new ExpressionNode.NumberNode(prefixOperator, numberToken.getValue());
            case ArraySigilToken _ -> new ExpressionNode.ArrayReferenceNode(prefixOperator, this.parseParenthesizedExpression(context, "array reference expression"));
            case DelimiterToken delimiterToken -> {
                if (delimiterToken.getDelimiter() != Delimiter.LEFT_PARENTHESIS) {
                    throw new TinyBasicParseException("Expected '(' to begin expression, found '%s!".formatted(delimiterToken.getLexeme()), delimiterToken.getSourcePosition());
                }
                ExpressionNode expressionNode = new ExpressionNode.OperatorPrefixedExpressionNode(prefixOperator, this.parseExpression(context));
                Token closing = context.poll();
                if (!(closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS)) {
                    throw new TinyBasicParseException("Expected ')' to end expression, found '%s'!".formatted(closing.getLexeme()), closing.getSourcePosition());
                }
                yield expressionNode;
            }
            case KeywordToken keywordToken -> switch (keywordToken.getKeyword()) {
                case ASC -> {
                    Token arg = context.poll();
                    if (!(arg instanceof StringToken stringArg)) {
                        throw new TinyBasicParseException("Expected string literal argument for ASC() function, found '%s'!".formatted(arg.getLexeme()), arg.getSourcePosition());
                    }
                    yield new ExpressionNode.AscFunctionNode(prefixOperator, stringArg.getContents());
                }
                case ABS -> new ExpressionNode.AbsFunctionNode(prefixOperator, this.parseParenthesizedExpression(context, "ABS() expression argument"));
                case RND -> new ExpressionNode.RndFunctionNode(prefixOperator, this.parseParenthesizedExpression(context, "RND() expression argument"));
                case SGN -> new ExpressionNode.SgnFunctionNode(prefixOperator, this.parseParenthesizedExpression(context, "SGN() expression argument"));
                default -> throw new TinyBasicParseException("Expected variable, number, array reference, function or expression enclosing in parenthesis, found '%s'!".formatted(keywordToken.getLexeme()), keywordToken.getSourcePosition());
            };
            case Token token -> throw new TinyBasicParseException("Expected variable, number, array reference, function or expression enclosing in parenthesis, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private AssignmentTargetNode parseAssignmentTarget(TinyBasicParserContext context) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> new AssignmentTargetNode.VariableTarget(variableToken.getVariable());
            case ArraySigilToken _ -> new AssignmentTargetNode.ArrayElementTarget(this.parseParenthesizedExpression(context, "array reference expression"));
            case Token token -> throw new TinyBasicParseException("Expected variable or array reference, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private ExpressionNode parseParenthesizedExpression(TinyBasicParserContext context, String expected) throws TinyBasicParseException {
        Token opening = context.poll();
        if (!(opening instanceof DelimiterToken openingDelimiter && openingDelimiter.getDelimiter() == Delimiter.LEFT_PARENTHESIS)) {
            throw new TinyBasicParseException("Expected '(' to begin %s, found '%s'!".formatted(expected, opening.getLexeme()), opening.getSourcePosition());
        }
        ExpressionNode expressionNode = this.parseExpression(context);
        Token closing = context.poll();
        if (!(closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS)) {
            throw new TinyBasicParseException("Expected ')' to end %s, found '%s'!".formatted(expected, closing.getLexeme()), opening.getSourcePosition());
        }
        return expressionNode;
    }

}