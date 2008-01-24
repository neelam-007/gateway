package com.l7tech.external.assertions.whichmodule;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * A utility module that shows info about the module that provided a currently-loaded assertion.
 */
public class WhichModuleAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(WhichModuleAssertion.class.getName());

    private String assertionXml = "";

    public String getAssertionXml() {
        return assertionXml;
    }

    public void setAssertionXml(String assertionXml) {
        if (assertionXml == null)
            throw new IllegalArgumentException("assertionXml may not be null");
        this.assertionXml = assertionXml;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata("module.exists", false, false, null, false, DataType.BOOLEAN),
                new VariableMetadata("module.name", false, false, null, false, DataType.STRING),
                new VariableMetadata("module.sha1", false, false, null, false, DataType.STRING),
                new VariableMetadata("module.assertions", false, true, null, false, DataType.STRING),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.LONG_NAME, "Gets info about the module that offers the specified assertion (identified by its XML representation); fails if it is unrecognized");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ServerLogs.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, getClass().getName() + "$ServerImpl");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, getClass().getName() + "$PropDialog");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, getClass().getName() + "$Validator");
        return meta;
    }

    public static class ServerImpl extends AbstractServerAssertion<WhichModuleAssertion> {
        private final Auditor auditor;
        private final WspReader wspReader;
        private final ServerAssertionRegistry assertionRegistry;

        public ServerImpl(WhichModuleAssertion assertion, ApplicationContext context) {
            super(assertion);
            this.auditor = new Auditor(this, context, logger);
            this.wspReader = (WspReader)context.getBean("wspReader", WspReader.class);
            this.assertionRegistry = (ServerAssertionRegistry)context.getBean("assertionRegistry", ServerAssertionRegistry.class);
        }

        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            try {
                Assertion ass = wspReader.parseStrictly(getAssertion().getAssertionXml());
                if (ass == null) throw new IOException("Parsed AssertionXml produced null policy");

                AssertionModule module = assertionRegistry.getModuleForClassLoader(ass.getClass().getClassLoader());
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
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { ExceptionUtils.getMessage(e) }, e);
                return AssertionStatus.SERVER_ERROR;
            }
        }
    }

    public static class PropDialog extends AssertionPropertiesEditorSupport<WhichModuleAssertion> {
        private JTextArea field = new JTextArea();
        private JButton ok;
        private boolean confirmed = false;

        public PropDialog(Frame parent, WhichModuleAssertion bean) {
            super(parent, "Assertion XML", true);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JScrollPane(field), BorderLayout.CENTER);
            panel.add(makeButtons(), BorderLayout.SOUTH);
            setContentPane(panel);
            if (bean != null) setData(bean);
            panel.setPreferredSize(new Dimension(630, 470));
            pack();
        }

        private JPanel makeButtons() {
            ok = new JButton("Ok");
            JButton cancel = new JButton("Cancel");
            Utilities.equalizeButtonSizes(new JButton[] { ok, cancel });
            Utilities.setEscKeyStrokeDisposes(this);
            getRootPane().setDefaultButton(ok);

            ActionListener al = new ActionListener() {
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

        public boolean isConfirmed() {
            return confirmed;
        }

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
        }

        public WhichModuleAssertion getData(WhichModuleAssertion assertion) {
            assertion.setAssertionXml(field.getText());
            return assertion;
        }

        @Override
        protected void configureView() {
            ok.setEnabled( !isReadOnly() );
        }
    }

    public static class Validator implements AssertionValidator {
        private final WhichModuleAssertion assertion;
        private Throwable xmlProblem = null;

        public Validator(WhichModuleAssertion a) {
            assertion = a;
            try {
                WspReader.getDefault().parseStrictly(a.getAssertionXml());
            } catch (Exception e) {
                xmlProblem = e;
            }
        }

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
            if (xmlProblem != null)
                result.addError(
                        new PolicyValidatorResult.Error(assertion,
                                                        path,
                                                        "Invalid policy assertion XML: " + ExceptionUtils.getMessage(xmlProblem),
                                                        xmlProblem));
        }
    }
}
