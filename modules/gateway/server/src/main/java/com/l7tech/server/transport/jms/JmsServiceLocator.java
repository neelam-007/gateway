package com.l7tech.server.transport.jms;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Cache JNDI lookup for InitialContext.
 */
public class JmsServiceLocator extends InitialContext {

    private Map<String, Object> cache = new HashMap<String, Object>();

    public JmsServiceLocator(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
    }

    @Override
    public Object lookup(String name) throws NamingException {
        Object o = cache.get(name);
        if (o == null) {
            o = super.lookup(name);
            cache.put(name, o);
        }
        return o;
    }
}
