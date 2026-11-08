package io.github.arkosammy12.ibb.core.parser;

import io.github.arkosammy12.ibb.core.exception.TinyBasicParseException;
import io.github.arkosammy12.ibb.core.grammar.Delimiter;
import io.github.arkosammy12.ibb.core.grammar.Keyword;
import io.github.arkosammy12.ibb.core.grammar.Operator;
import io.github.arkosammy12.ibb.core.grammar.Variable;
import io.github.arkosammy12.ibb.core.lexer.*;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
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
        Token upcomingToken = context.peek();
        Integer lineNumber = null;
        if (upcomingToken instanceof NumberToken lineNumberToken) {
            context.poll();
            if (lineNumberToken.getValue() >= 0) {
                lineNumber = lineNumberToken.getValue();
            } else {
                throw new TinyBasicParseException("The line number '%d' cannot be negative!".formatted(lineNumberToken.getValue()), lineNumberToken.getSourcePosition());
            }
        }
        StatementNode firstStatementNode = this.parseStatement(context);
        Collection<StatementNode> extraStatementNodes = new ArrayList<>();
        while (context.peek() instanceof DelimiterToken statementDelimiterToken && statementDelimiterToken.getDelimiter() == Delimiter.COLON) {
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
                case PRINT -> switch (context.peek()) {
                    case EndOfLineToken _ -> new StatementNode.EmptyPrint();
                    case DelimiterToken delimiterToken when delimiterToken.getDelimiter() == Delimiter.COLON -> new StatementNode.EmptyPrint();
                    default -> new StatementNode.Print(this.parsePrintElement(context), this.parsePrintElements(context));
                };
                case GOTO -> new StatementNode.Goto(this.parseExpression(context));
                case LET -> this.parseVariableAssignment(context, this.parseAssignmentTarget(context));
                case GOSUB -> new StatementNode.GoSub(this.parseExpression(context));
                case RETURN -> new StatementNode.Return();
                case END -> new StatementNode.End();
                case NEXT -> new StatementNode.Next(this.parseVariable(context));
                case REM -> {
                    this.consumeRemainingTokens(context);
                    yield new StatementNode.Rem();
                }
                case INPUT -> {
                    String prompt = null;
                    if (context.peek() instanceof StringToken promptToken) {
                        context.poll();
                        prompt = promptToken.getContents();
                        this.expect(context, token -> token instanceof DelimiterToken delimiterToken && delimiterToken.getDelimiter() == Delimiter.COMMA, "comma after 'input' prompt");
                    }
                    yield new StatementNode.Input(prompt, this.parseAssignmentTarget(context), this.parseAssignmentTargetList(context));
                }
                case IF -> {
                    ExpressionNode conditionalExpressionNode = this.parseExpression(context);
                    yield switch (context.poll()) {
                        case KeywordToken ifKeyword -> switch (ifKeyword.getKeyword()) {
                            case THEN -> new StatementNode.If(conditionalExpressionNode, this.parseStatement(context));
                            case GOTO -> new StatementNode.If(conditionalExpressionNode, new StatementNode.Goto(this.parseExpression(context)));
                            case GOSUB -> new StatementNode.If(conditionalExpressionNode, new StatementNode.GoSub(this.parseExpression(context)));
                            default -> throw new TinyBasicParseException("Expected 'then', 'goto' or 'gosub' to begin 'if' statement body!", ifKeyword.getSourcePosition());
                        };
                        case Token token -> throw new TinyBasicParseException("Expected 'then', 'goto' or 'gosub' to begin 'if' statement body!", token.getSourcePosition());
                    };
                }
                case FOR -> {
                    Variable loopVariable = this.parseVariable(context);
                    this.expect(context, assignmentToken -> assignmentToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS, "assignment operator '='");
                    ExpressionNode startValueExpression = this.parseExpression(context);
                    this.expect(context, toToken -> toToken instanceof KeywordToken toKeywordToken && toKeywordToken.getKeyword() == Keyword.TO, "range separator 'to'");
                    ExpressionNode endValueExpression = this.parseExpression(context);
                    yield new StatementNode.For(loopVariable, startValueExpression, endValueExpression);
                }
                default -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(keywordToken.getLexeme()), keywordToken.getSourcePosition());
            };
            case EndOfLineToken _ -> new StatementNode.Rem();
            case Token token -> throw new TinyBasicParseException("Illegal start of statement '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

    private StatementNode.Assignment parseVariableAssignment(TinyBasicParserContext context, AssignmentTargetNode assignmentTargetNode) throws TinyBasicParseException {
        this.expect(context, middleToken -> middleToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.EQUALS, "assignment operator '='");
        ExpressionNode expressionNode = this.parseExpression(context);
        return new StatementNode.Assignment(assignmentTargetNode, expressionNode);
    }

    private Collection<StatementNode.Print.Element> parsePrintElements(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<StatementNode.Print.Element> extraPrintElements = new ArrayList<>();
        expressionLoop: while (context.peek() instanceof DelimiterToken upcomingDelimiterToken && (upcomingDelimiterToken.getDelimiter() == Delimiter.COMMA || upcomingDelimiterToken.getDelimiter() == Delimiter.SEMICOLON)) {
            context.poll();
            switch (context.peek()) {
                case DelimiterToken delimiterToken when delimiterToken.getDelimiter() == Delimiter.COLON -> {
                    break expressionLoop;
                }
                case EndOfLineToken _ -> {
                    break expressionLoop;
                }
                default -> {}
            }
            extraPrintElements.add(this.parsePrintElement(context));
        }
        return extraPrintElements;
    }

    private StatementNode.Print.Element parsePrintElement(TinyBasicParserContext context) throws TinyBasicParseException {
        ExpressionListNode expressionListNode = switch (context.peek()) {
            case StringToken stringToken -> {
                context.poll();
                yield new StringNode(stringToken.getContents());
            }
            default -> this.parseExpression(context);
        };
        StatementNode.Print.FormattingDelimiter formattingDelimiter = switch (context.peek()) {
            case DelimiterToken delimiterToken -> switch (delimiterToken.getDelimiter()) {
                case COMMA -> StatementNode.Print.FormattingDelimiter.COMMA;
                case SEMICOLON -> StatementNode.Print.FormattingDelimiter.SEMICOLON;
                default -> StatementNode.Print.FormattingDelimiter.NONE;
            };
            default -> StatementNode.Print.FormattingDelimiter.NONE;
        };
        return new StatementNode.Print.Element(expressionListNode, formattingDelimiter);
    }

    private Collection<AssignmentTargetNode> parseAssignmentTargetList(TinyBasicParserContext context) throws TinyBasicParseException {
        Collection<AssignmentTargetNode> assignmentTargetNodes = new ArrayList<>();
        while (context.peek() instanceof DelimiterToken assignmentTargetDelimiterToken && assignmentTargetDelimiterToken.getDelimiter() == Delimiter.COMMA) {
            context.poll();
            assignmentTargetNodes.add(this.parseAssignmentTarget(context));
        }
        return assignmentTargetNodes;
    }

    private AssignmentTargetNode parseAssignmentTarget(TinyBasicParserContext context) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> new AssignmentTargetNode.VariableTarget(variableToken.getVariable());
            case ArraySigilToken _ -> new AssignmentTargetNode.ArrayElementTarget(this.parseParenthesizedExpression(context, "array reference expression"));
            case Token token -> throw new TinyBasicParseException("Expected variable or array reference, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
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

    private ExpressionNode parseParenthesizedExpression(TinyBasicParserContext context, String expected) throws TinyBasicParseException {
        this.expect(context, opening -> opening instanceof DelimiterToken openingDelimiter && openingDelimiter.getDelimiter() == Delimiter.LEFT_PARENTHESIS, "'(' to begin %s".formatted(expected));
        ExpressionNode expressionNode = this.parseExpression(context);
        this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end %s".formatted(expected));
        return expressionNode;
    }

    private void consumeRemainingTokens(TinyBasicParserContext context) {
        while (context.hasTokens()) {
            context.poll();
        }
    }

    private NumericExpressionNode parseAndExpectNumericExpression(TinyBasicParserContext context) throws TinyBasicParseException {
        SourcePosition expressionStartPosition = context.peek().getSourcePosition();
        return switch (this.parseNumericExpression(context)) {
            case NumericExpressionNode numericExpressionNode -> numericExpressionNode;
            case BooleanExpressionNode _ -> throw new TinyBasicParseException("Expected numeric expression!", expressionStartPosition);
        };
    }

    private ExpressionNode parseExpression(TinyBasicParserContext context) throws TinyBasicParseException {
        return this.parseExpressionRest(context, this.parseBooleanExpressionTerm(context));
    }

    private ExpressionNode parseExpressionRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token upcomingToken = context.peek();
        BooleanExpressionNode.BinaryOperator booleanBinaryOperator = switch (upcomingToken) {
            case OperatorToken operatorToken when operatorToken.getOperator() == Operator.OR -> BooleanExpressionNode.BinaryOperator.OR;
            default -> null;
        };
        if (booleanBinaryOperator == null) {
            return lhs;
        }
        return switch (lhs) {
            case BooleanExpressionNode lhsBooleanExpressionNode -> {
                context.poll();
                BooleanExpressionNode rhsBooleanExpressionNode = switch (this.parseBooleanExpressionTerm(context)) {
                    case BooleanExpressionNode booleanExpressionNode -> booleanExpressionNode;
                    case NumericExpressionNode _ -> throw new TinyBasicParseException("Cannot apply operator '%s' to numeric expression!".formatted(upcomingToken.getLexeme()), upcomingToken.getSourcePosition());
                };
                yield this.parseExpressionRest(context, new BooleanExpressionNode.BinaryBooleanOperation(booleanBinaryOperator, lhsBooleanExpressionNode, rhsBooleanExpressionNode));
            }
            case NumericExpressionNode _ -> throw new TinyBasicParseException("Cannot apply operator '%s' to numeric expression!".formatted(upcomingToken.getLexeme()), upcomingToken.getSourcePosition());
        };
    }

    private ExpressionNode parseBooleanExpressionTerm(TinyBasicParserContext context) throws TinyBasicParseException {
        return this.parseBooleanExpressionTermRest(context, this.parseBooleanOperand(context));
    }

    private ExpressionNode parseBooleanExpressionTermRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token upcomingToken = context.peek();
        BooleanExpressionNode.BinaryOperator booleanBinaryOperator = switch (upcomingToken) {
            case OperatorToken operatorToken when operatorToken.getOperator() == Operator.AND -> BooleanExpressionNode.BinaryOperator.AND;
            default -> null;
        };
        if (booleanBinaryOperator == null) {
            return lhs;
        }
        return switch (lhs) {
            case BooleanExpressionNode lhsBooleanExpressionNode -> {
                context.poll();
                BooleanExpressionNode rhsBooleanExpressionNode = switch (this.parseBooleanOperand(context)) {
                    case BooleanExpressionNode booleanExpressionNode -> booleanExpressionNode;
                    case NumericExpressionNode _ -> throw new TinyBasicParseException("Cannot apply operator '%s' to numeric expression!".formatted(upcomingToken.getLexeme()), upcomingToken.getSourcePosition());
                };
                yield this.parseBooleanExpressionTermRest(context, new BooleanExpressionNode.BinaryBooleanOperation(booleanBinaryOperator, lhsBooleanExpressionNode, rhsBooleanExpressionNode));
            }
            case NumericExpressionNode _ -> throw new TinyBasicParseException("Cannot apply operator '%s' to numeric expression!".formatted(upcomingToken.getLexeme()), upcomingToken.getSourcePosition());
        };
    }

    private ExpressionNode parseBooleanOperand(TinyBasicParserContext context) throws TinyBasicParseException {
        Token upcomingToken = context.peek();
        BooleanExpressionNode.UnaryOperator booleanUnaryOperator = BooleanExpressionNode.UnaryOperator.NONE;
        if (upcomingToken instanceof OperatorToken operatorToken && operatorToken.getOperator() == Operator.NOT) {
            context.poll();
            booleanUnaryOperator = BooleanExpressionNode.UnaryOperator.NOT;
        }
        ExpressionNode lhs = this.parseNumericExpression(context);
        Token nextToken = context.peek();
        BooleanExpressionNode.RelationalOperation relationalOperation = switch (nextToken) {
            case OperatorToken operatorToken -> switch (operatorToken.getOperator()) {
                case LESS_THAN -> BooleanExpressionNode.RelationalOperation.LESS_THAN;
                case LESS_THAN_OR_EQUALS -> BooleanExpressionNode.RelationalOperation.LESS_THAN_OR_EQUALS;
                case GREATER_THAN -> BooleanExpressionNode.RelationalOperation.GREATER_THAN;
                case GREATER_THAN_OR_EQUALS -> BooleanExpressionNode.RelationalOperation.GREATER_THAN_OR_EQUALS;
                case EQUALS -> BooleanExpressionNode.RelationalOperation.EQUALS;
                case NOT_EQUALS_CONVERGENT, NOT_EQUALS_DIVERGENT -> BooleanExpressionNode.RelationalOperation.NOT_EQUALS;
                default -> null;
            };
            default -> null;
        };
        if (relationalOperation == null) {
            return switch (lhs) {
                case NumericExpressionNode numericExpressionNode -> {
                    if (booleanUnaryOperator != BooleanExpressionNode.UnaryOperator.NONE) {
                        throw new TinyBasicParseException("Cannot apply unary operator 'not' to numeric expression!", upcomingToken.getSourcePosition());
                    }
                    yield numericExpressionNode;
                }
                case BooleanExpressionNode booleanExpressionNode -> new BooleanExpressionNode.UnaryBooleanExpression(booleanUnaryOperator, booleanExpressionNode);
            };
        }
        return switch (lhs) {
            case NumericExpressionNode lhsNumericExpressionNode -> {
                context.poll();
                NumericExpressionNode rhsNumericExpressionNode = this.parseAndExpectNumericExpression(context);
                yield new BooleanExpressionNode.RelationalOperationNode(booleanUnaryOperator, relationalOperation, lhsNumericExpressionNode, rhsNumericExpressionNode);
            }
            case BooleanExpressionNode _ -> throw new TinyBasicParseException("Cannot apply operator '%s' to boolean expression!".formatted(nextToken.getLexeme()), nextToken.getSourcePosition());
        };
    }

    private ExpressionNode parseNumericExpression(TinyBasicParserContext context) throws TinyBasicParseException {
        Token upcomingToken = context.peek();
        NumericExpressionNode.UnaryOperator numericUnaryOperator = NumericExpressionNode.UnaryOperator.NONE;
        if (upcomingToken instanceof OperatorToken operatorToken) {
            switch (operatorToken.getOperator()) {
                case PLUS -> {
                    context.poll();
                    numericUnaryOperator = NumericExpressionNode.UnaryOperator.PLUS;
                }
                case MINUS -> {
                    context.poll();
                    numericUnaryOperator = NumericExpressionNode .UnaryOperator.MINUS;
                }
            }
        }
        return this.parseNumericExpressionRest(context, this.parseNumericTerm(context, numericUnaryOperator));
    }

    private ExpressionNode parseNumericExpressionRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token nextToken = context.peek();
        NumericExpressionNode.BinaryOperationExpressionNode.Operator operator = switch (nextToken) {
            case OperatorToken operatorToken -> switch (operatorToken.getOperator()) {
                case PLUS -> NumericExpressionNode.BinaryOperationExpressionNode.Operator.ADD;
                case MINUS -> NumericExpressionNode.BinaryOperationExpressionNode.Operator.SUBTRACT;
                default -> null;
            };
            default -> null;
        };
        if (operator == null) {
            return lhs;
        }
        context.poll();
        return this.parseNumericExpressionRest(context, new NumericExpressionNode.BinaryOperationExpressionNode(NumericExpressionNode.UnaryOperator.NONE, operator, lhs, this.parseNumericTerm(context, NumericExpressionNode.UnaryOperator.NONE)));
    }

    private ExpressionNode parseNumericTerm(TinyBasicParserContext context, NumericExpressionNode.UnaryOperator numericUnaryOperator) throws TinyBasicParseException {
        return this.parseNumericTermRest(context, this.parseNumericFactor(context, numericUnaryOperator));
    }

    private ExpressionNode parseNumericTermRest(TinyBasicParserContext context, ExpressionNode lhs) throws TinyBasicParseException {
        Token nextToken = context.peek();
        NumericExpressionNode.BinaryOperationExpressionNode.Operator operator = switch (nextToken) {
            case OperatorToken operatorToken -> switch (operatorToken.getOperator()) {
                case TIMES -> NumericExpressionNode.BinaryOperationExpressionNode.Operator.MULTIPLY;
                case DIVIDE_FORWARD, DIVIDE_BACKWARD -> NumericExpressionNode.BinaryOperationExpressionNode.Operator.DIVIDE;
                case MOD -> NumericExpressionNode.BinaryOperationExpressionNode.Operator.MODULO;
                default -> null;
            };
            default -> null;
        };
        if (operator == null) {
            return lhs;
        }
        context.poll();
        return this.parseNumericTermRest(context, new NumericExpressionNode.BinaryOperationExpressionNode(NumericExpressionNode.UnaryOperator.NONE, operator, lhs, this.parseNumericFactor(context, NumericExpressionNode.UnaryOperator.NONE)));
    }

    private ExpressionNode parseNumericFactor(TinyBasicParserContext context, NumericExpressionNode.UnaryOperator numericUnaryOperator) throws TinyBasicParseException {
        return switch (context.poll()) {
            case VariableToken variableToken -> new NumericExpressionNode.VariableNode(numericUnaryOperator, variableToken.getVariable());
            case NumberToken numberToken -> new NumericExpressionNode.NumberNode(numericUnaryOperator, (short) numberToken.getValue());
            case ArraySigilToken _ -> new NumericExpressionNode.ArrayReferenceNode(numericUnaryOperator, this.parseParenthesizedExpression(context, "array reference expression"));
            case DelimiterToken delimiterToken -> {
                if (delimiterToken.getDelimiter() != Delimiter.LEFT_PARENTHESIS) {
                    throw new TinyBasicParseException("Expected '(' to begin expression, found '%s!".formatted(delimiterToken.getLexeme()), delimiterToken.getSourcePosition());
                }
                ExpressionNode expressionNode = switch (this.parseExpression(context)) {
                    case NumericExpressionNode numericExpressionNode -> new NumericExpressionNode.SubExpressionNode(numericUnaryOperator, numericExpressionNode);
                    case BooleanExpressionNode booleanExpressionNode -> switch (numericUnaryOperator) {
                        case PLUS -> throw new TinyBasicParseException("Cannot apply unary '+' to boolean expression!", delimiterToken.getSourcePosition());
                        case MINUS -> throw new TinyBasicParseException("Cannot apply unary '-' to boolean expression!", delimiterToken.getSourcePosition());
                        case NONE -> booleanExpressionNode;
                    };
                };
                this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end expression");
                yield expressionNode;
            }
            case KeywordToken keywordToken -> switch (keywordToken.getKeyword()) {
                case ASC -> {
                    this.expect(context, opening -> opening instanceof DelimiterToken openingDelimiter && openingDelimiter.getDelimiter() == Delimiter.LEFT_PARENTHESIS, "'(' to begin ASC() expression argument");
                    String arg = switch (context.poll()) {
                        case StringToken stringToken -> stringToken.getContents();
                        case Token token -> throw new TinyBasicParseException("Expected string literal argument for ASC() function, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
                    };
                    this.expect(context, closing -> closing instanceof DelimiterToken closingDelimiter && closingDelimiter.getDelimiter() == Delimiter.RIGHT_PARENTHESIS, "')' to end ASC() expression argument");
                    yield new NumericExpressionNode.AscFunctionNode(numericUnaryOperator, arg);
                }
                case ABS -> new NumericExpressionNode.AbsFunctionNode(numericUnaryOperator, this.parseParenthesizedExpression(context, "ABS() expression argument"));
                case RND -> new NumericExpressionNode.RndFunctionNode(numericUnaryOperator, this.parseParenthesizedExpression(context, "RND() expression argument"));
                case SGN -> new NumericExpressionNode.SgnFunctionNode(numericUnaryOperator, this.parseParenthesizedExpression(context, "SGN() expression argument"));
                default -> throw new TinyBasicParseException("Expected variable, number, array reference, function or expression enclosing in parenthesis, found '%s'!".formatted(keywordToken.getLexeme()), keywordToken.getSourcePosition());
            };
            case Token token -> throw new TinyBasicParseException("Expected variable, number, array reference, function or expression enclosing in parenthesis, found '%s'!".formatted(token.getLexeme()), token.getSourcePosition());
        };
    }

}