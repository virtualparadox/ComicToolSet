package eu.virtualparadox.comictoolset.downloader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import eu.virtualparadox.comictoolset.command.AbstractCommand;
import eu.virtualparadox.comictoolset.command.StringArgumentToPathConverter;

import java.nio.file.Path;


@Parameters(commandDescription = "To download issues and pages from an online comic reader (eg. ReadComicOnline.li)")
public class DownloaderCommand extends AbstractCommand {

    @Parameter(names = "--comicRoot", description = "URL to the comic (eg.: https://readcomiconline.li/Comic/Dylan-Dog-1986)")
    private String comicRoot;

    @Parameter(names = "--outputFolder", description = "Folder where the issues will be stored", converter = StringArgumentToPathConverter.class)
    private Path outputFolder;

    @Override
    public String getCommand() {
        return "download";
    }

    @Override
    protected void printDetailedDescription() {
        System.out.println("`download` command is to download all accessible issue of a given comic from an online reader to a folder");
        System.out.println("Example: java -jar ComicToolSet.jar download --comicRoot https://readcomiconline.li/Comic/Dylan-Dog-1986 --outputFolder /Users/jack/Documents/comics/dylan-dog");
    }

    @Override
    protected void internalRun() {
        final Downloader downloader = new Downloader(comicRoot, outputFolder);
        downloader.download();
    }

    @Override
    protected boolean validateAndPrint() {
        return true;
    }
}
