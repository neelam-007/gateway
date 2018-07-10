package com.l7tech.console.module;

import com.l7tech.gateway.common.module.ServerModuleFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Helper {@code ServerModuleFile} to hold signed zip bytes instead of the raw bytes and signature pair.<br/>
 * This is needed for {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin#saveServerModuleFile(byte[], String, String)}.
 */
public class ServerModuleFileWithSignedBytes extends ServerModuleFile {
    @NotNull
    private byte[] signedBytes;

    /**
     * Copy constructor.
     *
     * @param origModule    Original module.  Required and cannot be {@code null}.
     */
    public ServerModuleFileWithSignedBytes(@NotNull final ServerModuleFileWithSignedBytes origModule) {
        copyFrom(origModule, false, true, true);
        this.signedBytes = origModule.signedBytes;
    }

    /**
     * Copy from {@code origModule}, excluding data (i.e. bytes and signature), and load all {@code moduleFile} bytes.
     *
     * @param origModule    original module. Required and cannot be {@code null}.
     * @param moduleFile    module file. Required and cannot be {@code null}.
     * @throws IOException if an I/O error occurs reading from the {@code moduleFile}.
     */
    public ServerModuleFileWithSignedBytes(@NotNull final ServerModuleFile origModule, @NotNull final File moduleFile) throws IOException {
        copyFrom(origModule, false, true, true);
        this.signedBytes = Files.readAllBytes(moduleFile.toPath());

        // make sure we have the filename in the properties
        assert moduleFile.getName().equals(getProperty(PROP_FILE_NAME));
    }

    @NotNull
    public byte[] getSignedBytes() {
        return signedBytes;
    }
}
