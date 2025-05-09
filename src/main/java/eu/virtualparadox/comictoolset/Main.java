package eu.virtualparadox.comictoolset;

import com.beust.jcommander.JCommander;
import eu.virtualparadox.comictoolset.command.AbstractCommand;
import eu.virtualparadox.comictoolset.downloader.DownloaderCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    protected static final List<AbstractCommand> COMMAND_LIST = List.of(
            new DownloaderCommand()
    );

    public static void main(final String[] args) {
        logger.info("Arguments: {}", Arrays.asList(args));

        // Initialize the builder and add commands.
        final JCommander.Builder builder = JCommander.newBuilder();
        for (final AbstractCommand command : COMMAND_LIST) {
            builder.addCommand(command.getCommand(), command);
        }

        // Parse command line arguments.
        boolean parsedSuccessfully = true;
        final JCommander commander = builder.build();
        try {
            commander.parse(args);
        } catch (final Exception e) {
            logger.error("Error while parsing command line: {}", e.getMessage());
            parsedSuccessfully = false;
        }

        // Try to run command.
        tryRunCommand(parsedSuccessfully, commander);
    }

    private static void tryRunCommand(final boolean parsedSuccessfully, final JCommander commander) {
        final String parsedCommand = commander.getParsedCommand();
        if (StringUtils.isEmpty(parsedCommand)) {
            commander.usage();
        } else {
            final AbstractCommand command = getCommand(parsedCommand);
            command.run(parsedSuccessfully);
        }
    }

    private static AbstractCommand getCommand(final String parsedCommand) {
        for (final AbstractCommand command : COMMAND_LIST) {
            if (command.getCommand().equals(parsedCommand)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Invalid command: " + parsedCommand + "! Something went wrong.");
    }

}
