package eu.virtualparadox.comictoolset.command;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Parameter(names = "--help", description = "To print detailed help of this command")
    private boolean printDetailedHelp = false;

    public abstract String getCommand();

    protected abstract void printDetailedDescription();

    protected abstract void internalRun();

    protected abstract boolean validateAndPrint();

    public void run(final boolean parsedSuccessfully) {
        if (printDetailedHelp || !parsedSuccessfully) {
            printDetailedDescription();
        }
        else {
            boolean valid = validateAndPrint();
            if (valid) {
                internalRun();
            }
            else {
                printDetailedDescription();
            }
        }
    }
}
