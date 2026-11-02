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
        Long lineNumber = null;
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
        if (!(trailingToken instanceof EndOfFileToken)) {
            throw new TinyBasicParseException("Trailing line token '%s'!".formatted(trailingToken.getLexeme()), trailingToken.getSourcePosition());
        }
        return new LineNode(lineNumber, statementNode);
    }

    private StatementNode parseStatement(TinyBasicParserContext context) throws TinyBasicParseException {
        Token token = context.poll();
        if (token == null) {
            throw new TinyBasicParseException("Expected statement beginning!", context.peekSourcePosition());
        }
        if (!(token instanceof KeywordToken keywordToken)) {
            throw new TinyBasicParseException("Invalid statement beginning '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        }
        return switch (keywordToken.getKeyword()) {
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
                        default -> throw new TinyBasicParseException("Unknown relational operator '%s'!".formatted(operatorToken.getLexeme()), operatorToken.getSourcePosition());
                    };
                    case Token t -> throw new TinyBasicParseException("The token '%s' is not a relational operator!".formatted(t.getLexeme()), t.getSourcePosition());
                    case null -> throw new TinyBasicParseException("Expected relational operator in 'IF' condition!", context.peekSourcePosition());
                };
                ExpressionNode rhs = this.parseExpression(context);
                if (!(context.poll() instanceof KeywordToken thenToken && thenToken.getKeyword() == Keyword.THEN)) {
                    throw new TinyBasicParseException("Expected 'then' to begin 'if' statement body!", context.peekSourcePosition());
                }
                StatementNode statementNode = this.parseStatement(context);
                yield new StatementNode.If(relationalOperation, lhs, rhs, statementNode);
            }
            case GOTO -> new StatementNode.Goto(this.parseExpression(context));
            case INPUT -> new StatementNode.Input(this.parseVariableList(context));
            case LET -> {
                ExpressionNode.VariableNode variableNode = this.parseVariable(context);
                Token middleToken = context.poll();
                if (middleToken instanceof EndOfFileToken) {
                    throw new TinyBasicParseException("Expected assignment operator '=' in variable assignment!", middleToken.getSourcePosition());
                }
                if (!(middleToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS)) {
                    throw new TinyBasicParseException("Unknown assignment operator token '%s'!".formatted(middleToken.getLexeme()), middleToken.getSourcePosition());
                }
                ExpressionNode expressionNode = this.parseExpression(context);
                yield new StatementNode.VariableAssignment(variableNode, expressionNode);
            }
            case GOSUB -> new StatementNode.Gosub(this.parseExpression(context));
            case RETURN -> new StatementNode.Return();
            case END -> new StatementNode.End();
            default -> throw new TinyBasicParseException("Invalid statement beginning '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
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

    private Collection<ExpressionNode.VariableNode> parseVariableList(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<ExpressionNode.VariableNode> variableNodes = new ArrayList<>();
        variableNodes.add(this.parseVariable(context));
        while (context.peek() instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COMMA) {
            context.poll();
            variableNodes.add(this.parseVariable(context));
        }
        return variableNodes;
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
            case DelimiterToken delimiterToken -> {
                if (delimiterToken.getDelimiter() != Delimiter.LEFT_PARENTHESIS) {
                    throw new TinyBasicParseException("Expected '(' to begin expression!", delimiterToken.getSourcePosition());
                }
                ExpressionNode expressionNode = new ExpressionNode.OperatorPrefixedExpressionNode(prefixOperator, this.parseExpression(context));
                Token closing = context.poll();
                if (closing instanceof EndOfFileToken) {
                    throw new TinyBasicParseException("Expected ')' to end expression!", context.peekSourcePosition());
                }
                if (!(closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS)) {
                    throw new TinyBasicParseException("Expected ')' to end expression, but found '%s'!".formatted(closing.getLexeme()), closing.getSourcePosition());
                }
                yield expressionNode;
            }
            case Token t -> throw new TinyBasicParseException("Invalid factor token '%s'".formatted(t.getLexeme()), t.getSourcePosition());
            case null -> throw new TinyBasicParseException("Expected variable, number or expression!", context.peekSourcePosition());
        };
    }

    private ExpressionNode.VariableNode parseVariable(TinyBasicParserContext context) throws TinyBasicParseException {
        Token token = context.poll();
        if (token == null) {
            throw new TinyBasicParseException("Expected variable!", context.peekSourcePosition());
        } else if (token instanceof VariableToken variableToken) {
            return new ExpressionNode.VariableNode(ExpressionNode.PrefixOperator.NONE, variableToken.getVariable());
        } else {
            throw new TinyBasicParseException("The token '%s' is not a variable!".formatted(token.getLexeme()), token.getSourcePosition());
        }
    }

}