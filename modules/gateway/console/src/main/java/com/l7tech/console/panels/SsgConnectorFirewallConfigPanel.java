package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.InetAddressUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: kdiep
 * Date: 11/26/12
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class SsgConnectorFirewallConfigPanel extends CustomTransportPropertiesPanel {

    private JPanel contentPanel;

    private JComboBox ddTable;
    private JComboBox ddChain;
    private JComboBox ddTarget;
    private JComboBox ddProtocol;
    private JPanel tcpOptionsPanel;
    private JPanel udpOptionsPanel;
    private JPanel implicitOptionsPanel;
    private JPanel targetOptionsPanel;
    private SquigglyTextField source;
    private SquigglyTextField destination;
    private SquigglyTextField udpSrcPort;

    private SquigglyTextField tcpSrcPort;

    private SquigglyTextField tcpFlags;
    private SquigglyTextField tcpOptions;
    private JPanel icmpOptionsPanel;
    private JTextField icmpType;
    private JPanel targetHolderPanel;

    private TargetOptionsPanel targetOptions;

    private static final Set<String> AVAILABLE_TCP_FLAGS;
    static {
        AVAILABLE_TCP_FLAGS = new HashSet<String>();
        AVAILABLE_TCP_FLAGS.add("FIN");
        AVAILABLE_TCP_FLAGS.add("SYN");
        AVAILABLE_TCP_FLAGS.add("RST");
        AVAILABLE_TCP_FLAGS.add("PSH");
        AVAILABLE_TCP_FLAGS.add("ACK");
        AVAILABLE_TCP_FLAGS.add("URG");
        AVAILABLE_TCP_FLAGS.add("ALL");
        AVAILABLE_TCP_FLAGS.add("NONE");
    }
    private static final String[] AVAILABLE_TABLE = new String[]{"filter", "NAT"};

    private static final String[] BASE_TARGET = new String[]{"ACCEPT", "DROP"};

    private static final String[] AVAILABLE_PROTOCOL = new String[]{"tcp", "udp", "icmp"};

    private static final Map<String, String[]> CHAIN_FILTER;
    static {
        Map<String, String[]> m = new HashMap<String, String[]>();
        m.put("filter", new String[]{"INPUT", "FORWARD"}); //OUTPUT is not required
        m.put("NAT", new String[]{"PREROUTING", "POSTROUTING"}); //OUTPUT is not required

        CHAIN_FILTER = Collections.unmodifiableMap(m);
    }

    private InputValidator inputValidator;

    public SsgConnectorFirewallConfigPanel() {
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        inputValidator = new InputValidator(this, "Firewall Settings");
        initialize();
    }

    private static final String[] ADVANCE_PROPERTIES = new String[]{"table", "chain", "jump", "protocol",
        "source", "destination", "source-port", "destination-port", "tcp-flags", "tcp-option", "to-ports", "to-destination", "icmp-type"};

    @Override
    public void setData(final Map<String, String> advancedProperties) {
        ddTable.setSelectedItem(advancedProperties.get("table"));
        ddChain.setSelectedItem(advancedProperties.get("chain"));
        ddTarget.setSelectedItem(advancedProperties.get("jump"));
        final String protocol = advancedProperties.get("protocol");
        ddProtocol.setSelectedItem(protocol);
        source.setText(advancedProperties.get("source"));
        destination.setText(advancedProperties.get("destination"));

        if("udp".equals(protocol)){
            udpSrcPort.setText(advancedProperties.get("source-port"));
        }
        else if("tcp".equals(protocol)){
            tcpSrcPort.setText(advancedProperties.get("source-port"));
        }
        else if("icmp".equals(protocol)){
            icmpType.setText(advancedProperties.get("icmp-type"));
        }

        tcpFlags.setText(advancedProperties.get("tcp-flags"));
        tcpOptions.setText(advancedProperties.get("tcp-option"));

        if(targetOptions != null){
            targetOptions.setFormValues(advancedProperties);
        }
    }

    @Override
    public Map<String, String> getData() {
        final Map<String, String> values = new HashMap<String, String>();
        String selectedTarget = ddTarget.getSelectedItem().toString();
        values.put("table", ddTable.getSelectedItem().toString());
        values.put("chain", ddChain.getSelectedItem().toString());
        values.put("jump", selectedTarget);

        final String src = source.getText().trim();
        if(!src.isEmpty()) values.put("source", src);
        final String dst = destination.getText().trim();
        if(!dst.isEmpty()) values.put("destination", dst);

        final String protocol = ddProtocol.getSelectedItem().toString();
        String srcPort = "";

        if("udp".equals(protocol)){
            srcPort = udpSrcPort.getText().trim();
        } else if ("tcp".equals(protocol)){
            srcPort = tcpSrcPort.getText().trim();
        } else if ("icmp".equals(protocol)){
            final String icmp = icmpType.getText().trim();
            if(!icmp.trim().isEmpty()) values.put("icmp-type", icmp);
        }

        if(!srcPort.isEmpty()) values.put("source-port", srcPort);

        values.put("protocol", protocol);
        final String flags = tcpFlags.getText().trim();
        if(!flags.isEmpty()) values.put("tcp-flags", flags);
        final String option = tcpOptions.getText().trim();
        if(!option.isEmpty()) values.put("tcp-option", option);

        //retrieve data from view
        if(targetOptions != null && TARGET_OPTIONS.containsKey(selectedTarget)){
            values.putAll(targetOptions.getFormValues());
        }
        return values;
    }

    @Override
    public String getValidationError() {
        if(!targetOptionsPanel.isVisible()) return null;
        if(targetOptions != null){
            for(Component c : targetOptions.getComponents()){
                if(c instanceof SquigglyTextField){
                    String feedback = ((SquigglyTextField) c).getModelessFeedback();
                    String text = ((SquigglyTextField) c).getText();
                    if(text == null || text.trim().isEmpty()){
                        c.requestFocusInWindow();
                        return "Target option(s) is required";
                    }
                    if(feedback != null && !feedback.isEmpty()){
                        c.requestFocusInWindow();
                        return feedback;
                    }
                }
            }
        }
        final String validationMessage = inputValidator.validate();
        if(validationMessage != null) return validationMessage;
        return null;
    }

    @Override
    public String[] getAdvancedPropertyNamesUsedByGui() {
        return ADVANCE_PROPERTIES;
    }

    private void initialize(){
        icmpType.setEnabled(false);
        fireActionSelectionChange();
        ddTarget.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                fireActionSelectionChange();
            }
        });

        ddTable.setModel(new DefaultComboBoxModel(AVAILABLE_TABLE));
        fireTableSelectionChange();
        ddTable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                fireTableSelectionChange();
            }
        });

        ddProtocol.setModel(new DefaultComboBoxModel(AVAILABLE_PROTOCOL));
        fireProtocolSelectionChange();
        ddProtocol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                fireProtocolSelectionChange();
            }
        });
        ddChain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateTarget();
            }
        });
        addValidationRules();
    }

    private void addValidationRules(){
        inputValidator.addRule(new InvertablePortValidator(udpSrcPort));
        inputValidator.addRule(new InvertablePortValidator(tcpSrcPort));
        inputValidator.addRule(new InvertableIpAddressValidator(source));
        inputValidator.addRule(new InvertableIpAddressValidator(destination));

        inputValidator.addRule(new InputValidator.ComponentValidationRule(source) {
            @Override
            public String getValidationError() {
                String src = source.getText().trim();
                if(!src.isEmpty()){
                    String dst = destination.getText().trim();
                    if(!dst.isEmpty()){
                        if(InetAddressUtil.isValidIpv4Address(src) && InetAddressUtil.isValidIpv4Address(dst)){
                            return null;
                        }
                        if(InetAddressUtil.isValidIpv6Address(src) && InetAddressUtil.isValidIpv6Address(dst)){
                            return null;
                        }
                        return "A rule can not contain both IPv4 and IPv6 addresses.";
                    }
                }
                return null;
            }
        });
        inputValidator.addRule(inputValidator.constrainTextFieldToBeNonEmpty("ICMP Type", icmpType, null));
        inputValidator.addRule(new InputValidator.ComponentValidationRule(tcpFlags) {
            @Override
            public String getValidationError() {
                if(!tcpOptionsPanel.isVisible()) return null;
                String flags = tcpFlags.getText().trim();
                if(!flags.isEmpty()){
                    if(flags.indexOf("!") > -1){
                        flags = flags.substring(flags.indexOf("!") + 1).trim();
                    }
                    String[] fields = flags.split("\\s+");
                    Set<String> set = new HashSet<String>();
                    for(String f : fields){
                        String[] cs = f.split(",");
                        for(String s : cs){
                            set.add(s.trim());
                        }
                    }
                    for(String s : set){
                        if(!s.isEmpty() && !AVAILABLE_TCP_FLAGS.contains(s)){
                            return s + " is not a supported TCP Flag";
                        }
                    }
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(tcpOptions) {
            @Override
            public String getValidationError() {
                if(!tcpOptionsPanel.isVisible()) return null;
                String options = tcpOptions.getText().trim();
                if(!options.isEmpty()){
                    if(options.indexOf("!") > -1){
                        options = options.substring(options.indexOf("!") + 1).trim();
                    }
                    try{
                        Integer num = Integer.parseInt(options);
                        if(num.intValue() < 0 || num.intValue() > 255){
                            return "TCP Option must be between 0 and 255";
                        }
                    }
                    catch(NumberFormatException e){
                        return "TCP Option must be a numeric number";
                    }
                }
                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(tcpFlags);
        inputValidator.validateWhenDocumentChanges(tcpOptions);
        inputValidator.validateWhenDocumentChanges(udpSrcPort);
        inputValidator.validateWhenDocumentChanges(tcpSrcPort);
        inputValidator.validateWhenDocumentChanges(icmpType);
        inputValidator.validateWhenDocumentChanges(source);
        inputValidator.validateWhenDocumentChanges(destination);
    }

    private void updateTarget(){
        //set the base target first
        ddTarget.setModel(new DefaultComboBoxModel(BASE_TARGET));

        //add other rules
        final String table = ddTable.getSelectedItem().toString();
        final String chain = ddChain.getSelectedItem().toString();
        if("NAT".equals(table) && ("PREROUTING".equals(chain) || "OUTPUT".equals(chain))){
            ddTarget.addItem("REDIRECT");
            ddTarget.addItem("DNAT");
        }
        fireActionSelectionChange();
    }

    private static final Map<String, java.util.List<InputField>> TARGET_OPTIONS;
    static {
        Map<String, java.util.List<InputField>> m = new HashMap<String, java.util.List<InputField>>();
        //REDIRECT
        java.util.List<InputField> r = new ArrayList<InputField>();
        r.add(new InputField("To Port:", "to-ports", new PortRangeDocumentListener()));
        m.put("REDIRECT", r);

        //DNAT
        java.util.List<InputField> d = new ArrayList<InputField>();
        d.add(new InputField("To Destination:", "to-destination", new IpAddressDocumentListener()));
        m.put("DNAT", d);

        TARGET_OPTIONS = Collections.unmodifiableMap(m);

    }

    private void fireActionSelectionChange() {
        final Object selected = ddTarget.getSelectedItem();
        final java.util.List<InputField> fields = TARGET_OPTIONS.get(selected);

        targetOptionsPanel.removeAll();
        final boolean hasOptions = fields != null && !fields.isEmpty();
        if(hasOptions){
            targetOptionsPanel.setLayout(new BorderLayout());
            targetOptions = new TargetOptionsPanel(fields);
            targetOptionsPanel.add(targetOptions, BorderLayout.CENTER);
        }
        targetOptionsPanel.setVisible(hasOptions);
        final JDialog parent = getSsgConnectorPropertiesDialog();
        if(parent != null) DialogDisplayer.pack(parent);
    }

    private void fireTableSelectionChange() {
        final Object selected = ddTable.getSelectedItem();
        if(!"filter".equals(selected)){
            ddProtocol.removeItem("icmp");
        }
        else {
            ddProtocol.setModel(new DefaultComboBoxModel(AVAILABLE_PROTOCOL));
        }
        ddChain.setModel(new DefaultComboBoxModel(CHAIN_FILTER.get(selected)));
        updateTarget();
        fireActionSelectionChange();
    }

    private void fireProtocolSelectionChange(){
        final String selected = ddProtocol.getSelectedItem().toString();
        implicitOptionsPanel.setBorder(new TitledBorder(selected.toUpperCase() + " Options"));
        final boolean isTcp = "tcp".equals(selected);

        udpSrcPort.setEnabled(!isTcp);
        tcpSrcPort.setEnabled(isTcp);
        tcpOptionsPanel.setVisible(isTcp);
        udpOptionsPanel.setVisible("udp".equals(selected));

        final boolean isIcmp = "icmp".equals(selected);
        icmpType.setEnabled(isIcmp);
        icmpOptionsPanel.setVisible(isIcmp);
        targetHolderPanel.setVisible(!isIcmp);
    }

    private class TargetOptionsPanel extends JPanel {

        public TargetOptionsPanel(@NotNull final java.util.List<InputField> inputFields){
            setLayout(new GridLayoutManager(inputFields.size(), 2));
            renderForm(inputFields);
        }

        private void renderForm(@NotNull final java.util.List<InputField> inputFields){
            for(int row = 0; row < inputFields.size(); row++){
                final InputField field = inputFields.get(row);
                add(new JLabel(field.getLabel()), new GridConstraints(row, 0, 1, 1, 0, 1, 0, 0, null, null, null, 0, false));
                JTextField input = new SquigglyTextField();
                final DocumentListener listener = field.getDocumentListener();
                if(listener != null){
                    input.getDocument().putProperty("owner", input);
                    input.getDocument().addDocumentListener(listener);
                }
                input.setName(field.getFieldName());
                add(input, new GridConstraints(row, 1, 1, 1, 0, 1, 2, 0, null, null, null, 0, false));
            }
        }

        public Map<String, String> getFormValues(){
            final Map<String, String> values = new HashMap<String, String>();
            for(final Component c : getComponents()){
                if(c instanceof JTextComponent){
                    values.put(c.getName(), ((JTextComponent) c).getText());
                }
            }
            return values;
        }

        public void setFormValues(@NotNull final Map<String, String> data){
            for(final Component c : getComponents()){
                final String name = c.getName();
                if(name != null && !name.isEmpty() && c instanceof JTextComponent){
                    ((JTextComponent)c).setText(data.get(name));
                }
            }
        }
    }

    private static class InputField {
        private final String label;
        private final String fieldName;
        private final DocumentListener documentListener;

        private InputField(@NotNull final String label, @NotNull final String fieldName, @Nullable final DocumentListener documentListener) {
            this.label = label;
            this.fieldName = fieldName;
            this.documentListener = documentListener;
        }

        public String getLabel() {
            return label;
        }

        public String getFieldName() {
            return fieldName;
        }

        public DocumentListener getDocumentListener() {
            return documentListener;
        }
    }

    private static class PortRangeDocumentListener implements DocumentListener {
        private static final Pattern PORT_RANGE = Pattern.compile("(\\d{1,5})(?:-(\\d{1,5}))?");
        @Override
        public void insertUpdate(final DocumentEvent e) {
            changed(e);
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            changed(e);
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            changed(e);
        }

        private void changed(final DocumentEvent e){
            final Object owner = e.getDocument().getProperty("owner");
            if(owner instanceof SquigglyTextField){
                final SquigglyTextField input = (SquigglyTextField)owner;
                final String text = input.getText();
                String feedback;
                final Matcher matcher = PORT_RANGE.matcher(text);
                if(matcher.matches()){
                    String start = matcher.group(1);
                    String end = matcher.group(2);
                    feedback = validatePort(start, end);
                }
                else {
                    feedback = "Invalid port entry.";
                }
                input.setModelessFeedback(feedback);
            }
        }
    }

    private static class IpAddressDocumentListener implements DocumentListener {
        private static final Pattern IP_PORT_RANGE = Pattern.compile("((?:(?:0|1[0-9]{0,2}|2[0-9]?|2[0-4][0-9]|25[0-5]|[3-9][0-9]?)\\.){3}(?:0|1[0-9]{0,2}|2[0-9]?|2[0-4][0-9]|25[0-5]|[3-9][0-9]?))(?::(\\d{1,5})(?:-(\\d{1,5}))?)?" +
                "(?:-((?:(?:0|1[0-9]{0,2}|2[0-9]?|2[0-4][0-9]|25[0-5]|[3-9][0-9]?)\\.){3}(?:0|1[0-9]{0,2}|2[0-9]?|2[0-4][0-9]|25[0-5]|[3-9][0-9]?))(?::(\\d{1,5})(?:-(\\d{1,5}))?)?)?");

        @Override
        public void insertUpdate(final DocumentEvent e) {
            changed(e);
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            changed(e);
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            changed(e);
        }

        private void changed(final DocumentEvent e){
            final Object owner = e.getDocument().getProperty("owner");
            if(owner instanceof SquigglyTextField){
                final SquigglyTextField input = (SquigglyTextField)owner;
                final String text = input.getText();
                String feedback;
                final Matcher matcher = IP_PORT_RANGE.matcher(text);
                if(matcher.matches()){

                    String startFirstPort = matcher.group(2);
                    String startSecondPort = matcher.group(3);

                    String endFirstPort = matcher.group(5);
                    String endSecondPort = matcher.group(6);

                    //port is allowed on 1 ip only, either the first or second but not both
                    if(startFirstPort != null && endFirstPort != null){
                        input.setModelessFeedback("Port can only be applied to the start ip or end ip but not both.");
                        return;
                    }
                    feedback = validatePort(startFirstPort, startSecondPort);
                    if(feedback != null){
                        input.setModelessFeedback(feedback);
                        return;
                    }

                    feedback = validatePort(endFirstPort, endSecondPort);
                    if(feedback != null){
                        input.setModelessFeedback(feedback);
                        return;
                    }
                }
                else {
                    feedback = "Invalid IP address format.";
                }
                input.setModelessFeedback(feedback);
            }
        }
    }

    private static String validatePort(final String start, final String end){
        String feedback = null;
        if(start != null){
            try{
                final Integer sport = Integer.valueOf(start);
                if(sport < 1 || sport > 65535){
                    feedback = "Start port number must be between 1 and 65535.";
                }
                if(end != null){
                    final Integer eport = Integer.valueOf(end);
                    if(eport < 1 || eport > 65535){
                        feedback = "End port number must be between 1 and 65535.";
                    }
                    else if(sport >= eport){
                        feedback = "Start port must be less than end port.";
                    }
                }
            }catch(NumberFormatException nfe){
                feedback = "Invalid port number.";
            }
        }
        return feedback;
    }

    private static class InvertablePortValidator extends InputValidator.ComponentValidationRule {
        private static final Pattern INVERTABLE_PORT = Pattern.compile("(?:!\\s)?(\\d{1,5})(?:-(\\d{1,5}))?");

        private InvertablePortValidator(final Component component) {
            super(component);
        }

        @Override
        public String getValidationError() {
            Component c = getComponent();
            if(c instanceof JTextField){
                final String text = ((JTextField) c).getText();
                if(!c.isEnabled() || text == null || text.isEmpty()) return null; //optional field
                //allow single port or port range and allow inversion
                final Matcher matcher = INVERTABLE_PORT.matcher(text);
                if(matcher.matches()){
                    final String start = matcher.group(1);
                    final String end = matcher.group(2);
                    return validatePort(start, end);
                }
                return "Invalid port entry.";
            }
            return null;
        }
    }

    private static class InvertableIpAddressValidator extends InputValidator.ComponentValidationRule {

        private static final Pattern IP_ADDRESS = Pattern.compile("(?:!\\s+)?(.+?)(?:/(.+))?");

        private InvertableIpAddressValidator(final Component component) {
            super(component);
        }

        @Override
        public String getValidationError() {
            final Component c = getComponent();
            if(c instanceof JTextField){
                String msg = null;
                final String text = ((JTextField) c).getText();
                if(!c.isEnabled() || text == null || text.isEmpty()) return null; //optional field
                //allow single port or port range and allow inversion
                final Matcher matcher = IP_ADDRESS.matcher(text);
                if(matcher.matches()){
                    final String ipAddress = matcher.group(1);
                    final String netmask = matcher.group(2);
                    if(InetAddressUtil.isValidIpv4Address(ipAddress)){
                        if(netmask != null){
                            if(Pattern.matches("\\d{1,3}", netmask)){
                                Integer cidr = Integer.valueOf(netmask);
                                if(cidr.intValue() < 1 || cidr.intValue() > 32){
                                    msg = "CIDR bit must be between 1 and 32.";
                                }
                            }
                            else {
                                if(!InetAddressUtil.isValidIpv4Address(netmask)) msg = "Invalid netmask address.";
                                else msg = null;
                            }
                        }
                    }
                    else if(InetAddressUtil.isValidIpv6Address(ipAddress)){
                        if(netmask != null){
                            if(Pattern.matches("\\d{1,3}", netmask)){
                                Integer cidr = Integer.valueOf(netmask);
                                if(cidr.intValue() < 4 || cidr.intValue() > 128){
                                    msg = "IPv6 netmask must be between 4 and 128";
                                }
                            }
                        }
                    }
                    else msg = "Invalid IP address.";
                    return msg;
                }
            }
            return null;
        }
    }
}
