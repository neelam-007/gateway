package com.l7tech.external.assertions.email;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.UNCHECKED_WIDE_OPEN;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote interface for supporting email administrative operations.
 * 
 * User: dlee
 * Date: Nov 13, 2008
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured
@Administrative
public interface EmailAdmin {

    /**
     * Send test email.
     *
     * @param emailMessage Email message details
     * @param emailConfig Email configuration
     * @throws MessagingException Fail to send the test email.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public void sendTestEmail(EmailMessage emailMessage, EmailConfig emailConfig) throws MessagingException;

}
