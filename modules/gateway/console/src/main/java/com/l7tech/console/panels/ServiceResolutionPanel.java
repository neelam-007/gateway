package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import static com.l7tech.gui.util.Utilities.comboBoxModel;
import static com.l7tech.gui.widgets.TextListCellRenderer.basicComboBoxRenderer;

import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * A wizard step panel configures a service resolution path.
 *
 * @author ghuang
 */
public class ServiceResolutionPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ServiceResolutionPanel");
    private static final String STD_PORT = ":8080";
    private static final String STD_PORT_DISPLAYED = ":[port]";

    private JPanel mainPanel;
    private JRadioButton noURIRadio;
    private JRadioButton customURIRadio;
    private JEditorPane ssgRoutingUrlPane;
    private JComboBox customUri;

    private String ssgUrl;
    private PublishServiceWizard.ServiceAndAssertion subject;

    public ServiceResolutionPanel() {
        super(null);
        initialize();
    }

    @Override
    public String getDescription() {
        return resources.getString("step.description");
    }

    @Override
    public String getStepLabel() {
        return resources.getString("step.label");
    }

    @Override
    public boolean onNextButton() {
        if (subject != null) {
            PublishedService service = subject.getService();
            service.setRoutingUri(null);

            if (customURIRadio.isSelected()) {
                String routingUri = ((String)customUri.getSelectedItem()).trim();
                //bug 11529 check if the uri is using one of the reserved path elements
                if (! routingUri.isEmpty()) {
                    final String message = ValidatorUtils.validateResolutionPath(routingUri, subject.getService().isSoap(), subject.getService().isInternal());
                    if ( message != null ) {
                        JOptionPane.showMessageDialog(this, message);
                        return false;
                    }
                    service.setRoutingUri(routingUri);
                }
            }
        }
        return true;
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof PublishServiceWizard.ServiceAndAssertion) {
            subject = (PublishServiceWizard.ServiceAndAssertion)settings;
            customUri.setModel( comboBoxModel( subject.getCustomUriOptions() ) );
            customUri.setSelectedItem( subject.getService().getRoutingUri()==null ? "" : subject.getService().getRoutingUri() );
        }
    }

    private void initialize() {
        String hostname = TopComponents.getInstance().ssgURL().getHost();
        ssgUrl = "http://" + hostname + STD_PORT;

        // By default, the radio button 'No resolution path' is on, so uriField is disabled.
        customUri.setEnabled(false);
        customUri.setRenderer( basicComboBoxRenderer() );

        ActionListener toggleUriField = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                customUri.setEnabled(customURIRadio.isSelected());
                updateUrl();
            }
        };
        noURIRadio.addActionListener(toggleUriField);
        customURIRadio.addActionListener(toggleUriField);

        final JTextField uriField = (JTextField)customUri.getEditor().getEditorComponent();
        uriField.setDocument( new FilterDocument( 128, null ) );
        uriField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                //always start with "/" for URI except an empty uri.
                String uri = uriField.getText();
                if ( uri != null && !uri.isEmpty() && !uri.startsWith( "/" ) ) {
                    uri = "/" + uri.trim();
                    uriField.setText( uri );
                }

                updateUrl();
            }
        } );
        customUri.addActionListener( new RunOnChangeListener(){
            @Override
            protected void run() {
                updateUrl();
            }
        } );
        customUri.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (customURIRadio.isSelected()) {
                    String url = updateUrl();
                    if (url != null && !url.startsWith("/")) url = "/" + url;
                    uriField.setText(url);
                }
            }
        });

        // The ssgRoutingUrlPane should be initialized.
        updateUrl();

        add(mainPanel);
    }

    private String updateUrl() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            final JTextField uriField = (JTextField)customUri.getEditor().getEditorComponent();
            currentValue = uriField.getText();
        }
        String urlValue;
        if (currentValue != null) {
            currentValue = currentValue.trim();
            String cvWithoutSlashes = currentValue.replace("/", "");
            if (cvWithoutSlashes.length() <= 0) {
                currentValue = null;
            }
        }
        if (currentValue == null || currentValue.length() < 1) {
            urlValue = ssgUrl + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlValue = ssgUrl + currentValue;
            } else {
                urlValue = ssgUrl + "/" + currentValue;
            }
        }

        String tmp = urlValue.replace(STD_PORT, STD_PORT_DISPLAYED);
        tmp = tmp.replace("http://", "http(s)://");
        ssgRoutingUrlPane.setText("<html><a href=\"" + urlValue + "\">" + tmp + "</a></html>");
        ssgRoutingUrlPane.setCaretPosition(0);
        return currentValue;
    }
}
