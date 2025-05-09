package eu.virtualparadox.comictoolset.command;

import com.beust.jcommander.IStringConverter;

import java.nio.file.Path;

/**
 * A JCommander converter that transforms a string argument into a {@link Path} object.
 * <p>
 * This allows command-line parameters such as {@code --outputFolder /some/path}
 * to be automatically converted to {@code java.nio.file.Path} instances.
 * </p>
 *
 * @see IStringConverter
 */
public class StringArgumentToPathConverter implements IStringConverter<Path> {

    /**
     * Converts the given string to a {@link Path}.
     *
     * @param s the input string from the command line
     * @return the corresponding {@link Path} object
     */
    @Override
    public Path convert(final String s) {
        return Path.of(s);
    }
}
