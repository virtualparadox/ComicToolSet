package eu.virtualparadox.comictoolset;

import com.beust.jcommander.JCommander;
import eu.virtualparadox.comictoolset.command.AbstractCommand;
import eu.virtualparadox.comictoolset.downloader.DownloaderCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the ComicToolSet CLI application.
 * <p>
 * Registers all supported commands and delegates execution using JCommander.
 * </p>
 */
public class Main {

    /** Logger for application startup and command-line input tracing */
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * The list of supported commands in this CLI tool.
     * New commands should be added here.
     */
    protected static final List<AbstractCommand> COMMAND_LIST = List.of(
            new DownloaderCommand()
    );

    /**
     * Main method invoked by the JVM.
     * Parses arguments and runs the appropriate command.
     *
     * @param args the command-line arguments
     */
    public static void main(final String[] args) {
        logger.info("Arguments: {}", Arrays.asList(args));

        // Initialize the JCommander builder and register commands
        final JCommander.Builder builder = JCommander.newBuilder();
        for (final AbstractCommand command : COMMAND_LIST) {
            builder.addCommand(command.getCommand(), command);
        }

        boolean parsedSuccessfully = true;
        final JCommander commander = builder.build();

        // Attempt to parse command-line arguments
        try {
            commander.parse(args);
        } catch (final Exception e) {
            logger.error("Error while parsing command line: {}", e.getMessage());
            parsedSuccessfully = false;
        }

        // Dispatch the parsed command
        tryRunCommand(parsedSuccessfully, commander);
    }

    /**
     * Finds and runs the appropriate command after parsing.
     *
     * @param parsedSuccessfully true if argument parsing was successful
     * @param commander          the JCommander instance
     */
    private static void tryRunCommand(final boolean parsedSuccessfully, final JCommander commander) {
        final String parsedCommand = commander.getParsedCommand();
        if (StringUtils.isEmpty(parsedCommand)) {
            commander.usage();
        } else {
            final AbstractCommand command = getCommand(parsedCommand);
            command.run(parsedSuccessfully);
        }
    }

    /**
     * Retrieves the command object matching the parsed command name.
     *
     * @param parsedCommand the command name parsed by JCommander
     * @return the matching {@link AbstractCommand} instance
     * @throws IllegalArgumentException if the command name is not recognized
     */
    private static AbstractCommand getCommand(final String parsedCommand) {
        for (final AbstractCommand command : COMMAND_LIST) {
            if (command.getCommand().equals(parsedCommand)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Invalid command: " + parsedCommand + "! Something went wrong.");
    }
}
