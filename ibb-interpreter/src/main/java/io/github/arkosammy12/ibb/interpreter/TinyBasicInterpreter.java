package io.github.arkosammy12.ibb.interpreter;

import io.github.arkosammy12.ibb.core.grammar.Variable;
import io.github.arkosammy12.ibb.core.parser.*;
import io.github.arkosammy12.ibb.interpreter.exception.*;
import io.github.arkosammy12.ibb.interpreter.util.StatementResult;
import io.github.arkosammy12.ibb.interpreter.util.TinyBasicLineList;

import java.util.*;

public final class TinyBasicInterpreter {

    private static final String DEFAULT_INPUT_PROMPT_TEXT = "? ";

    private final TinyBasicInterpreterHost host;
    private final int printZoneWidth;

    private final TinyBasicLineList lines = new TinyBasicLineList();
    private final short[] variables = new short[Variable.values().length];
    private final Map<Short, Short> array = new HashMap<>();
    private final Stack<ProgramIndex> gosubStack = new Stack<>();
    private final LoopFrame[] loopFrames = new LoopFrame[Variable.values().length];
    private final Random random = new Random();

    private int lineIndex = 0;
    private int statementIndex = 0;
    private int currentTerminalColumn = 0;
    private boolean running = false;

    public TinyBasicInterpreter(TinyBasicInterpreterHost host, int printZoneWidth) {
        if (printZoneWidth < 1) {
            throw new IllegalArgumentException("The provided print zone width '%d' cannot be less than 1!".formatted(printZoneWidth));
        }
        this.host = host;
        this.printZoneWidth = printZoneWidth;
    }

    public void removeLine(int lineNumber) {
        if (lineNumber < 0) {
            throw new IllegalArgumentException("The provided line number to remove '%d' cannot be negative!".formatted(lineNumber));
        }
        this.lines.removeLine(lineNumber);
    }

    public void newProgram() {
        this.lines.clear();
        this.clear();
    }

    public void clear() {
        Arrays.fill(this.variables, (short) 0);
        this.array.clear();
        this.gosubStack.clear();
        Arrays.fill(this.loopFrames, null);
        this.currentTerminalColumn = 0;
    }

    public Collection<String> list() {
        return this.lines.getLines().stream().map(TinyBasicLineList.NumberedLineNode::rawLine).toList();
    }

    public void evaluate(String rawLine, LineNode lineNode) throws TinyBasicInterpreterException {
        try {
            this.lines.putLine(rawLine, lineNode);
            if (lineNode.getLineNumber().isEmpty()) {
                // Immediate mode statement
                this.setLineIndex(-1);
                this.execute();
            }
        } finally {
            this.lines.removeLine(-1);
        }
    }

    public void run() throws TinyBasicInterpreterException {
        try {
            this.setLineIndex(0);
            this.execute();
        } finally {
            this.lines.removeLine(-1);
        }
    }

    private void execute() throws TinyBasicInterpreterException {
        this.clear();
        this.setStatementIndex(0);
        this.running = true;
        while (this.running && !this.host.shouldExit()) {
            switch (this.lines.getStatement(this.getLineIndex(), this.getStatementIndex())) {
                case StatementResult.Ok(int lineNumber, StatementNode statementNode) -> {
                    int currentStatementIndex = this.getStatementIndex();
                    this.incrementStatementIndex();
                    this.executeStatement(lineNumber, currentStatementIndex, statementNode);
                }
                case StatementResult.Missing missing -> {
                    switch (missing) {
                        case END_OF_PROGRAM -> this.running = false;
                        case END_OF_LINE -> {
                            if (this.getLineIndex() < 0) {
                                // Break out of immediate mode line execution
                                this.running = false;
                                this.lines.removeLine(-1);
                            } else {
                                this.skipToNextLine();
                            }
                        }
                    }
                }
            }
        }
    }

    private void executeStatement(int lineNumber, int currentStatementIndex, StatementNode statementNode) throws TinyBasicInterpreterException {
        switch (statementNode) {
            case StatementNode.Assignment assignmentStatement -> {
                switch (assignmentStatement.assignmentTargetNode()) {
                    case AssignmentTargetNode.VariableTarget variableTarget -> this.setVariable(variableTarget.variable(), this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, assignmentStatement.expressionNode()));
                    case AssignmentTargetNode.ArrayElementTarget arrayElementTarget -> this.setArray(lineNumber, currentStatementIndex, this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, arrayElementTarget.arrayIndexExpression()), this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, assignmentStatement.expressionNode()));
                }
            }
            case StatementNode.Goto(ExpressionNode targetExpression) -> {
                 short jumpTargetLineNumber = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, targetExpression);
                 if (jumpTargetLineNumber < 0) {
                     throw new TinyBasicNegativeTargetException(normalizeLineNumber(lineNumber), currentStatementIndex, JumpType.GOTO, jumpTargetLineNumber);
                 }
                 int jumpTargetLineIndex = this.lines.getIndexForLineNumber(jumpTargetLineNumber);
                 if (jumpTargetLineIndex < 0) {
                     throw new TinyBasicUndefinedTargetException(normalizeLineNumber(lineNumber), currentStatementIndex, JumpType.GOTO, jumpTargetLineNumber);
                 }
                 this.setLineIndex(jumpTargetLineIndex);
                 this.setStatementIndex(0);
            }
            case StatementNode.GoSub(ExpressionNode targetExpression) -> {
                short subroutineLineNumber = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, targetExpression);
                if (subroutineLineNumber < 0) {
                    throw new TinyBasicNegativeTargetException(normalizeLineNumber(lineNumber), currentStatementIndex, JumpType.GOSUB, subroutineLineNumber);
                }
                int subroutineLineIndex = this.lines.getIndexForLineNumber(subroutineLineNumber);
                if (subroutineLineIndex < 0) {
                    throw new TinyBasicUndefinedTargetException(normalizeLineNumber(lineNumber), currentStatementIndex, JumpType.GOSUB, subroutineLineNumber);
                }
                this.gosubStack.push(new ProgramIndex(this.getLineIndex(), this.getStatementIndex()));
                this.setLineIndex(subroutineLineIndex);
                this.setStatementIndex(0);
            }
            case StatementNode.Return _ -> {
                if (this.gosubStack.isEmpty()) {
                    throw new TinyBasicReturnWithoutGoSubException(normalizeLineNumber(lineNumber), currentStatementIndex);
                } else {
                    ProgramIndex returnIndex = this.gosubStack.pop();
                    this.setLineIndex(returnIndex.lineIndex());
                    this.setStatementIndex(returnIndex.statementIndex());
                }
            }
            case StatementNode.If(ExpressionNode conditionalExpression, StatementNode statement) -> {
                if (this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, conditionalExpression)) {
                    this.executeStatement(lineNumber, currentStatementIndex, statement);
                } else {
                    this.skipToNextLine();
                }
            }
            case StatementNode.For(Variable loopVariable, ExpressionNode startValueExpression, ExpressionNode endValueExpression) -> {
                short startingValue = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, startValueExpression);
                short endValue = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, endValueExpression);
                this.setVariable(loopVariable, startingValue);
                this.loopFrames[loopVariable.getIndex()] = new LoopFrame(endValue, new ProgramIndex(this.getLineIndex(), this.getStatementIndex()));
            }
            case StatementNode.Next(Variable loopVariable) -> {
                LoopFrame loopFrame = this.loopFrames[loopVariable.getIndex()];
                if (loopFrame == null) {
                    throw new TinyBasicNextWithoutForException(normalizeLineNumber(lineNumber), currentStatementIndex, loopVariable);
                } else {
                    this.setVariable(loopVariable, (short) (this.getVariable(loopVariable) + 1));
                    if (this.getVariable(loopVariable) <= loopFrame.endValue()) {
                        ProgramIndex loopStartProgramIndex = loopFrame.loopStartProgramIndex();
                        this.setLineIndex(loopStartProgramIndex.lineIndex());
                        this.setStatementIndex(loopStartProgramIndex.statementIndex());
                    } else {
                        this.loopFrames[loopVariable.getIndex()] = null;
                    }
                }
            }
            case StatementNode.EmptyPrint _ -> {
                this.host.print("\n");
                this.currentTerminalColumn = 0;
            }
            case StatementNode.Print(StatementNode.Print.Element firstPrintElement, Collection<StatementNode.Print.Element> extraPrintElements) -> {
                StringBuilder sb = new StringBuilder();
                this.printElement(lineNumber, currentStatementIndex, firstPrintElement, sb);
                for (StatementNode.Print.Element printElement : extraPrintElements) {
                    this.printElement(lineNumber, currentStatementIndex, printElement, sb);
                }
                this.host.print(sb.toString());
            }
            case StatementNode.Input inputStatement -> {
                String prompt = inputStatement.getPrompt().orElse(DEFAULT_INPUT_PROMPT_TEXT);
                this.host.print(prompt);
                this.incrementTerminalColumn(prompt.length());
                this.inputElement(lineNumber, currentStatementIndex, inputStatement.getFirstAssignmentTargetNode());
                this.host.print("\n");
                this.currentTerminalColumn = 0;
                for (AssignmentTargetNode extraAssignmentTargetNodes : inputStatement.getExtraAssignmentTargetNodes()) {
                    this.host.print(DEFAULT_INPUT_PROMPT_TEXT);
                    this.incrementTerminalColumn(DEFAULT_INPUT_PROMPT_TEXT.length());
                    this.inputElement(lineNumber, currentStatementIndex, extraAssignmentTargetNodes);
                    this.host.print("\n");
                    this.currentTerminalColumn = 0;
                }
            }
            case StatementNode.End _ -> this.running = false;
            case StatementNode.Rem _ -> {}
        }
    }

    private void printElement(int lineNumber, int currentStatementIndex, StatementNode.Print.Element printElement, StringBuilder sb) throws TinyBasicInterpreterException {
        String printed = switch (printElement.expressionListNode()) {
            case StringNode(String contents) -> contents;
            case ExpressionNode expressionNode -> String.valueOf(this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, expressionNode));
        };
        this.incrementTerminalColumn(printed.length());
        sb.append(printed);
        switch (printElement.suffixDelimiter()) {
            case SEMICOLON -> {}
            case NONE -> {  
                sb.append('\n');
                this.currentTerminalColumn = 0;
            }
            case COMMA -> {
                int extraSpaces = this.printZoneWidth - (this.currentTerminalColumn % this.printZoneWidth);
                sb.repeat(" ", Math.max(0, extraSpaces));
                this.incrementTerminalColumn(extraSpaces);
            }
        }
    }

    private void inputElement(int lineNumber, int currentStatementIndex, AssignmentTargetNode assignmentTargetNode) throws TinyBasicInterpreterException {
        short value = this.host.input();
        switch (assignmentTargetNode) {
            case AssignmentTargetNode.VariableTarget(Variable variable) -> this.setVariable(variable, value);
            case AssignmentTargetNode.ArrayElementTarget(ExpressionNode arrayIndexExpression) -> this.setArray(lineNumber, currentStatementIndex, this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, arrayIndexExpression), value);
        }
    }

    private void incrementTerminalColumn(int incrementAmount) {
        if (incrementAmount < 0) {
            throw new IllegalArgumentException("Cannot increment terminal column index by a negative amount!");
        }
        this.currentTerminalColumn = Math.addExact(this.currentTerminalColumn, incrementAmount);
    }

    private boolean evaluateExpressionToBooleanValue(int lineNumber, int currentStatementIndex, ExpressionNode expressionNode) throws TinyBasicInterpreterException {
        return switch (expressionNode) {
            case BooleanExpressionNode booleanExpressionNode -> this.evaluateBooleanExpression(lineNumber, currentStatementIndex, booleanExpressionNode);
            case NumericExpressionNode numericExpressionNode -> this.evaluateNumericExpression(lineNumber, currentStatementIndex, numericExpressionNode) != 0;
        };
    }

    private short evaluateExpressionToNumericValue(int lineNumber, int currentStatementIndex, ExpressionNode expressionNode) throws TinyBasicInterpreterException {
        return (short) switch (expressionNode) {
            case NumericExpressionNode numericExpressionNode -> this.evaluateNumericExpression(lineNumber, currentStatementIndex, numericExpressionNode);
            case BooleanExpressionNode booleanExpressionNode -> this.evaluateBooleanExpression(lineNumber, currentStatementIndex, booleanExpressionNode) ? 1 : 0;
        };
    }

    private boolean evaluateBooleanExpression(int lineNumber, int currentStatementIndex, BooleanExpressionNode booleanExpressionNode) throws TinyBasicInterpreterException {
        return switch (booleanExpressionNode) {
            case BooleanExpressionNode.UnaryBooleanExpression(BooleanExpressionNode.UnaryOperator unaryOperator, ExpressionNode expressionNode) -> switch (unaryOperator) {
                case NONE -> this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, expressionNode);
                case NOT -> !this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, expressionNode);
            };
            case BooleanExpressionNode.BinaryBooleanOperation(BooleanExpressionNode.BinaryOperator binaryOperator, ExpressionNode lhs, ExpressionNode rhs) -> switch (binaryOperator) {
                case OR -> this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, lhs) || this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, rhs);
                case AND -> this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, lhs) && this.evaluateExpressionToBooleanValue(lineNumber, currentStatementIndex, rhs);
            };
            case BooleanExpressionNode.RelationalOperationNode(BooleanExpressionNode.RelationalOperation relationalOperation, ExpressionNode lhs, ExpressionNode rhs) -> {
                short lhsValue = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, lhs);
                short rhsValue = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, rhs);
                yield switch (relationalOperation) {
                    case LESS_THAN -> lhsValue < rhsValue;
                    case LESS_THAN_OR_EQUALS -> lhsValue <= rhsValue;
                    case GREATER_THAN -> lhsValue > rhsValue;
                    case GREATER_THAN_OR_EQUALS -> lhsValue >= rhsValue;
                    case EQUALS -> lhsValue == rhsValue;
                    case NOT_EQUALS -> lhsValue != rhsValue;
                };
            }
        };
    }

    private short evaluateNumericExpression(int lineNumber, int currentStatementIndex, NumericExpressionNode numericExpressionNode) throws TinyBasicInterpreterException {
        return (short) switch (numericExpressionNode) {
            case NumericExpressionNode.NumberNode numberNode -> numberNode.value();
            case NumericExpressionNode.VariableNode variableNode -> this.getVariable(variableNode.variable());
            case NumericExpressionNode.ArrayReferenceNode arrayReferenceNode -> this.getArray(lineNumber, currentStatementIndex, this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, arrayReferenceNode.arrayIndexExpression()));
            case NumericExpressionNode.AscFunctionNode ascFunctionNode -> !ascFunctionNode.argumentString().isEmpty() ? (short) ascFunctionNode.argumentString().charAt(0) : 0;
            case NumericExpressionNode.AbsFunctionNode absFunctionNode -> Math.abs(this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, absFunctionNode.argumentExpression()));
            case NumericExpressionNode.RndFunctionNode rndFunctionNode -> this.getRandom(lineNumber, currentStatementIndex, this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, rndFunctionNode.argumentExpression()));
            case NumericExpressionNode.UnaryNumericExpression subExpressionNode -> switch (subExpressionNode.unaryOperator()) {
                case NONE, PLUS -> this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, subExpressionNode.expressionNode());
                case MINUS -> -this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, subExpressionNode.expressionNode());
            };
            case NumericExpressionNode.SgnFunctionNode sgnFunctionNode -> {
                short val = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, sgnFunctionNode.argumentExpression());
                yield val > 0 ? 1 : (val < 0 ? -1 : 0);
            }
            case NumericExpressionNode.BinaryNumericOperation binaryOperationExpressionNode -> {
                short lhs = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, binaryOperationExpressionNode.lhs());
                short rhs = this.evaluateExpressionToNumericValue(lineNumber, currentStatementIndex, binaryOperationExpressionNode.rhs());
                yield (short) switch (binaryOperationExpressionNode.binaryOperator()) {
                    case ADD -> lhs + rhs;
                    case SUBTRACT -> lhs - rhs;
                    case MULTIPLY -> lhs * rhs;
                    case POWER -> Math.pow(lhs, rhs);
                    case DIVIDE -> {
                        if (rhs == 0) {
                            throw new TinyBasicDivisionByZeroException(normalizeLineNumber(lineNumber), currentStatementIndex, lhs);
                        } else {
                            yield (double) lhs / rhs;
                        }
                    }
                    case MODULO -> {
                        if (rhs == 0) {
                            throw new TinyBasicDivisionByZeroException(normalizeLineNumber(lineNumber), currentStatementIndex, lhs);
                        } else {
                            yield lhs % rhs;
                        }
                    }
                };
            }
        };
    }

    private void setVariable(Variable variable, short value) {
        this.variables[variable.getIndex()] = value;
    }

    private short getVariable(Variable variable) {
        return this.variables[variable.getIndex()];
    }

    private void setArray(int lineNumber, int currentStatementIndex, short index, short value) throws TinyBasicInterpreterException {
        if (index >= 0) {
            this.array.put(index, value);
        } else {
            throw new TinyBasicArrayIndexOutOfBoundsException(normalizeLineNumber(lineNumber), currentStatementIndex, index);
        }
    }

    private short getArray(int lineNumber, int currentStatementIndex, short index) throws TinyBasicInterpreterException {
        if (index >= 0) {
            return this.array.computeIfAbsent(index, _ -> (short) 0);
        } else {
            throw new TinyBasicArrayIndexOutOfBoundsException(normalizeLineNumber(lineNumber), currentStatementIndex, index);
        }
    }

    private short getRandom(int lineNumber, int currentStatementIndex, short bounds) throws TinyBasicInterpreterException {
        if (bounds > 0) {
            return (short) ((short) this.random.nextInt(bounds) + 1);
        } else {
            throw new TinyBasicRndArgumentBelowOneException(normalizeLineNumber(lineNumber), currentStatementIndex, bounds);
        }
    }

    private void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }

    private int getLineIndex() {
        return this.lineIndex;
    }

    private void setStatementIndex(int statementIndex) {
        this.statementIndex = statementIndex;
    }

    private int getStatementIndex() {
        return this.statementIndex;
    }

    private void skipToNextLine() {
        if (this.lineIndex < 0) {
            this.lineIndex = 0;
            this.statementIndex = 0;
        } else if (this.lineIndex == Integer.MAX_VALUE) {
            throw new IllegalStateException("Tried incrementing interpreter line index beyond maximum integer value");
        } else {
            this.lineIndex++;
            this.statementIndex = 0;
        }
    }

    private void incrementStatementIndex() {
        if (this.statementIndex == Integer.MAX_VALUE) {
            throw new IllegalStateException("Tried incrementing interpreter line statement index beyond maximum integer value");
        } else {
            this.statementIndex++;
        }
    }

    private record LoopFrame(short endValue, ProgramIndex loopStartProgramIndex) {}

    private record ProgramIndex(int lineIndex, int statementIndex) {

        private ProgramIndex {
            if (this.statementIndex() < 0) {
                throw new IllegalArgumentException("The provided statement index '%d' cannot be negative!".formatted(statementIndex));
            }
        }

    }

    private static Integer normalizeLineNumber(int lineNumber) {
        return lineNumber < 0 ? null : lineNumber;
    }

}
