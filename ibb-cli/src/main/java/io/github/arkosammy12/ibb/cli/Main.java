package io.github.arkosammy12.ibb.cli;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "ibb",
        mixinStandardHelpOptions = true,
        versionProvider = MavenProperties.Provider.class,
        description = "Enter the interactive REPL or execute the given Tiny BASIC script"
)
public final class Main implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(
            names = {"-p", "--print-zone-width"},
            description = "Specify the default print zone width. Default is 14",
            defaultValue = "14"
    )
    private int printZoneWidth;

    @CommandLine.Parameters(
            index = "0",
            arity = "0..1",
            description = "The path of the Tiny BASIC script to execute, if provided"
    )
    private Path scriptPath;

    @Override
    public Integer call() {
        if (this.printZoneWidth < 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "The provided print zone width '%d' cannot be less than 1!".formatted(this.printZoneWidth));
        }
        Ibb ibb = new Ibb(this.printZoneWidth);
        if (this.scriptPath == null) {
            ibb.startRepl();
        } else {
            if (!Files.exists(this.scriptPath)) {
                throw new CommandLine.ParameterException(spec.commandLine(), "The provided script path '%s' does not exist!".formatted(this.scriptPath.toString()));
            }
            ibb.runScript(this.scriptPath);
        }
        return 0;
    }

    static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

}