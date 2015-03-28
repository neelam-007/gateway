package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;
import java.util.Map;

/**
 * The ScheduledTaskMO managed object represents a Scheduled Task.
 *
 * <p>The Accessor for Scheduled Tasks supports read and write. Scheduled
 * Tasks can be accessed by name or identifier.</p>
 *
 * @see com.l7tech.gateway.api.ManagedObjectFactory#createScheduledTaskMO()
 */


@XmlRootElement(name = "ScheduledTask")
@XmlType(name = "ScheduledTaskType",
        propOrder = {"name",  "policyReference", "useOneNode", "jobType", "jobStatus", "executionDate", "cronExpression", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "scheduledTasks")
public class ScheduledTaskMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the Scheduled Task (case insensitive, required)
     *
     * @return The name
     */
    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the Scheduled Task.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the BackgroundTask policy reference. (required)
     *
     * @return the BackgroundTask policy reference.
     */
    @XmlElement(name = "PolicyReference", required = true)
    public ManagedObjectReference getPolicyReference() {
        return policyReference;
    }

    /**
     * Set the policy reference. Must be a BackgroundTask policy
     * @param policyReference  the BackgroundTask policy to use
     */
    public void setPolicyReference(ManagedObjectReference policyReference) {
        this.policyReference = policyReference;
    }

    /**
     * Scheduled task is only executed on the master node. (required)
     * @return true if only executed on the master node
     */
    @XmlElement(name = "OneNode", required = true)
    public Boolean isUseOneNode() {
        return useOneNode;
    }

    /**
     * Sets if the scheduled task is only executed on the master node.
     * @param useOneNode
     */
    public void setUseOneNode(Boolean useOneNode) {
        this.useOneNode = useOneNode;
    }

    /**
     * Gets the job type. (required)
     *
     * @return the job type
     */
    @XmlElement(name = "JobType", required = true)
    public ScheduledTaskJobType getJobType() {
        return jobType;
    }

    /**
     * Set the job type.
     * @param jobType the job type
     */
    public void setJobType(ScheduledTaskJobType jobType) {
        this.jobType = jobType;
    }

    /**
     * Gets the Job status. (required)
     * @return the job status
     */
    @XmlElement(name = "JobStatus", required = true)
    public ScheduledTaskJobStatus getJobStatus() {
        return jobStatus;
    }

    /**
     * Sets the job status
     *
     * @param jobStatus the job status
     */
    public void setJobStatus(ScheduledTaskJobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    /**
     * Gets the task execution date.  Must be included for one time tasks
     *
     * @return the execution date
     */
    @XmlElement(name = "ExecutionDate")
    public Date getExecutionDate() {
        return executionDate;
    }

    /**
     * Sets the task execution date.  Must be included for one time tasks
     *
     * @param executionDate the execution date
     */
    public void setExecutionDate(Date executionDate) {
        this.executionDate = executionDate;
    }

    /**
     * Gets the Cron Expression. Must be include for recurring tasks
     *
     * @return the Cron Expression
     */
    @XmlElement(name = "CronExpression")
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the Cron Expression. Must be include for recurring tasks
     *
     * @param cronExpression the Cron Expression
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Get the properties for the Scheduled Task.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the Scheduled Task.
     *
     * @param properties The properties to use.
     */
    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Job type
     */
    @XmlEnum(String.class)
    @XmlType(name="ScheduledTaskJobTypeType")
    public enum ScheduledTaskJobType {
        /**
         * One time task
         */
        @XmlEnumValue("One time") ONE_TIME,

        /**
         * Recurring task
         */
        @XmlEnumValue("Recurring") RECURRING,

    }

    /**
     * Job Status
     */
    @XmlEnum(String.class)
    @XmlType(name="ScheduledTaskJobStatusType")
    public enum ScheduledTaskJobStatus {
        /**
         * Scheduled task
         */
        @XmlEnumValue("Scheduled") SCHEDULED,

        /**
         * Completed task
         */
        @XmlEnumValue("Completed") COMPLETED,

        /**
         * Disabled task
         */
        @XmlEnumValue("Disabled") DISABLED


    }

    //- PACKAGE

    ScheduledTaskMO() {
    }

    //- PRIVATE



    private String name;
    private ManagedObjectReference policyReference;
    private Boolean useOneNode = false;
    private ScheduledTaskJobType jobType;
    private ScheduledTaskJobStatus jobStatus;
    private Date executionDate;
    private String cronExpression;
    private Map<String, String> properties;
}
