package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.XmlSafe;

/**
 * User: njordan
 * Date: 06/03/12
 * Time: 10:48 AM
 */
@XmlSafe
public class XMPPConnectionEntity extends GenericEntity {
    private String name;
    private boolean inbound = true;
    private boolean legacySsl = false;
    private int threadpoolSize = 10;
    private String bindAddress = null;
    private boolean enabled = true;
    private String hostname;
    private int port = 5222;
    private Goid messageReceivedServiceOid;
    private Goid sessionTerminatedServiceOid;
    private String contentType = ContentTypeHeader.XML_DEFAULT.getFullValue();

    public XMPPConnectionEntity() {
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isInbound() {
        return inbound;
    }
    
    public void setInbound(boolean inbound) {
        this.inbound = inbound;
    }
    
    public int getThreadpoolSize() {
        return threadpoolSize;
    }
    
    public void setThreadpoolSize(int threadpoolSize) {
        this.threadpoolSize = threadpoolSize;
    }
    
    public String getBindAddress() {
        return bindAddress;
    }
    
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public Goid getMessageReceivedServiceOid() {
        return messageReceivedServiceOid;
    }

    // This allows migration from OID to GOID "reasonably seamlessly".
    @Deprecated
    public void setMessageReceivedServiceOid(long messageReceivedServiceOid) {
        this.messageReceivedServiceOid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, messageReceivedServiceOid);
    }

    public void setMessageReceivedServiceOid(Goid messageReceivedServiceOid) {
        this.messageReceivedServiceOid = messageReceivedServiceOid;
    }

    public Goid getSessionTerminatedServiceOid() {
        return sessionTerminatedServiceOid;
    }

    // Allows migration to GOID "reasonably seamlessly"
    @Deprecated
    public void setSessionTerminatedServiceOid(long sessionTerminatedServiceOid) {
        this.sessionTerminatedServiceOid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, sessionTerminatedServiceOid);
    }

    public void setSessionTerminatedServiceOid(Goid sessionTerminatedServiceOid) {
        this.sessionTerminatedServiceOid = sessionTerminatedServiceOid;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isLegacySsl() {
        return legacySsl;
    }

    public void setLegacySsl(boolean legacySsl) {
        this.legacySsl = legacySsl;
    }

    public static XMPPConnectionEntity newInstance() {
        return new XMPPConnectionEntity();
    }

    public static boolean restartNeeded(XMPPConnectionEntity oldEntity, XMPPConnectionEntity newEntity) {
        if(oldEntity.inbound != newEntity.inbound) {
            return true;
        }

        if(oldEntity.threadpoolSize != newEntity.threadpoolSize) {
            return true;
        }

        if(oldEntity.bindAddress == null) {
            if(newEntity.bindAddress != null) {
                return true;
            }
        } else if(!oldEntity.bindAddress.equals(newEntity.bindAddress)) {
            return true;
        }

        if(oldEntity.enabled != newEntity.enabled) {
            return true;
        }

        if(oldEntity.hostname == null) {
            if(newEntity.hostname != null) {
                return true;
            }
        } else if(!oldEntity.hostname.equals(newEntity.hostname)) {
            return true;
        }

        if(oldEntity.port != newEntity.port) {
            return true;
        }

        if(oldEntity.messageReceivedServiceOid == null && newEntity.messageReceivedServiceOid != null ||
                oldEntity.messageReceivedServiceOid != null && !oldEntity.messageReceivedServiceOid.equals(newEntity.messageReceivedServiceOid))
        {
            return true;
        }

        if(oldEntity.sessionTerminatedServiceOid == null && newEntity.sessionTerminatedServiceOid != null ||
                oldEntity.sessionTerminatedServiceOid != null && !oldEntity.sessionTerminatedServiceOid.equals(newEntity.sessionTerminatedServiceOid))
        {
            return true;
        }

        if(oldEntity.contentType == null) {
            if(newEntity.contentType != null) {
                return true;
            }
        } else if(!oldEntity.contentType.equals(newEntity.contentType)) {
            return true;
        }

        if(oldEntity.legacySsl != newEntity.legacySsl) {
            return true;
        }

        return false;
    }
}
