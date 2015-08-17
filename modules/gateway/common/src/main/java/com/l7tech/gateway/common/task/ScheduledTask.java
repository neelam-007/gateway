package com.l7tech.gateway.common.task;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.search.Dependency;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.util.*;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
@Entity
@Proxy(lazy = false)
@Table(name = "scheduled_task")
public class ScheduledTask extends ZoneableNamedEntityImp {

    private Goid policyGoid;
    private boolean useOneNode = false;
    private JobType jobType = JobType.ONE_TIME;
    private JobStatus jobStatus;
    private long executionDate = 0;
    private long executedDate = 0;
    private String cronExpression;
    private String propertiesXml;
    private Map<String, String> properties = new TreeMap<>();
    private boolean executeOnCreate;

    @RbacAttribute
    @Size(min = 1, max = 128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }


    @NotNull
    @Column(name = "policy_goid", nullable = false)
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency(type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPolicyGoid() {
        return policyGoid;
    }

    public void setPolicyGoid(Goid policyGoid) {
        this.policyGoid = policyGoid;
    }


    @Column(name = "use_one_node")
    public boolean isUseOneNode() {
        return useOneNode;
    }

    public void setUseOneNode(boolean useOneNode) {
        this.useOneNode = useOneNode;
    }

    @RbacAttribute
    @Column(name = "job_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    @Column(name = "job_status")
    @Enumerated(EnumType.STRING)
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    @Column(name = "executed_date")
    @Min(0)
    public long getExecutedDate() {
        return executedDate;
    }

    public void setExecutedDate(long executedDate) {
        this.executedDate = executedDate;
    }

    @Column(name = "execution_date")
    @Min(0)
    public long getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(long executionDate) {
        this.executionDate = executionDate;
    }

    @Size(min = 1, max = 256)
    @Column(name = "cron_expression")
    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Transient
    public Map<String, String> getProperties() {
        return new TreeMap<>(this.properties);
    }

    public void setProperties(Map<String, String> properties) {
        this.propertiesXml = null;
        this.properties = properties != null ? properties : new TreeMap<String, String>();
    }

    @Column(name = "properties", length = Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (propertiesXml == null) {
            if (properties.size() < 1) {
                propertiesXml = "";
            } else {
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(properties);
                    encoder.close();
                    encoder = null;
                    propertiesXml = output.toString(Charsets.UTF8);
                } finally {
                    if (encoder != null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propertiesXml;
    }

    public void setSerializedProps(String serializedProps) {
        propertiesXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            properties.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in).build();
            //noinspection unchecked
            properties = (Map<String, String>) decoder.readObject();
        }
    }

    @Column(name = "execute_on_create")
    public boolean isExecuteOnCreate() {
        return executeOnCreate;
    }

    public void setExecuteOnCreate(final boolean executeOnCreate) {
        this.executeOnCreate = executeOnCreate;
    }

    public int compareTo(Object o) {
        if (o == null || !(o instanceof ScheduledTask))
            throw new IllegalArgumentException("The compared object must be a ScheduledTask.");

        String originalConnectionName = getName();
        String comparedConnectionName = ((ScheduledTask) o).getName();

        if (originalConnectionName == null || comparedConnectionName == null)
            throw new NullPointerException("JDBC connection name must not be null.");
        return originalConnectionName.compareToIgnoreCase(comparedConnectionName);
    }

    public void copyFrom(ScheduledTask other) {
        this.setGoid(other.getGoid());
        this.setName(other.getName());
        this.setVersion(other.getVersion());
        this.setPolicyGoid(other.getPolicyGoid());
        this.setUseOneNode(other.isUseOneNode());
        this.setJobType(other.getJobType());
        this.setJobStatus(other.getJobStatus());
        this.setExecutedDate(other.getExecutedDate());
        this.setExecutionDate(other.getExecutionDate());
        this.setCronExpression(other.getCronExpression());
        this.setExecuteOnCreate(other.isExecuteOnCreate());
        this.setProperties(other.getProperties());
    }

    @Transient
    public String getUserId() {
        return properties.get("userId");
    }

    public void setUserId(String userId) {
        this.propertiesXml = null;
        if (userId == null)
            properties.remove("userId");
        else
            properties.put("userId", userId);
    }

    @Transient
    public Goid getIdProviderGoid() {
        return properties.get("idProvider") == null ? null : Goid.parseGoid(properties.get("idProvider"));
    }

    public void setIdProviderGoid(Goid idProvider) {
        this.propertiesXml = null;
        if (idProvider == null)
            properties.remove("idProvider");
        else
            properties.put("idProvider", idProvider.toString());
    }
}
