package com.l7tech.server.transport.email;

import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.http.AnonymousSslClientHostnameAwareSocketFactory;
import com.l7tech.util.ConfigFactory;

import javax.net.ssl.SSLSocketFactory;

/**
 * An email utility class
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailUtils {

    private EmailUtils() {}

    // We only support SSL without client cert, but allow configuration of SSL default key just in case. Mapped to
    // it's corresponding System Property com.l7tech.server.policy.emailalert.useDefaultSsl
    private static final String USE_DEFAULT_SSL_CLUSTER_PROPERTY = "email.useDefaultSsl";

    /**
     * SMTP STARTTLS Socket Factory using existing SSL Socket Factory
     */
    public static class SmtpStartTlsSocketFactory extends StartTlsSocketFactory {

        private final SSLSocketFactory sslFactory = EmailUtils.getSslSocketFactoryInstance();
        private static final StartTlsSocketFactory singleton = new SmtpStartTlsSocketFactory();

        public static synchronized SSLSocketFactory getDefault() {
            return singleton;
        }

        @Override
        protected SSLSocketFactory getSslFactory() {
            return sslFactory;
        }
    }
  
    /**
     * Gets Socket Factory Class name for STARTTLS protocol
     * @return Socket Factory Class name
     */
    public static String getStartTlsSocketFactoryClassName() {
        return SmtpStartTlsSocketFactory.class.getName();
    }

    /**
     * Gets Socket Factory instance for STARTTLS protocol
     * @return
     */
    @SuppressWarnings("unused")
    public static SSLSocketFactory getStartTlsSocketFactoryInstance() {
        return SmtpStartTlsSocketFactory.getDefault();
    }

    /**
     * Gets Socket Factory Class name for SSL protocol
     * @return Socket Factory Class name
     */
    public static String getSslSocketFactoryClassName() {
        return getUseDefaultSslProperty() ?
                SslClientHostnameAwareSocketFactory.class.getName() :
                AnonymousSslClientHostnameAwareSocketFactory.class.getName();
    }

    /**
     * Gets Socket Factory instance for SSL protocol
     * @return
     */
    public static SSLSocketFactory getSslSocketFactoryInstance() {
        return getUseDefaultSslProperty() ?
                SslClientHostnameAwareSocketFactory.getDefault() :
                AnonymousSslClientHostnameAwareSocketFactory.getDefault();
    }

    /**
     * Sanitizes the subject text by removing all carriage return and line feed characters
     * @param subject Subject text used in Email message
     * @return Sanitized text
     */
    public static String sanitizeSubject(final String subject) {
        return subject == null ? "" : subject.replace( "\015", "" ).replace( "\012", "" );
    }

    public static boolean getUseDefaultSslProperty() {
        return ConfigFactory.getBooleanProperty(USE_DEFAULT_SSL_CLUSTER_PROPERTY, false);
    }

}
