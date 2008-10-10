package com.l7tech.policy.assertion;

import com.l7tech.xml.soap.SoapVersion;

/**
 * Provides a mechanism for notifying an assertion that its parent service has changed.
 * Some assertions may need to change their default values depending on the service that they
 * are in.
 */
public interface AssertionServiceChangeListener {
    /**
     * Notifies the object that parent service has changed and that it might need to update
     * the soap version
     * @param soapVersion   The soap version enum type
     */
    public void updateSoapVersion(SoapVersion soapVersion);
}
