package io.github.arkosammy12.ibb.cli;

import io.github.arkosammy12.ibb.core.lexer.TinyBasicLexer;
import io.github.arkosammy12.ibb.core.parser.LineNode;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParseValue;
import io.github.arkosammy12.ibb.core.parser.TinyBasicParser;
import io.github.arkosammy12.ibb.core.result.ParseError;
import io.github.arkosammy12.ibb.core.result.ParseResult;
import io.github.arkosammy12.ibb.core.util.SourcePosition;
import io.github.arkosammy12.ibb.interpreter.TinyBasicInterpreter;
import io.github.arkosammy12.ibb.interpreter.TinyBasicInterpreterHost;
import io.github.arkosammy12.ibb.interpreter.exception.*;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import sun.misc.Signal;

import static io.github.arkosammy12.ibb.core.TinyBasicCore.parseTinyBasic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Ibb implements TinyBasicInterpreterHost {

    private static final String PROMPT = MavenProperties.ARTIFACT_ID + "> ";

    private final TinyBasicInterpreter interpreter = new TinyBasicInterpreter(this, 11);
    private final TinyBasicLexer lexer = new TinyBasicLexer();
    private final TinyBasicParser parser = new TinyBasicParser();

    private volatile boolean running = false;
    private volatile boolean interpreterRunning = false;
    private volatile boolean exitInterpreter = false;

    public Ibb() {
        Signal.handle(new Signal("INT"), _ -> {
            if (this.interpreterRunning) {
                this.exitInterpreter = true;
            } else {
                this.running = false;
                System.exit(0);
            }        
        });
    }

    public void start() {
        this.running = true;
        printHelpMessage();
        while (this.running) {
            String input = IO.readln(PROMPT);
            if (input == null) {
                IO.println();
                continue;
            }
            String strippedInput = input.strip();
            try {
                switch (strippedInput) {
                    case "new" -> this.handleNew();
                    case "clear" -> this.handleClear();
                    case "list" -> this.handleList();
                    case "run" -> this.handleRun();
                    case "help" -> this.handleHelp();
                    case "exit" -> this.handleExit();
                    default -> {
                        if (allDigits(strippedInput)) {
                            this.handleRemoveLine(Integer.parseInt(strippedInput));
                        } else if (strippedInput.startsWith("save")) {
                            this.handleSave(strippedInput);
                        } else if (strippedInput.startsWith("load")) {
                            this.handleLoad(strippedInput);
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

    private void handleRemoveLine(int lineNumber) {
        this.interpreter.removeLine(lineNumber);
    }

    private void handleHelp() {
        printHelpMessage();
    }

    private void handleExit() {
        this.running = false;
    }

    private void handleSave(String strippedInput) {
        Optional<Path> optionalPath;
        try {
            optionalPath = getPathArgument(strippedInput);
        } catch (InvalidPathException e) {
            IO.println("The provided 'save' path is invalid!");
            return;
        }
        if (optionalPath.isEmpty()) {
            IO.println("Must provide a path after the 'save' command!");
            return;
        }
        Path path = optionalPath.get();

        try {
            Files.write(path, this.interpreter.list());
        } catch (IOException e) {
            IO.println("Failed to 'save' to file '%s': %s".formatted(path.toString(), e.getCause()));
        }
    }

    private void handleLoad(String strippedInput) {
        Optional<Path> optionalPath;
        try {
            optionalPath = getPathArgument(strippedInput);
        } catch (InvalidPathException e) {
            IO.println("The provided 'load' path is invalid!");
            return;
        }
        if (optionalPath.isEmpty()) {
            IO.println("Must provide a path after the 'load' command!");
            return;
        }
        Path path = optionalPath.get();

        if (!Files.exists(path)) {
            IO.println("The provided 'load' argument path '%s' does not exist!".formatted(path.toString()));
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path);
            Collection<InsertedLine> readLines = new ArrayList<>();
            if (lines.isEmpty()) {
                IO.println("Cannot 'load' from file '%s' because the file is empty!".formatted(path.toString()));
                return;
            }
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                switch (parseTinyBasic(line, () -> this.lexer, () -> this.parser)) {
                    case ParseResult.Error<?, ?> error -> {
                        this.handleEvalParseError(error.error(), i);
                        return;
                    }
                    case ParseResult.Ok<? extends TinyBasicParseValue, ?> ok -> {
                        LineNode lineNode = ok.value().lineNode();
                        if (lineNode.getLineNumber().isEmpty()) {
                            IO.println("Cannot 'load' from file '%s' because the line in row '%s' does not have a line number!".formatted(path.toString(), i + 1));
                            return;
                        }
                        readLines.add(new InsertedLine(line, lineNode));
                    }
                }
            }
            this.interpreter.newProgram();
            for (InsertedLine insertedLine : readLines) {
                this.interpreter.evaluate(insertedLine.rawLine(), insertedLine.lineNode());
            }
        } catch (IOException e) {
            IO.println("Failed to load from file '%s': %s".formatted(path.toString(), e.getCause()));
        } catch (TinyBasicInterpreterException e) {
            // Should be unreachable since we only pass LineNodes with line numbers to the evaluate() method
            IO.println("Failed to load from file '%s'!".formatted(path.toString()));
            this.handleInterpreterException(e);
        }
    }

    private void handleRun() throws TinyBasicInterpreterException {
        this.interpreterRunning = true;
        this.interpreter.run();
        this.interpreterRunning = false;
        IO.println();
    }

    private void handleEvaluate(String input) throws TinyBasicInterpreterException {
        switch (parseTinyBasic(input, () -> this.lexer, () -> this.parser)) {
            case ParseResult.Error<?, ?> error -> this.handleEvalParseError(error.error(), -1);
            case ParseResult.Ok<? extends TinyBasicParseValue, ?> ok -> {
                LineNode lineNode = ok.value().lineNode();
                if (lineNode.getLineNumber().isPresent()) {
                    IO.println(input);
                }
                this.interpreterRunning = true;
                this.interpreter.evaluate(input, lineNode);
                this.interpreterRunning = false;
            }
        }
    }

    private void handleEvalParseError(ParseError parseError, int row) {
        Optional<SourcePosition> sourcePosition = parseError.getSourcePosition();
        String message = parseError.getMessage();
        if (sourcePosition.isEmpty()) {
            IO.println("Syntax error: %s".formatted(message));
        } else if (row >= 0) {
            IO.println("Syntax error in row '%d', column '%d': %s".formatted(row + 1, sourcePosition.get().column() + 1, message));
        } else {
            IO.println("Syntax error in column '%d': %s".formatted(sourcePosition.get().column() + 1, message));
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
            IO.println("Error on immediate mode line on statement '%d': %s".formatted(e.getStatementIndex(), message));
        } else {
            IO.println("Error on line '%d', statement '%d': %s".formatted(lineNumber.getAsInt(), e.getStatementIndex(), message));
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
            String s = IO.readln("> ");
            if (s == null) {
                return 0;
            }
            try {
                input = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                if (s.isEmpty()) {
                    IO.println("Please enter a valid number or character!");
                } else {
                    input = (int) s.charAt(0);
                }
            }
        } while (input == null);
        return (short) ((int) input);
    }

    @Override
    public boolean shouldExit() {
        if (this.exitInterpreter) {
            this.exitInterpreter = false;
            return true;
        } else {
            return false;
        }
    }

    private static boolean allDigits(String s) {
        if (s.isBlank()) {
            return false;
        }
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

    private static Optional<Path> getPathArgument(String strippedInput) {
        int firstWhitespaceIndex = strippedInput.indexOf(' ');
        if (firstWhitespaceIndex < 0 || firstWhitespaceIndex >= strippedInput.length() - 1) {
            return Optional.empty();
        }
        String arg = strippedInput.substring(firstWhitespaceIndex + 1);
        if (!arg.isEmpty() && arg.charAt(0) == '"') {
            arg = arg.substring(1);
        }
        if (!arg.isEmpty() && arg.charAt(arg.length() - 1) == '"') {
            arg = arg.substring(0, arg.length() - 1);
        }
        if (arg.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(arg));
    }

    private static void printHelpMessage() {
        IO.println("""
              %s v%s (Itsy Bitsy BASIC)
              
              Commands:
              - clear: Reset the state of the interpreter.
              - new: Reset the state of the interpreter and remove the currently stored Tiny BASIC program.
              - run: Run the currently stored Tiny BASIC program.
              - list: Print all lines of the currently stored Tiny BASIC program.
              - save <path>: Save the currently stored Tiny BASIC program to the provided file path
              - load <path>: Replace the currently stored Tiny BASIC program with the program read from the provided file path.
              - exit: Quit the Tiny BASIC interpreter and exit.
              - help: Print this help message.
              You can break out of the currently running Tiny BASIC program by pressing Ctrl+C.
              
              Statements:
              [let] <var> = <expr>
              goto <expr>
              gosub <expr>
              return
              if <expr> then <statement>
              for <var> = <expr> to <expr>
              next <var>
              print [expr|string] [(,|;) <expr|string>]*[;|,]
              input [string] <var> [, <var>]*
              rem <text> or '<text>
              end
              
              Operators:
              - Unary: +, -, not
              - Binary: +, -, *, /, \\, mod, <, <=, >, >=, =, <>, ><, and, or
              
              Variables:
              - Letters: A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z.
              - Array: @(expr)
              
              Functions:
              - SGN(expr): Returns 1 if the argument is positive, -1 if it is negative, 0 if it is 0.
              - ABS(expr): Returns the absolute value of the argument.
              - ASC(string): Returns the ASCII value of the first character of the string argument.
              - RND(expr): Returns a random value between 1 and the argument, inclusive. The argument cannot be 0 or negative.
              
              Notes:
              - Expressions may grouped with ().
              - Numeric expressions may be converted to boolean values by performing a '!= 0' comparison.
              - Boolean expressions may be converted to numeric values by evaluating to 1 if it is 'true' or 0 if it is 'false'.
              - The syntax is case insensitive.
              """.formatted(MavenProperties.ARTIFACT_ID, MavenProperties.VERSION));
    }

    private record InsertedLine(String rawLine, LineNode lineNode) {}

}
