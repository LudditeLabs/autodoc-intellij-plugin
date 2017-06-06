package com.ludditelabs.intellij.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


// Inspired by com.intellij.platform.templates.github.ZipUtil
/**
 * This class provides methods to extract ZIP archives.
 */
public class ZipUtils {
    private static final Logger logger = Logger.getInstance(ZipUtils.class);

    /**
     * Helper method to cleanup entry path.
     *
     * It removed leading and trailing slashes.
     *
     * @param zipEntry Zip entry.
     * @return Path relative to the zip archive.
     */
    @NotNull
    private static String getPath(@NotNull ZipEntry zipEntry) {
        String name = StringUtil.trimStart(zipEntry.getName(), "/");
        return StringUtil.trimEnd(name, "/");
    }

    /**
     * Helper method to extract content from the zip entry.
     *
     * If entry is dir then create same dir in the destination.
     * If it's a file then copy zip file content to destination dir.
     *
     * @param entry Zip entry.
     * @param entryStream Zip entry stream.
     * @param destDir Destination directory.
     * @param indicator IDE progress indicator.
     * @throws IOException on I/O errors.
     */
    private static void unzipEntry(@NotNull ZipEntry entry,
                                   @NotNull InputStream entryStream,
                                   @NotNull File destDir,
                                   @Nullable ProgressIndicator indicator) throws IOException {
        String entry_path = getPath(entry);
        File child = new File(destDir, entry_path);
        File dir = entry.isDirectory() ? child : child.getParentFile();

        // Make sure all parent dirs are exist.
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Unable to create directory: '" + dir + "'!");

        // Copy content to file.
        if (!entry.isDirectory()) {
            logger.debug("Extracting " + entry_path);
            if (indicator != null)
                indicator.setText("Extracting " + entry_path + "...");

            try (FileOutputStream out = new FileOutputStream(child)) {
                FileUtil.copy(entryStream, out);
            }
        }
    }

    /**
     * Helper method to unwrap single directory.
     *
     * If <em>dir</em> contains only single entry and it's a directory then
     * move its content out to the <em>dir</em>.
     *
     * The entry will be deleted.
     *
     * If <em>dir</em> has more than one entry then nothing wil happen.
     *
     * @param dir Directory nested directory.
     * @throws IOException on I/O errors.
     */
    private static void unwrap(@NotNull File dir) throws IOException {
        File[] files = dir.listFiles();
        // Act only if there is a single entry and it's a directory.
        if (files != null && files.length == 1 && files[0].isDirectory()) {
            File dir_to_unwrap = files[0];
            FileUtil.copyDirContent(dir_to_unwrap, dir);
            FileUtil.delete(dir_to_unwrap);
        }
    }

    /**
     * Unzip archive to the given directory with updating progress indicator.
     *
     * @param zipFile Archive to unzip.
     * @param destDir Destination directory.
     * @param indicator IDE progress indicator.
     * @param dropDest Delete destination before unzip if exists.
     * @param unwrapSingleDir Unwrap content of a single directory.
     * @throws IOException on I/O errors.
     *
     * @see ZipUtils#unzip(File, File, ProgressIndicator)
     */
    public static void unzip(@NotNull File zipFile,
                             @NotNull File destDir,
                             @Nullable ProgressIndicator indicator,
                             boolean dropDest,
                             boolean unwrapSingleDir) throws IOException {
        if (dropDest && destDir.exists())
            FileUtil.delete(destDir);

        try (ZipFile zip_file = new ZipFile(zipFile, ZipFile.OPEN_READ)) {
            Enumeration<? extends ZipEntry> entries = zip_file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                try (InputStream stream = zip_file.getInputStream(entry)) {
                    unzipEntry(entry, stream, destDir, indicator);
                }
            }
        }

        if (unwrapSingleDir)
            unwrap(destDir);
    }

    /**
     * Unzip archive to the given directory with updating progress indicator.
     *
     * It will delete destination directory if exists and unwrap content
     * of a single directory.
     *
     * @param zipFile Archive to unzip.
     * @param destDir Destination directory.
     * @param indicator IDE progress indicator.
     * @throws IOException on I/O errors.
     *
     * @see ZipUtils#unzip(File, File, ProgressIndicator, boolean, boolean)
     */
    public static void unzip(@NotNull File zipFile,
                             @NotNull File destDir,
                             @Nullable ProgressIndicator indicator) throws IOException {
        unzip(zipFile, destDir, indicator, true, true);
    }

    /**
     * Atomically Replace given directory with the zip archive content.
     *
     * @param zipFile Archive to unzip.
     * @param destDir Destination directory.
     * @param indicator IDE progress indicator.
     * @throws IOException on I/O errors.
     *
     * @see ZipUtils#unzip(File, File, ProgressIndicator)
     */
    public static void unzipAtomic(@NotNull File zipFile,
                                   @NotNull File destDir,
                                   @Nullable ProgressIndicator indicator) throws IOException {
        File dest_dir = destDir;
        boolean need_replace = false;

        // Logic.
        // * unzip to /path/to/dir.new
        // * move /path/to/dir -> /path/to/dir.old
        // * move /path/to/dir.new -> /path/to/dir
        // * delete /path/to/dir.old

        if (destDir.exists()) {
            need_replace = true;
            dest_dir = new File(destDir.getAbsolutePath() + ".new");
        }

        unzip(zipFile, dest_dir, indicator);

        if (need_replace) {
            Path orig_path = destDir.toPath();

            // At first move original dir to the /path/to/dir.old
            File old = new File(destDir.getAbsoluteFile() + ".old");
            if (old.exists())
                FileUtil.delete(old);

//            // Unsupported!!!
//            Files.move(destDir.toPath(), old.toPath(),
//                StandardCopyOption.ATOMIC_MOVE,
//                StandardCopyOption.COPY_ATTRIBUTES);

            FileUtil.moveDirWithContent(destDir, old);
            FileUtil.moveDirWithContent(dest_dir, destDir);

//            // Then rename /path/to/dir.new -> /path/to/dir
//            Files.move(dest_dir.toPath(), orig_path,
//                StandardCopyOption.ATOMIC_MOVE,
//                StandardCopyOption.COPY_ATTRIBUTES);

            FileUtil.delete(old);
        }
    }
}
