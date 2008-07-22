package com.l7tech.gateway.common.spring.remoting.rmi;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.remoting.rmi.RmiRegistryFactoryBean;

import com.l7tech.util.ExceptionUtils;

/**
 * An extension of the standard Spring RMI Registry that expects to create the Registry.
 *
 * <p>The only reason this exists is for better logging on startup (error if registry
 * already exists, no warning for creation).</p>
 *
 * <p>NOTE, you need to ensure you set the log level for this class to SEVERE.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class RmiRegistryInitializer extends RmiRegistryFactoryBean {

    //- PROTECTED

    /**
	 * Locate or create the RMI registry.
     *
	 * @param registryPort the registry port to use
	 * @param clientSocketFactory the RMI client socket factory for the registry (if any)
	 * @param serverSocketFactory the RMI server socket factory for the registry (if any)
	 * @return the RMI registry
	 * @throws java.rmi.RemoteException if the registry couldn't be located or created
	 */
	protected Registry getRegistry(int registryPort,
                                   RMIClientSocketFactory clientSocketFactory,
                                   RMIServerSocketFactory serverSocketFactory)
			throws RemoteException {

		if (clientSocketFactory != null) {
			try {
				// Retrieve existing registry.
				Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
				testRegistry(reg);

                // we expect the above to throw
                logger.severe("RMI registry already exists!");

                return reg;
			}
			catch (RemoteException ex) {
                // it is expected that the registry does not yet exist
                logger.log(Level.FINEST, "RMI registry access threw exception", ExceptionUtils.getDebugException(ex));
			}
		}

        return super.getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
	}

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(RmiRegistryInitializer.class.getName());
}
