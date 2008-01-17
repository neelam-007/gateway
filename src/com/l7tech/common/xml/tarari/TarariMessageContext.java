package com.l7tech.common.xml.tarari;

import com.l7tech.common.message.SoapInfo;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.SoftwareFallbackException;

/**
 * Represents resources held in kernel-memory by the Tarari driver, namely
 * a document in one buffer and a token list in the other.
 * <p>
 * This interface does not statically depend on any Tarari classes, so it is safe to refer to from classes
 * that need to load on a Windows VM without Tarari installed.
 * <p>
 * Code that needs to talk to the RAX API, and hence which already has dependencies on Tarari classes being loaded,
 * can feel free to downcast an instance of this to {@link TarariMessageContextImpl} for access to the
 * Tarari RAXContext and XMLDocument.
 */
public interface TarariMessageContext {
    /**
     * Free all resources used by this message context.  After this is called, behavior of this instance
     * is not defined. No-op when called a second time.
     */
    void close();

    /**
     * Get the shared ElementCursor for this message context.  Caller is responsible for ensuring that only
     * one scope at a time tries to make use of this cursor without duplicating it first.
     *
     * @return the ElementCursor wrapping the RaxCursor that lives in this context.  Never null.
     *         <p/>
     *         Note that it may have been
     *         left in a strange position by the last caller that used it.  Caller might want to immediately
     *         duplicate it and call moveToDocumentRoot() on the duplicate.
     */
    ElementCursor getElementCursor();

    /**
     * Get SoapInfo for this RaxDocument, creating it if necessary.
     *
     * @param soapAction
     * @return a SoapInfo instance.  Never null.
     * @throws SoftwareFallbackException if simultaneous xpath processing could not be performed.
     */
    SoapInfo getSoapInfo(String soapAction) throws SoftwareFallbackException;
}
