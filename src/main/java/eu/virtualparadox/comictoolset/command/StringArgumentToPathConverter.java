package eu.virtualparadox.comictoolset.command;

import com.beust.jcommander.IStringConverter;

import java.nio.file.Path;

public class StringArgumentToPathConverter implements IStringConverter<Path> {

    @Override
    public Path convert(final String s) {
        return Path.of(s);
    }
}
