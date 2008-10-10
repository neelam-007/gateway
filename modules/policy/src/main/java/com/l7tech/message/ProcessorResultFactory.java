package com.l7tech.message;

import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * SecurityFacet uses this interface for created ProcessorResult objects.
 */
public interface ProcessorResultFactory {
    public ProcessorResult createProcessorResult() throws ProcessorException, SAXException, IOException;
}
