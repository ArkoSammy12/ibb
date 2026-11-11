# ibb

Itsy Bitsy BASIC

A Tiny BASIC interpreter written in Java.

## Usage

A Java Runtime Environment (JRE) or Development Kit (JDK) is required to run *ibb*. 

You can run *ibb* with `java -jar ibb-x.y.z.jar <args>`.

### CLI

You can pass optional arguments to the interpreter instructing it to run a Tiny BASIC file directly, or to just change some settings:

```
Usage: ibb [-hV] [-p=<printZoneWidth>] [<scriptPath>]
Enter the interactive REPL or execute the given Tiny BASIC script
      [<scriptPath>]   The path of the Tiny BASIC script to execute, if provided
  -h, --help           Show this help message and exit.
  -p, --print-zone-width=<printZoneWidth>
                       Specify the default print zone width. Default is 14
  -V, --version        Print version information and exit.
```

### REPL

If no script path is passed to the interpreter, it will enter into REPL (read-eval print loop) mode, where you can interactively
enter commands to interact with the interpreter, run lines of code directly, or build the script from the terminal line by line.

```
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
if <expr> [then] <statement>
for <var> = <expr> to <expr>
next <var>
print [expr|string|#expr] [(,|;) <expr|string|#expr>]*[;|,]
input [string] <var> [, <var>]*
rem <text> or '<text>
end

Operators:
- Unary: +, -, not
- Binary: +, -, *, /, \, mod, ^, <, <=, >, >=, =, <>, ><, and, or

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
```

## Building

*ibb* is written in Java 25, so a Java Development Kit (JDK) targeting Java 25 or later to build this project.

Maven is used as the build and dependency management solution, though a wrapper is provided to avoid a local installation.
To build the project, clone or download the repository and run the following from the top-level directory:

### Windows

```
.\mvnw clean package
```

### macOS/Linux

```
./mvnw clean package
```

An output `.jar` should have then been generated in `/target/ibb-x.y.z.jar`.
To skip the automated tests, append the `-DskipTests` to the build command.


## License
This project is licensed under the [MIT License](LICENSE).