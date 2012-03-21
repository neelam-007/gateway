package com.l7tech.gateway.common.audit;

import com.l7tech.policy.GenericEntity;
import com.l7tech.util.*;

import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This entity stores JDBC Sink configuration
 *
 */

public class JdbcAuditSink extends GenericEntity {
    
    public static final String outputOptionAllAudits = "Audits";
    public static final String outputOptionMessageProcessingAudits = "Message Processing Audits";
    public static final String outputOptionMetrics = "Metrics";
    public static final String outputOtherAudits = "Others";
    
    private String outputs;
    private boolean fallbackToInternal;
    private String connectionName;

    public JdbcAuditSink() {
        connectionName = "";
        outputs = "";
        fallbackToInternal = true;
    }
    
    public JdbcAuditSink(String name,String connectionName, String outputs, boolean fallback){
        _name = name;
        this.connectionName = connectionName;
        this.outputs = outputs;
        this.fallbackToInternal = fallback;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public boolean isFallbackToInternal() {
        return fallbackToInternal;
    }

    public void setFallbackToInternal(boolean fallbackToInternal) {
        this.fallbackToInternal = fallbackToInternal;
    }

    public String getOutputs() {
        return outputs;
    }
    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    @Transient
    public void setOutputList(List<String> outputs) {
        StringBuffer buffer = TextUtils.join(",",outputs);
        this.outputs = buffer.toString();
    }

    @Transient
    public boolean isOutputOptionEnabled(String outputOption){
        return outputs.contains(outputOption);
    }
}
