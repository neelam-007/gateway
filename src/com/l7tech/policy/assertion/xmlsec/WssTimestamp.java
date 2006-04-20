package com.l7tech.policy.assertion.xmlsec;

import javax.swing.*;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.util.TimeUnit;

/**
 * Request and response WSS Timestamp.
 *
 * <p>Useful if you want request and response unsigned timestamps (as in WS-SecurityPolicy)</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WssTimestamp extends Assertion implements SecurityHeaderAddressable  {

    //- PUBLIC

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public int getRequestMaxExpiryMilliseconds() {
        return requestMaxExpiryMilliseconds;
    }

    public void setRequestMaxExpiryMilliseconds(int maxExpiryMilliseconds) {
        this.requestMaxExpiryMilliseconds = maxExpiryMilliseconds;
    }

    public TimeUnit getRequestTimeUnit() {
        return requestTimeUnit;
    }

    public void setRequestTimeUnit(TimeUnit timeUnit) {
        this.requestTimeUnit = timeUnit;
    }

    public int getResponseExpiryMilliseconds() {
        return responseMaxExpiryMilliseconds;
    }

    public void setResponseExpiryMilliseconds(int expiryMillis) {
        this.responseMaxExpiryMilliseconds = expiryMillis;
    }

    public TimeUnit getResponseTimeUnit() {
        return responseTimeUnit;
    }

    public void setResponseTimeUnit(TimeUnit timeUnit) {
        this.responseTimeUnit = timeUnit;
    }

    //- PRIVATE

    private TimeUnit requestTimeUnit = TimeUnit.SECONDS;
    private TimeUnit responseTimeUnit = TimeUnit.SECONDS;
    private int requestMaxExpiryMilliseconds = 60 * 60 * 1000; // One hour
    private int responseMaxExpiryMilliseconds = 60 * 60 * 1000; // One hour
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

}
