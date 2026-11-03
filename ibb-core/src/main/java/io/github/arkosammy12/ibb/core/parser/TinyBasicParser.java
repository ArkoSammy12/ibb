package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.exception.TinyBasicParseException;
import io.github.arkosammy12.ibb.core.grammar.Delimiter;
import io.github.arkosammy12.ibb.core.grammar.Keyword;
import io.github.arkosammy12.ibb.core.grammar.Operator;
import io.github.arkosammy12.ibb.core.grammar.Variable;
import io.github.arkosammy12.ibb.core.lexer.*;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourceStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

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
        StatementNode firstStatementNode = this.parseStatement(context);
        Collection<StatementNode> extraStatementNodes = new ArrayList<>();
        while (context.peek() instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COLON) {
            context.poll();
            extraStatementNodes.add(this.parseStatement(context));
        }
        switch (context.peek()) {
            case EndOfLineToken _ -> {}
            case KeywordToken trailingKeywordToken when trailingKeywordToken.getKeyword() == Keyword.REM -> this.consumeRemainingTokens(context);
            case Token trailingToken -> throw new TinyBasicParseException("Trailing line token '%s'!".formatted(trailingToken.getLexeme()), trailingToken.getSourcePosition());
        }
        return new LineNode(lineNumber, firstStatementNode, extraStatementNodes);
    }

    private StatementNode parseStatement(TinyBasicParserContext context) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> this.parseVariableAssignment(context, new AssignmentTargetNode.VariableTarget(variableToken.getVariable()));
            case ArraySigilToken _ -> this.parseVariableAssignment(context, new AssignmentTargetNode.ArrayElementTarget(this.parseParenthesizedExpression(context, "array reference expression")));
            case KeywordToken keywordToken -> switch (keywordToken.getKeyword()) {
                case PRINT -> new StatementNode.Print(this.parseExpressionListNode(context), this.parseExpressionList(context));
                case GOTO -> new StatementNode.Goto(this.parseExpression(context));
                case INPUT -> new StatementNode.Input(this.parseAssignmentTarget(context), this.parseAssignmentTargetList(context));
                case LET -> this.parseVariableAssignment(context, this.parseAssignmentTarget(context));
                case GOSUB -> new StatementNode.GoSub(this.parseExpression(context));
                case RETURN -> new StatementNode.Return();
                case END -> new StatementNode.End();
                case NEXT -> new StatementNode.Next(this.parseVariable(context));
                case REM -> {
                    this.consumeRemainingTokens(context);
                    yield new StatementNode.Rem();
                }
                case FOR -> {
                    Variable variable = this.parseVariable(context);
                    this.expect(context, assignmentToken -> assignmentToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS, "assignment operator '='");
                    NumericalExpressionNode startingNumericalExpressionNodeValue = this.parseExpression(context);
                    this.expect(context, toToken -> toToken instanceof KeywordToken toKeywordToken && toKeywordToken.getKeyword() == Keyword.TO, "range separator 'to'");
                    NumericalExpressionNode finalNumericalExpressionNodeValue = this.parseExpression(context);
                    yield new StatementNode.For(variable, startingNumericalExpressionNodeValue, finalNumericalExpressionNodeValue);
                }
                case IF -> {
                    NumericalExpressionNode lhs = this.parseExpression(context);
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
                    NumericalExpressionNode rhs = this.parseExpression(context);
                    this.expect(context, thenToken -> thenToken instanceof KeywordToken thenKeywordToken && thenKeywordToken.getKeyword() == Keyword.THEN, "'then' to begin 'if' statement body");
                    StatementNode statementNode = this.parseStatement(context);
                    yield new StatementNode.If(relationalOperation, lhs, rhs, statementNode);
                }
                default -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(keywordToken.getLexeme()), keywordToken.getSourcePosition());
            };
            case EndOfLineToken endOfLineToken -> throw new TinyBasicParseException("Expected start of statement!", endOfLineToken.getSourcePosition());
            case Token token -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private StatementNode.VariableAssignment parseVariableAssignment(TinyBasicParserContext context, AssignmentTargetNode assignmentTargetNode) throws TinyBasicParseException {
        this.expect(context, middleToken -> middleToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS, "assignment operator '='");
        NumericalExpressionNode numericalExpressionNode = this.parseExpression(context);
        return new StatementNode.VariableAssignment(assignmentTargetNode, numericalExpressionNode);
    }

    private Collection<ExpressionListNode> parseExpressionList(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<ExpressionListNode> expressionListNodes = new ArrayList<>();
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
        while (context.peek() instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COMMA) {
            context.poll();
            assignmentTargetNodes.add(this.parseAssignmentTarget(context));
        }
        return assignmentTargetNodes;
    }

    private NumericalExpressionNode parseExpression(TinyBasicParserContext context) throws TinyBasicParseException {
        Token token = context.peek();
        NumericalExpressionNode.UnaryOperator unaryOperator = NumericalExpressionNode.UnaryOperator.NONE;
        if (token instanceof OperatorToken operatorToken) {
            switch (operatorToken.getOperator()) {
                case PLUS -> {
                    context.poll();
                    unaryOperator = NumericalExpressionNode.UnaryOperator.PLUS;
                }
                case MINUS -> {
                    context.poll();
                    unaryOperator = NumericalExpressionNode.UnaryOperator.MINUS;
                }
            }
        }
        NumericalExpressionNode lhs = this.parseTerm(context, unaryOperator);
        return this.parseExpressionRest(context, lhs);
    }

    private NumericalExpressionNode parseExpressionRest(TinyBasicParserContext context, NumericalExpressionNode lhs) throws TinyBasicParseException {
        Token rhsOperator = context.peek();
        NumericalExpressionNode.BinaryOperation binaryOperation = switch (rhsOperator) {
            case OperatorToken rhsOperatorToken -> switch (rhsOperatorToken.getOperator()) {
                case PLUS -> NumericalExpressionNode.BinaryOperation.ADD;
                case MINUS -> NumericalExpressionNode.BinaryOperation.SUBTRACT;
                default -> null;
            };
            default -> null;
        };
        if (binaryOperation == null) {
            return lhs;
        }
        context.poll();
        NumericalExpressionNode rhs = this.parseTerm(context, NumericalExpressionNode.UnaryOperator.NONE);
        return this.parseExpressionRest(context, new NumericalExpressionNode.OperationExpressionNode(NumericalExpressionNode.UnaryOperator.NONE, binaryOperation, lhs, rhs));
    }

    private NumericalExpressionNode parseTerm(TinyBasicParserContext context, NumericalExpressionNode.UnaryOperator unaryOperator) throws TinyBasicParseException {
        NumericalExpressionNode lhs = this.parseFactor(context, unaryOperator);
        return this.parseTermRest(context, lhs);
    }

    private NumericalExpressionNode parseTermRest(TinyBasicParserContext context, NumericalExpressionNode lhs) throws TinyBasicParseException {
        Token rhsOperator = context.peek();
        NumericalExpressionNode.BinaryOperation binaryOperation = switch (rhsOperator) {
            case OperatorToken rhsOperatorToken -> switch (rhsOperatorToken.getOperator()) {
                case TIMES -> NumericalExpressionNode.BinaryOperation.MULTIPLY;
                case DIVIDE -> NumericalExpressionNode.BinaryOperation.DIVIDE;
                case MOD -> NumericalExpressionNode.BinaryOperation.MODULO;
                default -> null;
            };
            default -> null;
        };
        if (binaryOperation == null) {
            return lhs;
        }
        context.poll();
        NumericalExpressionNode rhs = this.parseFactor(context, NumericalExpressionNode.UnaryOperator.NONE);
        return this.parseTermRest(context, new NumericalExpressionNode.OperationExpressionNode(NumericalExpressionNode.UnaryOperator.NONE, binaryOperation, lhs, rhs));
    }

    private NumericalExpressionNode parseFactor(TinyBasicParserContext context, NumericalExpressionNode.UnaryOperator unaryOperator) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> new NumericalExpressionNode.VariableNode(unaryOperator, variableToken.getVariable());
            case NumberToken numberToken -> new NumericalExpressionNode.NumberNode(unaryOperator, numberToken.getValue());
            case ArraySigilToken _ -> new NumericalExpressionNode.ArrayReferenceNode(unaryOperator, this.parseParenthesizedExpression(context, "array reference expression"));
            case DelimiterToken delimiterToken -> {
                if (delimiterToken.getDelimiter() != Delimiter.LEFT_PARENTHESIS) {
                    throw new TinyBasicParseException("Expected '(' to begin expression, found '%s!".formatted(delimiterToken.getLexeme()), delimiterToken.getSourcePosition());
                }
                NumericalExpressionNode numericalExpressionNode = new NumericalExpressionNode.SubExpressionNode(unaryOperator, this.parseExpression(context));
                this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end expression");
                yield numericalExpressionNode;
            }
            case KeywordToken keywordToken -> switch (keywordToken.getKeyword()) {
                case ASC -> {
                    this.expect(context, opening -> opening instanceof DelimiterToken openingDelimiter && openingDelimiter.getDelimiter() == Delimiter.LEFT_PARENTHESIS, "'(' to begin ASC() expression argument");
                    String arg = switch (context.poll()) {
                        case StringToken stringToken -> stringToken.getContents();
                        case Token token -> throw new TinyBasicParseException("Expected string literal argument for ASC() function, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
                    };
                    this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end ASC() expression argument");
                    yield new NumericalExpressionNode.AscFunctionNode(unaryOperator, arg);
                }
                case ABS -> new NumericalExpressionNode.AbsFunctionNode(unaryOperator, this.parseParenthesizedExpression(context, "ABS() expression argument"));
                case RND -> new NumericalExpressionNode.RndFunctionNode(unaryOperator, this.parseParenthesizedExpression(context, "RND() expression argument"));
                case SGN -> new NumericalExpressionNode.SgnFunctionNode(unaryOperator, this.parseParenthesizedExpression(context, "SGN() expression argument"));
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

    private NumericalExpressionNode parseParenthesizedExpression(TinyBasicParserContext context, String expected) throws TinyBasicParseException {
        this.expect(context, opening -> opening instanceof DelimiterToken openingDelimiter && openingDelimiter.getDelimiter() == Delimiter.LEFT_PARENTHESIS, "'(' to begin %s".formatted(expected));
        NumericalExpressionNode numericalExpressionNode = this.parseExpression(context);
        this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end %s".formatted(expected));
        return numericalExpressionNode;
    }

    private Variable parseVariable(TinyBasicParserContext context) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> variableToken.getVariable();
            case Token token -> throw new TinyBasicParseException("Expected variable, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private void expect(TinyBasicParserContext context, Predicate<Token> predicate, String expected) throws TinyBasicParseException {
        Token token = context.poll();
        if (!predicate.test(token)) {
            throw new TinyBasicParseException("Expected %s, found '%s'!".formatted(expected, token.getLexeme()), token.getSourcePosition());
        }
    }

    private void consumeRemainingTokens(TinyBasicParserContext context) {
        while (context.hasTokens()) {
            context.poll();
        }
    }

}