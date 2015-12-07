package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.policy.GenericEntity;
import com.l7tech.util.XmlSafe;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 04/05/12
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class RemoteCacheEntity extends GenericEntity {

    private String name;
    private String type = RemoteCacheTypes.Memcached.getEntityType();
    private int timeout;
    private boolean enabled = true;
    private HashMap<String, String> properties = new HashMap<String, String>();

    public RemoteCacheEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
