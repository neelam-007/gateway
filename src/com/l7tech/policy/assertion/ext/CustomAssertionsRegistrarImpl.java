package com.l7tech.policy.assertion.ext;

import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;
import java.io.IOException;

import net.jini.config.ConfigurationException;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl extends RemoteService implements CustomAssertionsRegistrar {

    public CustomAssertionsRegistrarImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public List getAssertions() throws RemoteException {
        return new ArrayList();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public List getAssertions(Category c) throws RemoteException {
        return new ArrayList();
    }
}