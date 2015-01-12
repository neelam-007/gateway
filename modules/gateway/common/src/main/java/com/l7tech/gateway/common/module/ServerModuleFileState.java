package com.l7tech.gateway.common.module;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Represents a {@link ServerModuleFile Server Module File} state for particular cluster node.
 */
@Entity
@Proxy(lazy = false)
@Table(name = "server_module_file_state")
public class ServerModuleFileState extends PersistentEntityImp implements Serializable {
    private static final long serialVersionUID = -6260051999490987295L;

    private String nodeId;
    private ModuleState state;
    private String errorMessage;
    // When adding fields, update copyFrom() method

    /**
     * The owning {@link ServerModuleFile Server Module File}.
     */
    private ServerModuleFile serverModuleFile;

    /**
     * Required by Hibernate.
     */
    @Deprecated
    protected ServerModuleFileState() {
    }

    /**
     * Default constructor.
     *
     * @param serverModuleFile    Owning {@link ServerModuleFile module file}.  Required.
     */
    public ServerModuleFileState(@NotNull final ServerModuleFile serverModuleFile) {
        this.serverModuleFile = serverModuleFile;
    }


    /**
     * Represents the owning {@link ServerModuleFile Server Module File}.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "module_goid", nullable = false)
    @Fetch(FetchMode.SELECT)
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
     * Represents the OID of the cluster node this state belongs to.
     */
    @Column(name = "node_id", length = 36, nullable = false)
    public String getNodeId() {
        return nodeId;
    }
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Represents the module current {@link ModuleState state}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 64, nullable = false)
    public ModuleState getState() {
        return state;
    }
    public void setState(final ModuleState state) {
        this.state = state;
    }


    /**
     * Represents module state error message. <br/>
     * When this field is set it indicates that the module failed to transit into the next state.
     */
    @Lob
    @Basic(fetch=FetchType.LAZY)
    @Column(name = "error_message", nullable = true)
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }


    /**
     * Copy data from the specified {@code otherState}
     *
     * @param otherState    the {@link ServerModuleFileState module state} from which to copy properties.  Required.
     */
    public void copyFrom(@NotNull final ServerModuleFileState otherState) {
        setNodeId(otherState.getNodeId());
        setState(otherState.getState());
        setErrorMessage(otherState.getErrorMessage());
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ServerModuleFileState that = (ServerModuleFileState) o;

        if (nodeId != null ? !nodeId.equals(that.nodeId) : that.nodeId != null) return false;
        if (state != null ? !state.equals(that.state) : that.state != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        return result;
    }
}
