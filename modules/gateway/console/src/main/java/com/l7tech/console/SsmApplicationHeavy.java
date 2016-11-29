package com.l7tech.console;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.HelpUtil;
import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Thick-client version of SsmApplication.
 */
public class SsmApplicationHeavy extends SsmApplication  {
    private final Logger log = Logger.getLogger(getClass().getName());
    private static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.ccj.disableServices";
    private static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty( PROP_DISABLE_BLACKLISTED_SERVICES, true );
    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
        new Pair<>( "CertificateFactory", "X.509" ),
        new Pair<>( "KeyStore", "PKCS12" ),
        new Pair<>( "CertPathBuilder", "PKIX" ),
        new Pair<>( "CertPathValidator", "PKIX" ),
        new Pair<>( "CertStore", "Collection" )
    ));
    private static SsmApplication ssmApplication;
    private boolean running = false;

    public SsmApplicationHeavy() {
        if (ssmApplication != null) {
            throw new IllegalStateException("Already initialized");
        }
        ssmApplication = this;
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException("Policy Manager already running");
        }

        installAdditionalSecurityProviders();

        if (!isSuppressAutoLookAndFeel()) setAutoLookAndFeel();
        mainWindow = new MainWindow(this);
        TopComponents.getInstance().registerComponent("mainWindow", mainWindow);

        // Window listener
        mainWindow.setVisible(true);
        mainWindow.toFront();
        running = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainWindow.checkConfiguration();
                mainWindow.activateLogonDialog();
            }
        });
    }

    public boolean isApplet() {
        return false;
    }

    /**
     * set the look and feel
     *
     * @param lookAndFeel a string specifying the name of the class that implements
     *                    the look and feel
     */
    protected void setLookAndFeel
      (String
      lookAndFeel)
    {
        if (!isSuppressAutoLookAndFeel()) {
            setAutoLookAndFeel();
            return;
        }

        if (lookAndFeel == null) return;
        boolean lfSet = true;

        // if same look and feel quick exit
        if (lookAndFeel.
          equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            Object lafObject =
              Class.forName(lookAndFeel).newInstance();
            UIManager.setLookAndFeel((LookAndFeel)lafObject);
        } catch (Exception e) {
            lfSet = false;
        }
        // there was a problem setting l&f, try crossplatform one (best bet)
        if (!lfSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                return;
            }
        }
        // update panels with new l&f
        MainWindow mainWindow = getMainWindow();
        SwingUtilities.updateComponentTreeUI(mainWindow);
        mainWindow.validate();
    }

    /**
     * The "Help Topics".
     * This procedure displays the help contents in the preferred browser for the system on which the SSM is running.
     */
    public void showHelpTopicsRoot() {
        HelpUtil.showHelpTopicsRoot( TopComponents.getInstance().getTopParent(), false );
    }

    private void installAdditionalSecurityProviders() {
        final boolean asFirstProvider = SyspropUtil.getBoolean("com.l7tech.console.install.ccj.first.provider", true);

        final Provider provider = getCcjProvider();
        if (asFirstProvider) {
            Security.removeProvider("WF");
            Security.insertProviderAt(provider, 1);
            log.info("Registering CryptoComply as most-preferred crypto provider");
        } else {
            log.info("Registering CryptoComply as additional crypto provider");
        }

        if (DISABLE_BLACKLISTED_SERVICES) {
            ProviderUtil.removeService(SERVICE_BLACKLIST, provider);
        }
    }

    private Provider getCcjProvider() {
        try {
            return (Provider) getClass().getClassLoader().loadClass("com.safelogic.cryptocomply.jce.provider.SLProvider").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
