package com.l7tech.gateway.common.module;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Holds data bytes for a {@link ServerModuleFile Server Module File} that an SSM
 * administrator adds to the Gateway database in the hope that every cluster node will
 * install or upgrade it.
 * <p/>
 * This entity have to be fetched lazy, therefore annotation {@code @Proxy(lazy=true)} is set.
 */
@Entity
@Proxy(lazy = true)
@Table(name = "server_module_file_data")
public class ServerModuleFileData extends PersistentEntityImp implements Serializable {
    private static final long serialVersionUID = -7525412738669913604L;

    /**
     * Module row bytes.
     */
    private byte[] dataBytes;
    // When adding fields, update copyFrom() method

    private String signatureProperties;

    /**
     * The owning {@link ServerModuleFile Server Module File}.
     */
    private ServerModuleFile serverModuleFile;

    /**
     * Required by Hibernate.
     */
    @Deprecated
    protected ServerModuleFileData() {
    }


    /**
     * Default constructor.
     *
     * @param serverModuleFile    Owning {@link ServerModuleFile module file}.  Required.
     */
    public ServerModuleFileData(@NotNull final ServerModuleFile serverModuleFile) {
        this.serverModuleFile = serverModuleFile;
    }


    /**
     * Represents the owning {@link ServerModuleFile Server Module File}.
     */
    @NotNull
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true, mappedBy = "data")
    @Dependency(isDependency = false)
    public ServerModuleFile getServerModuleFile() {
        return serverModuleFile;
    }
    public void setServerModuleFile(final ServerModuleFile serverModuleFile) {
        this.serverModuleFile = serverModuleFile;
    }


    /**
     * Represents entity version field or property.
     */
    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }


    /**
     * Get the module row bytes.<br/>
     * Note that executing this property getter method might cause the object to be fetched from the Database.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data_bytes")
    public byte[] getDataBytes() {
        return dataBytes;
    }
    public void setDataBytes(final byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }


    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "signature_properties", length = Integer.MAX_VALUE)
    public String getSignatureProperties() {
        return signatureProperties;
    }

    public void setSignatureProperties(String signatureProperties) {
        this.signatureProperties = signatureProperties;
    }

    /**
     * Copy data from the specified {@code otherData}
     *
     * @param otherData    the {@link ServerModuleFileData module data} from which to copy properties.  Required.
     */
    public void copyFrom(@NotNull final ServerModuleFileData otherData) {
        setDataBytes(otherData.getDataBytes());
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ServerModuleFileData that = (ServerModuleFileData) o;

        // extract the module sha256
        //
        // todo (tveninov): Perhaps remove the check (serverModuleFile != null), serverModuleFile shouldn't be null
        // unless hibernate somehow mess-up the entity, in which NullPointerException exception might be a better option.
        final String moduleSha256 = serverModuleFile != null ? serverModuleFile.getModuleSha256() : null;
        final String thatModuleSha256 = that.getServerModuleFile() != null ? that.getServerModuleFile().getModuleSha256() : null;

        // no need to compare the row bytes, sha256 is enough
        return (moduleSha256 != null ? moduleSha256.equals(thatModuleSha256) : thatModuleSha256 == null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // extract the module sha256
        // no need to generate row bytes hash-code, having sha256 hash-code is enough
        //
        // todo (tveninov): Perhaps remove the check (serverModuleFile != null), serverModuleFile shouldn't be null
        // unless hibernate somehow mess-up the entity, in which NullPointerException exception might be a better option.
        final String moduleSha256 = serverModuleFile != null ? serverModuleFile.getModuleSha256() : null;
        result = 31 * result + (moduleSha256 != null ? moduleSha256.hashCode() : 0);
        return result;
    }
}