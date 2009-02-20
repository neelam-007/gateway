package com.l7tech.message;

import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * SecurityFacet uses this interface for created ProcessorResult objects.
 */
public interface ProcessorResultFactory {
    /**
     * Perform a deferred invocation of the WSS Processor on this factory's associated Message and return the processor result.
     *
     * @return the processor result.  Never null.
     * @throws ProcessorException if processing fails
     * @throws java.io.IOException if there is a problem reading a message
     * @throws SAXException if there is a problem parsing the XML
     */
    public ProcessorResult createProcessorResult() throws ProcessorException, SAXException, IOException;
}
