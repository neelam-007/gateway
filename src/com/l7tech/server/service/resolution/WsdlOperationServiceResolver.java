/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service.resolution;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.service.PublishedService;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class WsdlOperationServiceResolver extends NameValueServiceResolver {
    protected Object[] doGetTargetValues( PublishedService service ) {
        // non soap services do not have those parameters
        if (!service.isSoap()) {
            return new Object[0];
        }
        List values = new ArrayList(2);
        int max = getMaxLength();
        try {
            Wsdl wsdl = service.parsedWsdl();
            if (wsdl == null) return new Object[0];
            Iterator operations = wsdl.getBindingOperations().iterator();
            BindingOperation operation;
            while ( operations.hasNext() ) {
                operation = (BindingOperation)operations.next();
                String value = getTargetValue( wsdl.getDefinition(), operation );
                if (value.length() > max) value = value.substring(0, max);
                if ( value != null ) values.add( value );
            }
        } catch ( WSDLException we ) {
            logger.log(Level.SEVERE, null, we);
        }

        return values.toArray();
    }

    /**
     * a set of distinct parameters for this service
     * @param candidateService object from which to extract parameters from
     * @return a Set containing distinct strings
     */
    public Set getDistinctParameters(PublishedService candidateService) {
        Set out = new HashSet();
        // non soap services do not have those parameters
        if (!candidateService.isSoap()) {
            out.add(null);
            return out;
        }
        try {
            Wsdl wsdl = candidateService.parsedWsdl();
            Iterator operations = wsdl.getBindingOperations().iterator();
            BindingOperation operation;
            while (operations.hasNext()) {
                operation = (BindingOperation)operations.next();
                String value = getTargetValue(wsdl.getDefinition(), operation);
                if (value != null) out.add(value);
            }
        } catch ( WSDLException we ) {
            logger.log(Level.SEVERE, null, we);
        }
        return out;
    }

    protected abstract String getTargetValue( Definition def, BindingOperation operation );
    private final Logger logger = Logger.getLogger(getClass().getName());
}
