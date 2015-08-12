package com.l7tech.gateway.common.module;

import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Utility class for calculating module digest.
 * Both {@link ServerModuleFile} and {@code ModulesScanner} (i.e. modules from the file system) use this utility for
 * calculating module digest.<br/>
 * Currently SHA256 is used, but the algorithm can be changed in the future if needed.
 */
public class ModuleDigest {

    /**
     * Calculates digest for the specified module {@code bytes}.
     *
     * @param bytes    module {@code byte array}.  Required and cannot be {@code null}.
     * @return {@code byte array} containing module {@code bytes} digest.  Never {@code null}.
     * @see #digest(java.io.InputStream)
     */
    @NotNull
    public static byte[] digest(@NotNull final byte[] bytes) {
        try {
            return digest(new ByteArrayInputStream(bytes));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates hex-encoded digest for the specified module {@code bytes}.
     *
     * @param bytes    module {@code byte array}.  Required and cannot be {@code null}.
     * @return A {@code String} containing hex-encoded module {@code bytes} digest.  Never {@code null}.
     * @see #hexEncodedDigest(java.io.InputStream)
     */
    @NotNull
    public static String hexEncodedDigest(@NotNull final byte[] bytes) {
        try {
            return hexEncodedDigest(new ByteArrayInputStream(bytes));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates digest for the specified module {@code file}.
     *
     * @param file    module {@code File}.  Required and cannot be {@code null}.
     * @return {@code byte array} containing module {@code file} digest.  Never {@code null}.
     * @throws IOException if an I/O error happens while reading file content.
     * @see #digest(java.io.InputStream)
     */
    @NotNull
    public static byte[] digest(@NotNull final File file) throws IOException {
        return digest(new FileInputStream(file));
    }

    /**
     * Calculates hex-encoded digest for the specified module {@code file}.
     *
     * @param file    module {@code File}.  Required and cannot be {@code null}.
     * @return A {@code String} containing hex-encoded module {@code file} digest.  Never {@code null}.
     * @throws IOException if an I/O error happens while reading file content.
     * @see #hexEncodedDigest(java.io.InputStream)
     */
    @NotNull
    public static String hexEncodedDigest(@NotNull final File file) throws IOException {
        return hexEncodedDigest(new FileInputStream(file));
    }

    /**
     * Calculates digest for the specified module {@code inputStream} and automatically closes the {@code inputStream} when done.
     *
     * @param inputStream    module {@code InputStream}.  Required and cannot be {@code null}.
     * @return {@code byte array} containing module {@code inputStream} digest.  Never {@code null}.
     * @throws IOException if an I/O error happens while reading file content.
     * @see #digest(java.io.InputStream, boolean)
     */
    @NotNull
    public static byte[] digest(@NotNull final InputStream inputStream) throws IOException {
        return digest(inputStream, true);
    }

    /**
     * Calculates hex-encoded digest for the specified module {@code inputStream} and automatically closes the {@code inputStream} when done.
     *
     * @param inputStream    module {@code InputStream}.  Required and cannot be {@code null}.
     * @return A {@code String} containing hex-encoded module {@code inputStream} digest.  Never {@code null}.
     * @throws IOException if an I/O error happens while reading file content.
     * @see #hexEncodedDigest(java.io.InputStream, boolean)
     */
    @NotNull
    public static String hexEncodedDigest(@NotNull final InputStream inputStream) throws IOException {
        return hexEncodedDigest(inputStream, true);
    }

    /**
     * Calculates digest for the specified module {@code inputStream} and optionally closes the {@code inputStream} when done.
     *
     * @param inputStream    module {@code InputStream}.  Required and cannot be {@code null}.
     * @param closeStream    A flag indicating whether to close the {@code stream} when done.
     * @return {@code byte array} containing module {@code inputStream} digest.  Never {@code null}.
     * @throws IOException IOException if an I/O error happens while reading file content.
     */
    @NotNull
    public static byte[] digest(@NotNull final InputStream inputStream, final boolean closeStream) throws IOException {
        return HexUtils.getSha256Digest(inputStream, closeStream);
    }

    /**
     * Calculates hex-encoded digest for the specified module {@code inputStream} and optionally closes the {@code inputStream} when done.
     *
     * @param inputStream    module {@code InputStream}.  Required and cannot be {@code null}.
     * @param closeStream    A flag indicating whether to close the {@code stream} when done.
     * @return A {@code String} containing hex-encoded module {@code inputStream} digest.  Never {@code null}.
     * @throws IOException IOException if an I/O error happens while reading file content.
     * @see #digest(java.io.InputStream, boolean)
     */
    @NotNull
    public static String hexEncodedDigest(@NotNull final InputStream inputStream, final boolean closeStream) throws IOException {
        return HexUtils.hexDump(digest(inputStream, closeStream));
    }
}
