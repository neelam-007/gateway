package com.l7tech.external.assertions.whichmodule;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.search.Dependency;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.DELETE_MULTI;
import static com.l7tech.objectmodel.EntityType.SSG_KEY_ENTRY;
import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;

/**
 * A utility module that shows info about the module that provided a currently-loaded assertion.
 */
public class WhichModuleAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(WhichModuleAssertion.class.getName());

    private String assertionXml = "";
    private Goid demoGenericGoid;

    public String getAssertionXml() {
        return assertionXml;
    }

    public void setAssertionXml(String assertionXml) {
        if (assertionXml == null)
            throw new IllegalArgumentException("assertionXml may not be null");
        this.assertionXml = assertionXml;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata("module.exists", false, false, null, false, DataType.BOOLEAN),
                new VariableMetadata("module.name", false, false, null, false, DataType.STRING),
                new VariableMetadata("module.sha1", false, false, null, false, DataType.STRING),
                new VariableMetadata("module.assertions", false, true, null, false, DataType.STRING),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.DESCRIPTION, "Gets info about the module that offers the specified assertion (identified by its XML representation); fails if it is unrecognized");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ServerLogs.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, getClass().getName() + "$ServerImpl");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, getClass().getName() + "$PropDialog");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, getClass().getName() + "$Validator");
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[]{getClass().getName() + "$CustomAction"});
        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<DemoExtensionInterface>(DemoExtensionInterface.class, null, new DemoExtensionInterface() {
                    @Override
                    public DemoReturnVal[] demoHello(DemoArgument[] args) {
                        return new DemoReturnVal[]{new DemoReturnVal("Hello from Gateway anon inner class!  You sent: " + args[0].s)};
                    }
                });
                return Collections.singletonList(binding);
            }
        });

        return meta;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.GENERIC)
    public Goid getDemoGenericGoid() {
        return demoGenericGoid;
    }

    public void setDemoGenericGoid(Goid demoGenericGoid) {
        this.demoGenericGoid = demoGenericGoid;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class CustomAction extends AbstractAction {
        public CustomAction() {
            super("Which Module Custom Action", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final DemoExtensionInterface demoInterface = Registry.getDefault().getExtensionInterface(DemoExtensionInterface.class, null);
            DemoExtensionInterface.DemoReturnVal[] response = demoInterface.demoHello(new DemoExtensionInterface.DemoArgument[]{new DemoExtensionInterface.DemoArgument("Info from SSM packed inside DemoArgument")});
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Which Module Assertion .AAR File", "Response from Gateway from DemoReturnVal: " + response[0].v, null);
        }
    }

    /**
     * A sample interface to demo use of an admin extension interface.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    @Administrative
    @Secured(types = TRUSTED_CERT)
    public static interface DemoExtensionInterface {

        // Demonstrate passing arguments and return values that are themselves classes loaded from the .aar
        // Naturally, passing serializable JDK classes works as well, as does passing classes shared between the core gateway and SSM (as long as they are serializable)
        @Secured(stereotype = DELETE_MULTI, types = SSG_KEY_ENTRY)
        public DemoReturnVal[] demoHello(DemoArgument[] args);

        public static class DemoArgument implements Serializable {
            public final String s;

            public DemoArgument(String s) {
                this.s = s;
            }
        }

        public static class DemoReturnVal implements Serializable {
            public final StringBuilder v;

            public DemoReturnVal(String v) {
                this.v = new StringBuilder(v);
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class ServerImpl extends AbstractServerAssertion<WhichModuleAssertion> {

        private final WspReader wspReader;
        private final ServerAssertionRegistry assertionRegistry;

        public ServerImpl(WhichModuleAssertion assertion, ApplicationContext context) {
            super(assertion);
            this.wspReader = context.getBean("wspReader", WspReader.class);
            this.assertionRegistry = context.getBean("assertionRegistry", ServerAssertionRegistry.class);
        }

        @Override
        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            try {
                Assertion ass = wspReader.parseStrictly(getAssertion().getAssertionXml(), WspReader.INCLUDE_DISABLED);
                if (ass == null) throw new IOException("Parsed AssertionXml produced null policy");

                ModularAssertionModule module = assertionRegistry.getModuleForClassLoader(ass.getClass().getClassLoader());
                if (module == null) {
                    context.setVariable("module.exists", Boolean.FALSE);
                    context.setVariable("module.name", "(core assertion)");
                    context.setVariable("module.sha1", "");
                    context.setVariable("module.assertions", "");
                    return AssertionStatus.NONE;
                }

                Collection<String> assclasses = new ArrayList<String>();
                for (Assertion proto : module.getAssertionPrototypes())
                    assclasses.add(proto.getClass().getName());

                context.setVariable("module.exists", Boolean.TRUE);
                context.setVariable("module.name", module.getName());
                context.setVariable("module.sha1", module.getSha1());
                context.setVariable("module.assertions", assclasses.toString());

                return AssertionStatus.NONE;

            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{ExceptionUtils.getMessage(e)}, e);
                return AssertionStatus.SERVER_ERROR;
            }
        }
    }

    public static class PropDialog extends AssertionPropertiesEditorSupport<WhichModuleAssertion> {
        private JTextArea field = new JTextArea();
        private JComboBox<String> demoGenericEntityCombo = new JComboBox<>();
        private JButton ok;
        private boolean confirmed = false;

        public PropDialog(Window parent, WhichModuleAssertion bean) {

            super(parent, "Assertion XML");
            JPanel panel = new JPanel(new BorderLayout());
            JPanel contentPanel = new JPanel(new FlowLayout());
            contentPanel.add(new JLabel("Demo Generic Entity: "));
            contentPanel.add(demoGenericEntityCombo);
            panel.add(contentPanel, BorderLayout.NORTH);
            panel.add(new JScrollPane(field), BorderLayout.CENTER);
            panel.add(new JScrollPane(field), BorderLayout.CENTER);
            panel.add(makeButtons(), BorderLayout.SOUTH);
            setContentPane(panel);
            if (bean != null) setData(bean);
            panel.setPreferredSize(new Dimension(630, 470));
            pack();
        }

        private JPanel makeButtons() {
            ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            Utilities.equalizeButtonSizes(new JButton[]{ok, cancel});
            Utilities.setEscKeyStrokeDisposes(this);
            getRootPane().setDefaultButton(ok);

            ActionListener al = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmed = e.getActionCommand().equals(ok.getActionCommand());
                    PropDialog.this.dispose();
                }
            };
            ok.addActionListener(al);
            cancel.addActionListener(al);

            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
            p.add(Box.createHorizontalGlue());
            p.add(ok);
            p.add(cancel);
            return p;
        }

        @Override
        public boolean isConfirmed() {
            return confirmed;
        }

        @Override
        public void setData(WhichModuleAssertion assertion) {
            String xml = assertion.getAssertionXml();
            try {
                xml = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(xml));
            } catch (SAXException e) {
                // ignore
            } catch (IOException e) {
                // can't happen
            }
            field.setText(xml);

            DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
            try {
                Collection<DemoGenericEntity> demoGenericEntities = getEntityManager().findAll();
                comboBoxModel.addElement("<Select Entity>");
                for (DemoGenericEntity demoGenericEntity : demoGenericEntities) {
                    comboBoxModel.addElement(demoGenericEntity.getName() + ":" + demoGenericEntity.getId());
                }
            } catch (FindException e) {
                comboBoxModel.removeAllElements();
                comboBoxModel.addElement("Error Listing Entities.");
            }
            if (assertion.getDemoGenericGoid() != null) {
                for (int i = 1; i < comboBoxModel.getSize(); i++) {
                    if (assertion.getDemoGenericGoid().toString().equals(comboBoxModel.getElementAt(i).split(":")[1])) {
                        comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(i));
                        break;
                    }
                }
            }
            demoGenericEntityCombo.setModel(comboBoxModel);
        }

        private static DemoGenericEntityAdmin getEntityManager() {
            return Registry.getDefault().getExtensionInterface(DemoGenericEntityAdmin.class, null);
        }

        @Override
        public WhichModuleAssertion getData(WhichModuleAssertion assertion) {
            assertion.setAssertionXml(field.getText());
            if (demoGenericEntityCombo.getSelectedIndex() > 0) {
                assertion.setDemoGenericGoid(Goid.parseGoid(demoGenericEntityCombo.getSelectedItem().toString().split(":")[1]));
            } else {
                assertion.setDemoGenericGoid(null);
            }
            return assertion;
        }

        @Override
        protected void configureView() {
            ok.setEnabled(!isReadOnly());
        }
    }

    public static class Validator implements AssertionValidator {
        private final WhichModuleAssertion assertion;
        private Throwable xmlProblem = null;

        public Validator(WhichModuleAssertion a) {
            assertion = a;
            try {
                WspReader.getDefault().parseStrictly(a.getAssertionXml(), WspReader.INCLUDE_DISABLED);
            } catch (Exception e) {
                xmlProblem = e;
            }
        }

        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            if (xmlProblem != null)
                result.addError(
                        new PolicyValidatorResult.Error(assertion,
                                "Invalid policy assertion XML: " + ExceptionUtils.getMessage(xmlProblem),
                                xmlProblem));
        }
    }
}
