package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.server.management.config.monitoring.EmailNotificationRule;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.processcontroller.monitoring.MonitoringKernelTest;
import com.l7tech.server.processcontroller.monitoring.InOut;
import org.junit.*;

import java.util.Arrays;
import java.io.IOException;

/**
 *
 */
public class EmailNotifierTest {
    @Ignore("Sends email to mlyons when run")
    @Test
    public void testSendEmail() throws IOException {

        EmailNotificationRule rule = new EmailNotificationRule();
        rule.setSmtpHost("mail.l7tech.com");
        rule.setPort(25);
        rule.setSubject("Hi there from EmailNotifierTest");
        rule.setFrom("mlyons@layer7tech.com");
        rule.setTo(Arrays.asList("mlyons@layer7tech.com"));
        rule.setText("Howdy doody do!\n\n" +
                     "${monitoring.context.entityType}: ${monitoring.context.entityPathName}\n" +
                     "Property: ${monitoring.context.propertyType}\n" +
                     "Property State: ${monitoring.context.propertyState}\n" +
                     "Property Value: ${monitoring.context.propertyValue} ${monitoring.context.propertyUnit}\n" +
                     "Trigger Value: ${monitoring.context.triggerValue}");
        rule.setCryptoType(EmailNotificationRule.CryptoType.PLAIN);

        EmailNotifier notifier = new EmailNotifier(rule);

        MonitoringConfiguration config = MonitoringKernelTest.makeConfig();
        config.getNotificationRules().add(rule);
        Trigger trigger = config.getTriggers().iterator().next();

        notifier.doNotification(System.currentTimeMillis(), InOut.OUT, "44", trigger);
    }
}
