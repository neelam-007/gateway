package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;

import com.l7tech.common.util.MultipartUtil;
import com.l7tech.common.util.MultipartMessageReader;

/**
 * Encapsulates a Message (i.e. a Request or Response) that contains XML.
 *
 * @author alex
 * @version $Revision$
 */
public interface XmlMessage extends Message {

    /**
     * Returns the DOM <code>Document</code> attached to this message.
     *
     * <b>Note</b> that this calling method may result in any underlying InputStream
     * being consumed.
     *
     * @return
     * @throws SAXException
     * @throws IOException
     */
    Document getDocument() throws SAXException, IOException;
    void setDocument(Document doc);
    
    public Map getAttachments() throws IOException;

    public MultipartUtil.Part getAttachment(int position) throws IOException;

    public String getMultipartBoundary();

    public MultipartMessageReader getMultipartReader();

    public MultipartUtil.Part getSoapPart() throws IOException;

    public boolean isMultipart();
}
