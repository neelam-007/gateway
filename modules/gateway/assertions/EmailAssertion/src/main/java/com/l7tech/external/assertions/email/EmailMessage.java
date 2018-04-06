package com.l7tech.external.assertions.email;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.Serializable;
import java.util.*;

/**
 * Bean which holds all information about an email message.
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailMessage implements Serializable {

    // Required parameters
    private InternetAddress fromAddress;
    private InternetAddress[] toAddresses;
    private InternetAddress[] ccAddresses;
    private InternetAddress[] bccAddresses;

    // Optional parameters
    private String message;
    private String subject;
    private EmailFormat format = EmailFormat.PLAIN_TEXT;
    private transient List<EmailAttachmentDataSource> attachmentDataSources;

    private EmailMessage(final InternetAddress fromAddress, final InternetAddress[] toAddresses, final String message,
                        final String subject) {
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses;
        this.message = message;
        this.subject = subject;
    }

    public InternetAddress getFromAddress() {
        return fromAddress;
    }

    public InternetAddress[] getToAddresses() {
        return toAddresses;
    }

    public InternetAddress[] getCcAddresses() {
        return ccAddresses;
    }

    public InternetAddress[] getBccAddresses() {
        return bccAddresses;
    }

    public String getMessage() {
        return message;
    }

    public String getSubject() {
        return subject;
    }

    public EmailFormat getFormat() { return format; }

    public List<EmailAttachmentDataSource> getAttachmentDataSources() {
        return attachmentDataSources != null ? attachmentDataSources : Collections.emptyList();
    }

    /**
     * Email Message builder class for constructing Email Message object using Builder pattern.
     */
    public static class EmailMessageBuilder {
        // Required parameters
        private InternetAddress fromAddress;
        private InternetAddress[] toAddresses;
        private String message;
        private String subject;

        // Optional parameter
        private EmailFormat format = EmailFormat.PLAIN_TEXT;
        private InternetAddress[] ccAddresses;
        private InternetAddress[] bccAddresses;
        private List<EmailAttachmentDataSource> attachmentDataSources;

        public EmailMessageBuilder(final InternetAddress fromAddress, final InternetAddress[] toAddresses, final
                String message, final String subject) {
            this.fromAddress = fromAddress;
            this.toAddresses = toAddresses;
            this.message = message;
            this.subject = subject;
        }

        public EmailMessageBuilder(final String fromAddress, final String toAddresses, final String message, final
                String subject) {
            this.fromAddress = toInternetAddress(fromAddress);
            this.toAddresses = toInternetAddresses(toAddresses);
            this.message = message;
            this.subject = subject;
        }

        public EmailMessageBuilder(final EmailAssertion assertion) {
            this(assertion.getSourceEmailAddress(), assertion.getTargetEmailAddress(), assertion.messageString(),
                    assertion.getSubject());
            setFormat(assertion.getFormat());
            setCcAddresses(assertion.getTargetCCEmailAddress());
            setBccAddresses(assertion.getTargetBCCEmailAddress());
        }

        public EmailMessageBuilder setCcAddresses(final String ccAddresses) {
            this.ccAddresses = toInternetAddresses(ccAddresses);
            return this;
        }

        public EmailMessageBuilder setCcAddresses(final InternetAddress[] ccAddresses) {
            this.ccAddresses = ccAddresses;
            return this;
        }

        public EmailMessageBuilder setBccAddresses(final String bccAddresses) {
            this.bccAddresses = toInternetAddresses(bccAddresses);
            return this;
        }

        public EmailMessageBuilder setBccAddresses(final InternetAddress[] bccAddresses) {
            this.bccAddresses = bccAddresses;
            return this;
        }

        public EmailMessageBuilder setAttachmentDataSources(final List<EmailAttachmentDataSource> attachmentDataSources) {
            this.attachmentDataSources = attachmentDataSources;
            return this;
        }

        public EmailMessageBuilder setFormat(final EmailFormat format) {
            this.format = format;
            return this;
        }

        public EmailMessage build() {
            final EmailMessage emailMessage = new EmailMessage(fromAddress, toAddresses, message, subject);

            emailMessage.format = this.format;
            emailMessage.ccAddresses = this.ccAddresses;
            emailMessage.bccAddresses = this.bccAddresses;
            emailMessage.attachmentDataSources = this.attachmentDataSources;

            return emailMessage;
        }

        private InternetAddress toInternetAddress(final String address) {
            try {
                return new InternetAddress(address);
            } catch (AddressException e) {
                return null;
            }
        }

        private InternetAddress[] toInternetAddresses(final String addresses) {
            try {
                return InternetAddress.parse(addresses);
            } catch (AddressException e) {
                return new InternetAddress[0];
            }
        }
    }
}
