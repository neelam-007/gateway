package com.l7tech.console.panels;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.Functions;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.FindException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Second step in the ImportPolicyFromUDDIWizard.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField nameField;
    private JButton searchButton;
    private JList policyList;
    private JComboBox uddiRegistriesComboBox;
    private ImportPolicyFromUDDIWizard.Data data;
    private static final Logger logger = Logger.getLogger(ImportPolicyFromUDDIWizardStep.class.getName());
    private boolean readyToAdvance = false;

    public ImportPolicyFromUDDIWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    @Override
    public String getDescription() {
        return "Search UDDI Directory for Policy and Import it";
    }

    @Override
    public String getStepLabel() {
        return "Select Policy From UDDI";
    }

    @Override
    public boolean canAdvance() {
        return readyToAdvance;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean onNextButton() {
        boolean res = false;
        ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            UDDINamedEntity entity = (UDDINamedEntity) policyList.getSelectedValue();
            res = importPolicy(entity.getPolicyUrl());
        } finally {
            ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        return res;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        uddiRegistriesComboBox.setRenderer(  new TextListCellRenderer<UDDIRegistry>( new Functions.Unary<String, UDDIRegistry>(){
            @Override
            public String call( final UDDIRegistry uddiRegistry ) {
                return uddiRegistry.getName();
            }
        }, null, false));
        uddiRegistriesComboBox.setModel( new DefaultComboBoxModel( loadUDDIRegistries() ) );

        policyList.setCellRenderer(new TextListCellRenderer<UDDINamedEntity>(new TextListCellProvider(true), new TextListCellProvider(false), false));

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    find();
                } finally {
                    ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        policyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (policyList.getSelectedIndex() < 0) {
                    readyToAdvance = false;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                } else {
                    readyToAdvance = true;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                }
            }
        });

        if ( uddiRegistriesComboBox.getModel().getSize()==0 ) {
            searchButton.setEnabled( false );           
        } else {
            uddiRegistriesComboBox.setSelectedIndex( 0 );   
        }
    }

    private UDDIRegistry[] loadUDDIRegistries() {
        final UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();

        Collection<UDDIRegistry> registries = Collections.emptyList();
        try {
            registries = uddiRegistryAdmin.findAllUDDIRegistries();
            for ( Iterator<UDDIRegistry> regIter = registries.iterator(); regIter.hasNext(); ) {
                UDDIRegistry registry = regIter.next();
                if ( !registry.isEnabled() ) {
                    regIter.remove();    
                }
            }
        } catch (FindException e) {
            logger.log( Level.WARNING, "Error loading UDDI registries", e );
        }

        UDDIRegistry[] result = registries.toArray(new UDDIRegistry[registries.size()]);
        Arrays.sort(result, new ResolvingComparator<UDDIRegistry,String>(new Resolver<UDDIRegistry, String>() {
            @Override
            public String resolve(UDDIRegistry key) {
                return key.getName().toLowerCase();
            }
        }, false));

        return result;
    }

    private void find() {
        String filter = nameField.getText();
        try {
            UDDINamedEntity[] policies = getServiceAdmin().findPoliciesFromUDDIRegistry(getSelectedRegistryGoid(), filter);
            policyList.setListData(policies);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error getting find result", e);
            showError("Error getting find result . " + ExceptionUtils.unnestToRoot(e));
        }
    }

    private Goid getSelectedRegistryGoid() {
        UDDIRegistry registry = (UDDIRegistry) uddiRegistriesComboBox.getSelectedItem();
        return registry.getGoid();
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (ImportPolicyFromUDDIWizard.Data)settings;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30),
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean importPolicy(String policyURL) {
        boolean imported = false;

        if ( policyURL != null ) {
            // try to get a policy document
            try {
                URL url = new URL(policyURL);
                URLConnection urlConn = url.openConnection();
                urlConn.setConnectTimeout( ConfigFactory.getIntProperty( "com.l7tech.console.policyImportConnectTimeout", 30000 ) );
                urlConn.setReadTimeout( ConfigFactory.getIntProperty( "com.l7tech.console.policyImportReadTimeout", 60000 ) );
                urlConn.setUseCaches( false );
                Document doc = XmlUtil.parse(urlConn.getInputStream());

                if (XmlUtil.findFirstChildElementByName(doc,
                                                        new String[] {WspConstants.L7_POLICY_NS,
                                                                      SoapConstants.WSP_NAMESPACE,
                                                                      SoapConstants.WSP_NAMESPACE2},
                                                        WspConstants.POLICY_ELNAME) != null) {
                    String output;
                    try {
                        output = XmlUtil.nodeToFormattedString(doc);
                    } catch (IOException e) {
                        // cannot happen
                        throw new RuntimeException(e);
                    }
                    data.setPolicyXML(output);
                    imported = true;
                } else {
                    logger.info("xml document resolved from " + policyURL + " was not a policy: " +
                                XmlUtil.nodeToFormattedString(doc));

                    showError("Referenced document was not a policy");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + policyURL);
                showError("Error accessing reference: " + ExceptionUtils.getMessage(e));
            } catch (SAXException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + policyURL);
                showError("Error accessing reference: " + ExceptionUtils.getMessage(e));
            }
        }

        return imported;
    }

    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        return Registry.getDefault().getUDDIRegistryAdmin();
    }

    private ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    /**
     * Function for accessing either the display or tooltip text.
     */
    private static final class TextListCellProvider implements Functions.Unary<String, UDDINamedEntity> {
        private final boolean showName;

        private TextListCellProvider(final boolean showName) {
            this.showName = showName;    
        }

        @Override
        public String call(final UDDINamedEntity uddiNamedEntity) {
            String text;

            if ( showName ) {
                // generate the entry text
                String tModelName = uddiNamedEntity.getName();
                String tModelKey = uddiNamedEntity.getKey();
                text = tModelName + " [" + tModelKey + "]";
            } else {
                // generate the tooltip text
                text = uddiNamedEntity.getPolicyUrl();
            }

            return text;
        }
    }
}
