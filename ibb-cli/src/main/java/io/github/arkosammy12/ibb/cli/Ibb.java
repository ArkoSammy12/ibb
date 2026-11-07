package io.github.arkosammy12.ibb.cli;

import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexValue;
import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexer;
import io.github.arkosammy12.ibb.core.parser.LineNode;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParseError;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParseValue;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParser;
import io.github.arkosammy12.ibb.core.result.ParseError;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.interpreter.TinyBasicInterpreter;
import io.github.arkosammy12.ibb.interpreter.TinyBasicInterpreterHost;
import io.github.arkosammy12.ibb.interpreter.exception.*;

import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.IO.println;
import static java.lang.IO.readln;

public final class Ibb implements TinyBasicInterpreterHost {

    private static final String PROMPT = MavenProperties.ARTIFACT_ID + ">";

    private final TinyBasicInterpreter interpreter = new TinyBasicInterpreter(this, 14);
    private final TinyBasicLexer lexer = new TinyBasicLexer();
    private final TinyBasicParser parser = new TinyBasicParser();

    private boolean running;

    public void start() {

        this.running = true;

        println("""
                Welcome to %s version v%s, a Tiny BASIC interpreter!
               \s
                Below are the available interpreter commands:
                clear: Reset the state of the interpreter.
                new: Reset the state of the interpreter and remove the currently stored Tiny BASIC program.
                run: Run the currently stored.
                list: Print all lines of the currently stored Tiny BASIC program.
                exit: Quit the Tiny BASIC interpreter and exit.\s
               \s""".formatted(MavenProperties.ARTIFACT_ID, MavenProperties.VERSION));

        while (this.running) {
            String input = readln(PROMPT);
            String strippedInput = input.strip();
            try {
                switch (strippedInput) {
                    case "new" -> this.handleNew();
                    case "clear" -> this.handleClear();
                    case "list" -> this.handleList();
                    case "run" -> this.handleRun();
                    case "exit" -> this.handleExit();
                    //TODO: case "save" -> IO.println("'save' not yet implemented!");
                    //TODO: case "load" -> IO.println("'load' not yet implemented!");
                    default -> {
                        if (allDigits(strippedInput)) {
                            this.handleRemoveLine(Integer.parseInt(strippedInput));
                        } else {
                            this.handleEvaluate(input);
                        }
                    }
                }
            } catch (TinyBasicInterpreterException e) {
                IO.println();
                this.handleInterpreterException(e);
            }
        }
    }

    private void handleNew() {
        this.interpreter.newProgram();
    }

    private void handleClear() {
        this.interpreter.clear();
    }

    private void handleList() {
        this.interpreter.list().forEach(IO::println);
    }

    private void handleExit() {
        this.running = false;
    }

    private void handleRun() throws TinyBasicInterpreterException {
        // TODO: Add poll callback to let the interpreter return
        this.interpreter.run();
        IO.println();
    }

    private void handleRemoveLine(int lineNumber) {
        this.interpreter.removeLine(lineNumber);
    }

    private void handleEvaluate(String input) throws TinyBasicInterpreterException {
        switch (this.lexer.lex(input)) {
            case ParseResult.Error<?, ?> lexError -> this.handleEvalParseError(lexError.error());
            case ParseResult.Ok<TinyBasicLexValue, ?> okLex -> {
                switch (this.parser.parse(okLex.value().tokenStream())) {
                    case ParseResult.Error<?, ?> parseError -> this.handleEvalParseError(parseError.error());
                    case ParseResult.Ok<TinyBasicParseValue, TinyBasicParseError> okParse -> {
                        LineNode lineNode = okParse.value().lineNode();
                        if (lineNode.getLineNumber().isPresent()) {
                            IO.println(input);
                        }
                        this.interpreter.evaluate(input, lineNode);
                    }
                }
            }

        }
    }

    private void handleEvalParseError(ParseError parseError) {
        Optional<SourcePosition> optionalSourcePosition = parseError.getSourcePosition();
        String message = parseError.getMessage();
        if (optionalSourcePosition.isEmpty()) {
            IO.println("?Syntax error: %s".formatted(message));
        } else {
            IO.println("?Syntax error in column %d: %s".formatted(optionalSourcePosition.get().column(), message));
        }
    }

    private void handleInterpreterException(TinyBasicInterpreterException e) {
        String message = switch (e) {
            case TinyBasicArrayIndexOutOfBoundsException arrayIndexOutOfBoundsExceptions -> "The array index '%d' is out of bounds!".formatted(arrayIndexOutOfBoundsExceptions.getIndex());
            case TinyBasicDivisionByZeroException divisionByZeroException -> "Tried dividing '%d' by zero!".formatted(divisionByZeroException.getDividend());
            case TinyBasicRndArgumentBelowOneException rndArgumentBelowOneException -> "The RND() argument '%d' cannot be below one!".formatted(rndArgumentBelowOneException.getArgument());
            case TinyBasicReturnWithoutGoSubException _ -> "Encountered 'return' without 'gosub'!";
            case TinyBasicNextWithoutForException nextWithoutForException -> "Encountered a 'next' with variable '%s' without a matching 'for' statement!".formatted(nextWithoutForException.getVariable().getPattern());
            case TinyBasicNegativeTargetException negativeTargetException -> switch (negativeTargetException.getJumpType()) {
                case GOTO -> "The 'goto' line number target '%d' is negative!".formatted(negativeTargetException.getTargetLineNumber());
                case GOSUB -> "The 'gosub' line number target '%d' is negative!".formatted(negativeTargetException.getTargetLineNumber());
            };
            case TinyBasicUndefinedTargetException undefinedTargetException -> switch (undefinedTargetException.getJumpType()) {
                case GOTO -> "The 'goto' line number target '%d' is undefined!".formatted(undefinedTargetException.getTargetLineNumber());
                case GOSUB -> "The 'gosub' line number target '%d' is undefined!".formatted(undefinedTargetException.getTargetLineNumber());
            };
        };
        OptionalInt lineNumber = e.getLineNumber();
        if (lineNumber.isEmpty()) {
            IO.println("Error on immediate mode line on statement %d: %s".formatted(e.getStatementIndex(), message));
        } else {
            IO.println("Error on line %d, statement %d: %s".formatted(lineNumber.getAsInt(), e.getStatementIndex(), message));
        }
    }

    @Override
    public void print(String output) {
        IO.print(output);
    }

    @Override
    public short input() {
        Integer input = null;
        do {
            try {
                input = Integer.parseInt(IO.readln(">"));
            } catch (NumberFormatException e) {
                IO.println("Please enter a valid number!");
            }
        } while (input == null);
        return (short) ((int) input);
    }

    private static boolean allDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {}
                default ->  {
                    return false;
                }
            }
        }
        return true;
    }

}
