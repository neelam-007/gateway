package com.l7tech.message;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpResponseFacet extends MessageFacet {

    private final FtpResponseKnob ftpResponseKnob;

    public FtpResponseFacet(final Message message,
                    final MessageFacet delegate,
                    final FtpResponseKnob ftpResponseKnob) {
        super(message, delegate);

        this.ftpResponseKnob = ftpResponseKnob;
    }

    public MessageKnob getKnob(Class c) {
        if (c == FtpResponseKnob.class || c == TcpKnob.class || c == UriKnob.class) {
            return ftpResponseKnob;
        }

        return super.getKnob(c);
    }
}
