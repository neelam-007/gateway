package com.l7tech.server.policy.custom;

import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.policy.ServerAssertionRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionsRegistrarImplTest {
    @Mock @SuppressWarnings("unused")
    private ServerAssertionRegistry serverAssertionRegistry;

    @Test
    public void registerCustomExtensionInterface() throws Exception {
        ExtensionInterfaceManager extensionInterfaceManager = new ExtensionInterfaceManager(null, null, null);
        CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl = new CustomAssertionsRegistrarImpl(serverAssertionRegistry, extensionInterfaceManager);

        // register MyCustomExtensionInterfaceBinding which holds an implementation of MyInterface
        customAssertionsRegistrarImpl.registerCustomExtensionInterface("com.l7tech.server.policy.custom.CustomAssertionsRegistrarImplTest$MyCustomExtensionInterfaceBinding", ClassLoader.getSystemClassLoader());

        // verify registration
        assertTrue(extensionInterfaceManager.isInterfaceRegistered("com.l7tech.server.policy.custom.CustomAssertionsRegistrarImplTest$MyInterface", null));
        assertFalse(extensionInterfaceManager.isInterfaceRegistered("com.l7tech.server.policy.custom.CustomAssertionsRegistrarImplTest$DoesNotExist", null));
    }

    private interface MyInterface {
        String echo(String in);
    }

    @SuppressWarnings("unused")
    private static class MyCustomExtensionInterfaceBinding extends CustomExtensionInterfaceBinding {
        public MyCustomExtensionInterfaceBinding() {
            super(MyInterface.class, new MyInterface() {
                @Override
                public String echo(String in) {
                    return "Echo: " + in;
                }
            });
        }
    }
}
