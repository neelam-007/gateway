package com.l7tech.message;

/**
 * Information about a response that arrived over FTP.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface FtpResponseKnob extends MessageKnob {

    /**
     * The FTP reply code received over the control connection. A value of 0 indicates there was no response or the
     * code has not been set yet.
     *
     * @return the FTP reply code, or 0 if there was no response or the code has not been set
     */
    int getReplyCode();

    /**
     * The data received over the control connection. Null if there was no data returned, if there was no response,
     * or the value has not been set yet.
     *
     * @return the reply data, or null if there was no data or no response or the value has not been set
     */
    String getReplyData(); // TODO jwilliams: make available from context variable messages too
}
