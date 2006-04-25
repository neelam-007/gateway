package com.l7tech.policy.assertion.xmlsec;

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

    public static WssTimestamp newInstance() {
        WssTimestamp timestamp = new WssTimestamp();
        timestamp.setRequestMaxExpiryMilliseconds(RequestWssTimestamp.DEFAULT_MAX_EXPIRY_TIME);
        timestamp.setResponseExpiryMilliseconds(ResponseWssTimestamp.DEFAULT_EXPIRY_TIME);
        return timestamp;
    }

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
        return responseExpiryMilliseconds;
    }

    public void setResponseExpiryMilliseconds(int expiryMillis) {
        this.responseExpiryMilliseconds = expiryMillis;
    }

    public TimeUnit getResponseTimeUnit() {
        return responseTimeUnit;
    }

    public void setResponseTimeUnit(TimeUnit timeUnit) {
        this.responseTimeUnit = timeUnit;
    }

    //- PRIVATE

    private TimeUnit requestTimeUnit = TimeUnit.MINUTES;
    private TimeUnit responseTimeUnit = TimeUnit.MINUTES;
    private int requestMaxExpiryMilliseconds;
    private int responseExpiryMilliseconds;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

}
