package com.l7tech.common.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes a {@link TarariMessageContext} available to assertions.  (The TarariMessageContext will need to be downcasted to
 * {@link com.l7tech.common.xml.tarari.TarariMessageContextImpl} by the caller, since we can't statically link any
 * Tarari classes from any class that needs to run without a Tarari card).
 * <p>
 * Note that this knob will not be present in any message before {@link Message#isSoap} has been called on it, and even
 * then only on systems with Tarari hardware installed.
 */
public class TarariKnob implements CloseableMessageKnob {
    private static final Logger logger = Logger.getLogger(TarariKnob.class.getName());

    private final Message message;
    private TarariMessageContext context;
    private SoapInfo soapInfo;
    private boolean giveup = false; // if true, stop attempting to run this message through the hardware

    public TarariKnob(Message message, TarariMessageContext context, SoapInfo soapInfo) {
        if (message == null) throw new NullPointerException();
        this.message = message;
        this.context = context;
        this.soapInfo = soapInfo;
    }

    /**
     * Get the TarariMessageContext for this message.  If the original TMC that located the SoapInfo has since
     * been invalidated, this method will try to create a new one by running the message through the hardware
     * again.  If a TarariMessageContext cannot be created, returns null.
     *
     * @return the TarariMessageContext, or null if one is not available and cannot be created.
     */
    public TarariMessageContext getContext() throws IOException, NoSuchPartException, SAXException {
        if (context == null && !giveup) {
            // Try to create one
            TarariMessageContextFactory mcfac = TarariLoader.getMessageContextFactory();
            if (mcfac == null) {
                // How did we get here?
                logger.log(Level.WARNING, "TarariKnob is present, but no TarariMessageContextFactory"); // can't happen
                giveup = true;
                return null;
            }

            SoapInfoFactory siFac = TarariLoader.getSoapInfoFactory();
            if (siFac == null) {
                logger.log(Level.WARNING, "TarariKnob is present, but no Tarari SoapInfoFactory"); // can't happen
                giveup = true;
                return null;
            }

            try {
                logger.log(Level.FINE, "Passing message into Tarari hardware again");
                context = mcfac.makeMessageContext(message.getMimeKnob().getFirstPart().getInputStream(false));
                soapInfo = siFac.getSoapInfo(context);
            } catch (SoftwareFallbackException e) {
                // TODO if this happens a lot for perfectly reasonable reasons, downgrade to something below INFO
                logger.log(Level.INFO, "Falling back from hardware to software processing", e);
                giveup = true;
                return null;
            }
        }
        return context;
    }

    /**
     * Manually attach a TarariMessageContext to this TarariKnob.  If a context already exists, it'll be replaced.
     *
     * @param tmc the context to attach.  Must not be null.
     */
    public void setContext(TarariMessageContext tmc, SoapInfo soapInfo) {
        close();
        this.context = tmc;
        this.soapInfo = soapInfo;
    }

    /** @return SoapInfo, or null if there isn't any known. */
    public SoapInfo getSoapInfo() {
        return soapInfo;
    }

    public void close() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    /**
     * Invalidate any TarariKnob attached to the specified Message.
     *
     * @param m the Message whose TarariKnob, if any, should be closed.
     */
    static void invalidate(Message m) {
        TarariKnob knob = (TarariKnob)m.getKnob(TarariKnob.class);
        if (knob != null)
            knob.close();
    }
}
