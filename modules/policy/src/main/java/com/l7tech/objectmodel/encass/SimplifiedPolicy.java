package com.l7tech.objectmodel.encass;

import com.l7tech.policy.Policy;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Simplified representation of a Policy.
 */
@XmlRootElement(name = "Policy")
public class SimplifiedPolicy {
    private String guid;
    private String name;

    public SimplifiedPolicy() {
    }

    public SimplifiedPolicy(@Nullable final Policy policy) {
        if (policy != null) {
            this.guid = policy.getGuid();
            this.name = policy.getName();
        }
    }

    @XmlAttribute()
    @Nullable
    public String getGuid() {
        return guid;
    }

    public void setGuid(final String guid) {
        this.guid = guid;
    }

    @XmlAttribute()
    @Nullable
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
