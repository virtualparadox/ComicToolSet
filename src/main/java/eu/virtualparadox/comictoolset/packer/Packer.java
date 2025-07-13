package eu.virtualparadox.comictoolset.packer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packs subfolders of a comic directory into .cbz (Comic Book Zip) files.
 * Each subfolder is treated as a comic issue.
 */
public final class Packer {

    private static final Logger logger = LoggerFactory.getLogger(Packer.class);

    /**
     * Runs the packing process for all subfolders in the specified comic folder.
     *
     * @param comicFolder the root folder containing issue subfolders.
     */
    public void run(final Path comicFolder) {
        final List<Path> issues = collectSubfolders(comicFolder);

        for (final Path issueFolder : issues) {
            compressAndPackIssue(comicFolder, issueFolder);
        }
    }

    /**
     * Compresses a single issue folder into a .cbz file.
     *
     * @param comicFolder the root comic folder (used to place the .cbz file).
     * @param issueFolder the subfolder representing a comic issue.
     */
    private void compressAndPackIssue(final Path comicFolder, final Path issueFolder) {
        final File[] files = issueFolder.toFile().listFiles(File::isFile);

        if (files == null || files.length == 0) {
            logger.warn("No files found in issue folder: {}", issueFolder);
            return;
        }

        final String zipFileName = issueFolder.getFileName() + ".cbz";
        final Path zipFilePath = comicFolder.resolve(zipFileName);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            for (final File file : files) {
                final ZipEntry zipEntry = new ZipEntry(file.getName());
                zipOutputStream.putNextEntry(zipEntry);
                Files.copy(file.toPath(), zipOutputStream);
                zipOutputStream.closeEntry();
            }
            logger.info("Packed issue '{}' into '{}'", issueFolder.getFileName(), zipFilePath.getFileName());
        } catch (final IOException e) {
            logger.error("Failed to pack issue '{}': {}", issueFolder.getFileName(), e.getMessage(), e);
        }
    }

    /**
     * Collects all subdirectories of the given comic folder.
     *
     * @param comicFolder the root comic folder.
     * @return a list of paths to issue subfolders.
     * @throws IllegalArgumentException if the path is not a valid directory.
     */
    private List<Path> collectSubfolders(final Path comicFolder) {
        final File[] subdirs = comicFolder.toFile().listFiles(File::isDirectory);

        if (!comicFolder.toFile().isDirectory()) {
            throw new IllegalArgumentException("Provided path is not a directory: " + comicFolder);
        }

        if (subdirs == null) {
            throw new IllegalArgumentException("Could not list subdirectories in: " + comicFolder);
        }

        return Stream.of(subdirs)
                .map(File::toPath)
                .toList();
    }
}
