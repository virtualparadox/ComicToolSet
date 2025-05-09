package eu.virtualparadox.comictoolset.command;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all CLI commands in the ComicToolSet application.
 * <p>
 * Each command implements its own argument handling, validation, and execution logic,
 * while inheriting shared behaviors such as help display and structured execution flow.
 * </p>
 */
public abstract class AbstractCommand {

    /** Logger bound to the concrete command class */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** If set via --help, the command prints usage information instead of executing */
    @Parameter(names = "--help", description = "To print detailed help of this command")
    private boolean printDetailedHelp = false;

    /**
     * Returns the name of the command as used in the CLI (e.g., "download", "translate").
     *
     * @return the command name
     */
    public abstract String getCommand();

    /**
     * Prints detailed usage or help information for the command.
     * Called when `--help` is passed or validation fails.
     */
    protected abstract void printDetailedDescription();

    /**
     * Executes the actual logic of the command if validation passes.
     */
    protected abstract void internalRun();

    /**
     * Validates input arguments and prints any errors if necessary.
     *
     * @return true if the input is valid and execution can proceed, false otherwise
     */
    protected abstract boolean validateAndPrint();

    /**
     * Entrypoint for executing the command after JCommander parsing.
     * <p>
     * This method handles help logic, validation, and delegates to the command’s implementation.
     * </p>
     *
     * @param parsedSuccessfully whether JCommander successfully parsed the input
     */
    public void run(final boolean parsedSuccessfully) {
        if (printDetailedHelp || !parsedSuccessfully) {
            printDetailedDescription();
        } else {
            boolean valid = validateAndPrint();
            if (valid) {
                internalRun();
            } else {
                printDetailedDescription();
            }
        }
    }
}
