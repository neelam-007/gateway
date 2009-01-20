/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service.resolution;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import java.util.*;

/**
 * @author alex
 */
public abstract class WsdlOperationServiceResolver<T> extends NameValueServiceResolver<T> {
    protected WsdlOperationServiceResolver(ApplicationContext spring) {
        super(spring);
    }

    protected List<T> doGetTargetValues(PublishedService service) throws ServiceResolutionException {
        // non soap services do not have those parameters
        if (!service.isSoap()) {
            return Collections.emptyList();
        }
        List<T> values = new ArrayList<T>(2);
        //int max = getMaxLength();
        try {
            Wsdl wsdl = service.parsedWsdl();
            // fla bugfix for 1827 soap bindings only should be considered for soap web services
            if (wsdl == null) return Collections.emptyList();
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            Iterator operations = wsdl.getBindingOperations().iterator();
            BindingOperation operation;
            while ( operations.hasNext() ) {
                operation = (BindingOperation)operations.next();
                T value = getTargetValue( wsdl.getDefinition(), operation );
                if ( value != null ) {
                    //if (value.length() > max) value = value.substring(0, max);
                    values.add( value );
                }
            }
        } catch ( WSDLException we ) {
            throw new ServiceResolutionException("Error accessing service WSDL '"+we.getMessage()+"'.", we);
        }

        return values;
    }

    /**
     * a set of distinct parameters for this service
     * @param candidateService object from which to extract parameters from
     * @return a Set containing distinct strings
     */
    public Set<T> getDistinctParameters(PublishedService candidateService) throws ServiceResolutionException {
        Set<T> out = new HashSet<T>();
        // non soap services do not have those parameters
        if (!candidateService.isSoap()) {
            out.add(null);
            return out;
        }
        try {
            Wsdl wsdl = candidateService.parsedWsdl();
            // fla bugfix for 1827 soap bindings only should be considered for soap web services
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            Iterator operations = wsdl.getBindingOperations().iterator();
            BindingOperation operation;
            while (operations.hasNext()) {
                operation = (BindingOperation)operations.next();
                T value = getTargetValue(wsdl.getDefinition(), operation);
                out.add(value); // Bug 1741: Ensure we don't return an empty set, it confuses ResolutionManager.
            }
        } catch ( WSDLException we ) {
            throw new ServiceResolutionException("Error accessing service WSDL '"+we.getMessage()+"'.", we);
        }
        
        return out;
    }

    protected abstract T getTargetValue(Definition def, BindingOperation operation);

    public boolean isApplicableToMessage(Message msg) throws ServiceResolutionException {
        try {
            return msg.isSoap();
        } catch (Exception e) {
            throw new ServiceResolutionException("Unable to determine whether message is SOAP", e);
        }
    }
}
