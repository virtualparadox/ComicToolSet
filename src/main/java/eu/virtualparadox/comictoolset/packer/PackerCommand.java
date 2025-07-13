package eu.virtualparadox.comictoolset.packer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import eu.virtualparadox.comictoolset.command.AbstractCommand;
import eu.virtualparadox.comictoolset.command.StringArgumentToPathConverter;

import java.nio.file.Files;
import java.nio.file.Path;

@Parameters(commandDescription = "To convert downloaded issues into a packed format (eg. CBZ, CBR, etc.)")
public class PackerCommand extends AbstractCommand {

    @Parameter(
            names = "--comicFolder",
            description = "Path to the folder containing downloaded comic issues (eg.: /Users/jack/Documents/comics/dylan-dog)",
            required = true,
            converter = StringArgumentToPathConverter.class
    )
    private Path comicFolder;

    @Override
    public String getCommand() {
        return "pack";
    }

    @Override
    protected void printDetailedDescription() {
        System.out.println("`pack` command is to convert downloaded issues into a packed format (eg. CBZ, CBR, etc.)");
        System.out.println("usage: java -jar ComicToolSet.jar pack --comicFolder /Users/jack/Documents/comics/dylan-dog");
    }

    @Override
    protected void internalRun() {
        final Packer packer = new Packer();
        packer.run(comicFolder);
    }

    @Override
    protected boolean validateAndPrint() {
        return Files.exists(comicFolder);
    }
}