package eu.virtualparadox.comictoolset.downloader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import eu.virtualparadox.comictoolset.command.AbstractCommand;

import java.nio.file.Path;

/**
 * CLI command for downloading comic issues and pages from an online reader.
 * <p>
 * This command uses {@code --comicRoot} to specify the source URL of the comic,
 * and {@code --outputFolder} to determine where the downloaded issues will be stored.
 * It currently supports sites like ReadComicOnline.li.
 * </p>
 */
@Parameters(commandDescription = "To download issues and pages from an online comic reader (eg. ReadComicOnline.li)")
public class DownloaderCommand extends AbstractCommand {

    /**
     * The root URL of the comic series to be downloaded.
     * Example: https://readcomiconline.li/Comic/Dylan-Dog-1986
     */
    @Parameter(
            names = "--comicRoot",
            description = "URL to the comic (eg.: https://readcomiconline.li/Comic/Dylan-Dog-1986)",
            required = true
    )
    private String comicRoot;

    /**
     * The local folder where downloaded comic issues will be saved.
     */
    @Parameter(
            names = "--outputFolder",
            description = "Folder where the issues will be stored",
            required = true,
            converter = PathConverter.class
    )
    private Path outputFolder;

    /**
     * Returns the CLI command keyword ("download").
     *
     * @return the command name
     */
    @Override
    public String getCommand() {
        return "download";
    }

    /**
     * Prints a detailed description and usage example for this command.
     */
    @Override
    protected void printDetailedDescription() {
        System.out.println("`download` command is to download all accessible issue of a given comic from an online reader to a folder");
        System.out.println("Example: java -jar ComicToolSet.jar download --comicRoot https://readcomiconline.li/Comic/Dylan-Dog-1986 --outputFolder /Users/jack/Documents/comics/dylan-dog");
    }

    /**
     * Executes the download operation using the provided arguments.
     */
    @Override
    protected void internalRun() {
        final Downloader downloader = new Downloader(comicRoot, outputFolder);
        downloader.download();
    }

    /**
     * Validates the command-line parameters. This version always returns true
     * because required parameters are already enforced by JCommander.
     *
     * @return true if validation passes
     */
    @Override
    protected boolean validateAndPrint() {
        return true;
    }
}
