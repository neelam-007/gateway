package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl extends RemoteService implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());

    public CustomAssertionsRegistrarImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        Set customAssertionDescriptors = CustomAssertions.getAssertions();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        final Set customAssertionDescriptors = CustomAssertions.getAssertions(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    private Collection asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection result = new ArrayList();
        Iterator it = customAssertionDescriptors.iterator();
        while (it.hasNext()) {
            try {
                Class ca = (Class)it.next();
                CustomAssertionHolder ch = new CustomAssertionHolder();
                ch.setCustomAssertion((CustomAssertion)ca.newInstance());
                result.add(ch);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
            }
        }
        return result;
    }
}