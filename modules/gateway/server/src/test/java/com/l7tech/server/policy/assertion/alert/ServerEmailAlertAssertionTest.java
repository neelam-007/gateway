package com.l7tech.server.policy.assertion.alert;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import org.junit.Test;
import junit.framework.Assert;
import org.springframework.context.ApplicationContext;


/**
 *
 * User: grduck
 * Date: Sep 23, 2010
 * Time: 3:14:30 PM
 *
 */

public class ServerEmailAlertAssertionTest {


    @Test
    @BugNumber(8681)
    public void testWithContextVariables(){
        EmailAlertAssertion eaa = new EmailAlertAssertion();

        eaa.setSmtpHost("${host}");
        eaa.setSmtpPort("${port}");
        eaa.setAuthPassword("${pwd}");
        eaa.setAuthUsername("${user}");
        eaa.setBase64message("Message with ${var}.");
        eaa.setTargetCCEmailAddress("${ccAddress}");
        eaa.setTargetBCCEmailAddress("${bccAddress}");
        eaa.setSourceEmailAddress("${fromAddress}");
        eaa.setSubject("${subject}");
        eaa.setTargetEmailAddress("${toAddress}");
        eaa.setIsTestBean(true);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("host", "mail");
        pec.setVariable("port", "25");
        pec.setVariable("pwd", "password!");
        pec.setVariable("user", "gduck");
        pec.setVariable("var", "this is the value of var");
        pec.setVariable("ccAddress", "ccAddress@email.com");
        pec.setVariable("bccAddress", "bccAddress@email.com");
        pec.setVariable("fromAddress", "fromAddress@email.com");
        pec.setVariable("subject", "This is the subject");
        pec.setVariable("toAddress", "toAddress@email.com");

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        Assert.assertTrue(containsContextVar(eaa.getSmtpHost()));

        ServerEmailAlertAssertion serverAss = new ServerEmailAlertAssertion(eaa, appContext);
        AssertionStatus s = null;

        try{
            s = serverAss.checkRequest(pec);
        }catch(Exception e){
//            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        //Start asserting that the context variables have been replaced.
        Assert.assertFalse(containsContextVar(eaa.getSmtpHost()));
        Assert.assertFalse(containsContextVar(eaa.getSmtpPort()));
        Assert.assertFalse(containsContextVar(eaa.getAuthUsername()));
        Assert.assertFalse(containsContextVar(eaa.getAuthPassword()));
        Assert.assertFalse(containsContextVar(eaa.getSubject()));
        Assert.assertFalse(containsContextVar(eaa.getSourceEmailAddress()));
        Assert.assertFalse(containsContextVar(eaa.getTargetBCCEmailAddress()));
        Assert.assertFalse(containsContextVar(eaa.getTargetCCEmailAddress()));
        Assert.assertFalse(containsContextVar(eaa.getTargetEmailAddress()));
    }

    public boolean containsContextVar(String s){
        if(s.matches(".*\\$\\{.*")){
            return true;
        }
        return false;
    }

}
