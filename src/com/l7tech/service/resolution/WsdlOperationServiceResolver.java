/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service.resolution;

import com.l7tech.service.PublishedService;
import com.l7tech.service.Wsdl;
import com.l7tech.logging.LogManager;

import javax.wsdl.*;
import java.util.*;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class WsdlOperationServiceResolver extends NameValueServiceResolver {
    protected Object[] getTargetValues( PublishedService service ) {
        List values = new ArrayList(2);

        try {
            Wsdl wsdl = service.parsedWsdl();
            Iterator bindings = wsdl.getBindings().iterator();
            Binding binding;
            Iterator operations;
            while ( bindings.hasNext() ) {
                binding = (Binding)bindings.next();
                operations = binding.getBindingOperations().iterator();
                BindingOperation operation;
                while ( operations.hasNext() ) {
                    operation = (BindingOperation)operations.next();
                    String value = doGetValue( operation );
                    values.add( value );
                }
            }
        } catch ( WSDLException we ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, we);
        }

        return values.toArray();
    }

    protected abstract String doGetValue( BindingOperation operation );
}
