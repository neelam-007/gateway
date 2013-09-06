package com.l7tech.gateway.common.transport.email;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import org.springframework.transaction.annotation.Transactional;

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
     * Sends an email based on the provided information.
     *
     * @param toAddr    TO address(es), delimited by commas
     * @param ccAddr    CC address(es), delimited by commas
     * @param bccAddr   BCC address(es), delimited by commas
     * @param fromAddr  FROM address
     * @param subject   email subject
     * @param host      SMTP host
     * @param port      port
     * @param base64Message The email message
     * @param protocol  Protocol, refer to {@link com.l7tech.policy.assertion.alert.EmailAlertAssertion.Protocol}
     * @param authenticate  TRUE if authentication is needed
     * @param authUsername  username for authentication
     * @param authPassword  password for authentication
     * @throws EmailTestException   Fail to send the test email.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public void testSendEmail(String toAddr, String ccAddr, String bccAddr, String fromAddr, String subject, String host,
                              int port, String base64Message, EmailAlertAssertion.Protocol protocol, boolean authenticate,
                              String authUsername, String authPassword) throws EmailTestException;

    /**
     * Send an email based on the configuration of the email alert assertion.
     *
     * @param eaa    Email Alert assertion
     * @throws EmailTestException   Fail to send the test email.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException;


    /**
     * Get the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     * @return the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    long getXmlMaxBytes();

}
