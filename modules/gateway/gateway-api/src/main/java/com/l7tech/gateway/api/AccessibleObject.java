package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Abstract base class for all accessible objects.
 * 
 * @see Client#getAccessor(Class)
 */
@XmlTransient
public abstract class AccessibleObject extends ManagedObject {
}
