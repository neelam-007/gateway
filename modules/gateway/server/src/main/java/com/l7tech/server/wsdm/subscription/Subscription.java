package com.l7tech.server.wsdm.subscription;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.wsdm.faults.TopicNotSupportedFaultException;
import com.l7tech.server.wsdm.method.Subscribe;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * A subscription
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 5, 2007<br/>
 */
@Entity
@Proxy(lazy=false)
@Table(name="wsdm_subscription")
public class Subscription extends PersistentEntityImp {
    //public static final String POLICY_TAG_ESM_NOTIFICATION = "esm-notification";
    public static final int TOPIC_METRICS_CAPABILITY = 1;
    public static final int TOPIC_OPERATIONAL_STATUS = 2;
    public static final String NS = "http://www.layer7tech.com/ns/wsdm/subscription";

    private String uuid;
    private Goid esmServiceGoid;
    private Goid publishedServiceGoid;
    private String referenceCallback;
    private int topic = -1;
    private volatile long termination;
    private String notificationPolicyGuid;
    private String ownerNodeId;
    private long lastNotificationTime;
    private static final String OPERATIONAL_STATUS_CAPABILITY = "OperationalStatusCapability";
    private static final String OPERATIONAL_STATUS_CAPABILITY_PREFIX = "muws-ev";
    private static final String METRICS_CAPABILITY = "MetricsCapability";
    private static final String METRICS_CAPABILITY_PREFIX = "mowse";
    private String referenceParams;

    public Subscription(Subscribe method, String uuid, String ownerNodeId) throws TopicNotSupportedFaultException {
        this.uuid = uuid;
        this.esmServiceGoid = method.getEsmServiceGoid();
        this.publishedServiceGoid = GoidUpgradeMapper.mapId(EntityType.SERVICE, method.getServiceId());
        this.ownerNodeId = ownerNodeId;
        if (method.isTerminationParsed()) {
            termination = method.getTermination();
        }
        referenceParams = method.getReferenceParams();

        String topicValue = method.getTopicValue();
        String prefix = null;
        if (topicValue.contains(":")) {
            String[] parts = topicValue.split(":",2);
            prefix = parts[0];
            topicValue = parts[1];
        }

        if (topicValue.equals(METRICS_CAPABILITY) && prefix != null && prefix.equals(METRICS_CAPABILITY_PREFIX)) {
                topic = TOPIC_METRICS_CAPABILITY;
        } else if (topicValue.equals(OPERATIONAL_STATUS_CAPABILITY) && prefix != null && prefix.equals(OPERATIONAL_STATUS_CAPABILITY_PREFIX)) {
                topic = TOPIC_OPERATIONAL_STATUS;
        } else {
            throw new TopicNotSupportedFaultException(method.getTopicValue() + " is not a supported Topic");
        }
        referenceCallback = method.getCallBackAddress();
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @Column(name="uuid", nullable=false, unique=true, length=36)
    public String getUuid() {
        return uuid;
    }

    @Column(name="esm_service_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getEsmServiceGoid() {
        return esmServiceGoid;
    }

    @Column(name="callback_url", nullable=false, length=255)
    public String getReferenceCallback() {
        return referenceCallback;
    }

    @Column(name="termination_time", nullable=false)
    public long getTermination() {
        return termination;
    }

    @Column(name="topic", nullable=false)
    public int getTopic() {
        return topic;
    }

    @Column(name="published_service_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getPublishedServiceGoid() {
        return publishedServiceGoid;
    }

    @Column(name="notification_policy_guid", length=36)
    public String getNotificationPolicyGuid() {
        return notificationPolicyGuid;
    }

    @Column(name="reference_parameters", length=16384)
    public String getReferenceParams() {
        return referenceParams;
    }

    @Deprecated
    public void setReferenceParams(String referenceParams) {
        this.referenceParams = referenceParams;
    }

    public void setNotificationPolicyGuid(String notificationPolicyGuid) {
        this.notificationPolicyGuid = notificationPolicyGuid;
    }

    public void setTermination(long termination) {
        this.termination = termination;
    }

    @Deprecated
    protected void setPublishedServiceGoid(Goid publishedServiceGoid) {
        this.publishedServiceGoid = publishedServiceGoid;
    }

    @Deprecated
    protected Subscription() { }

    @Deprecated
    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Deprecated
    public void setEsmServiceGoid(Goid esmServiceGoid) {
        this.esmServiceGoid = esmServiceGoid;
    }

    @Deprecated
    protected void setReferenceCallback(String referenceCallback) {
        this.referenceCallback = referenceCallback;
    }

    @Deprecated
    protected void setTopic(int topic) {
        this.topic = topic;
    }

    @Column(name="owner_node_id", length=64)
    public String getOwnerNodeId() {
        return ownerNodeId;
    }

    public void setOwnerNodeId(String ownerNodeId) {
        this.ownerNodeId = ownerNodeId;
    }

    /**
     * The time that notifications were last processed for this subscription.
     *
     * <p>Note that this may not be the time that the last notification was
     * transmitted (since there may have been no data to send).</p>
     *
     * @return The time this subscription was processed.
     */
    @Column(name="last_notification")
    public long getLastNotificationTime() {
        return lastNotificationTime;
    }

    public void setLastNotificationTime(long lastNotificationTime) {
        this.lastNotificationTime = lastNotificationTime;
    }
}
