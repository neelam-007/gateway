package com.l7tech.message;

/**
 * @author Steve Jones, $Author: $
 * @version $Revision: $
 */
public class FtpFacet extends MessageFacet {

    //- PUBLIC

    /**
     *
     */
    public FtpFacet(final Message message,
                    final MessageFacet delegate,
                    final FtpRequestKnob ftpRequestKnob) {
        super(message, delegate);
        this.ftpRequestKnob = ftpRequestKnob;
    }

    /**
     *
     */
    public MessageKnob getKnob(Class c) {
        if (c == FtpRequestKnob.class || c == TcpKnob.class) {
            return ftpRequestKnob;
        }
        return super.getKnob(c);
    }

    //- PRIVATE

    /**
     *
     */
    private final FtpRequestKnob ftpRequestKnob;
}
