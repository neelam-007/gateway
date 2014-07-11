package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;

/**
 * User: rseminoff
 * Date: 27/02/13
 */
public class JwtHeaderPanel extends JPanel {
    private TargetVariablePanel jsonHeaderVariable;
    private JPanel jwtHeaderPanel;
    private JRadioButton claimsToAppendRadio;
    private JRadioButton fullJWTHeaderRadio;
    private JRadioButton noClaimsRadio;
    private JLabel headerVariableLabel;

    private Assertion myAssertion;
    private JwtEncodePanel parent;  // Kludgy, but required for events to affect items in another panel.


    public JwtHeaderPanel() {
        super();
        jsonHeaderVariable.setVariable("");
        jsonHeaderVariable.setValueWillBeRead(true);
        jsonHeaderVariable.setAcceptEmpty(false);
        noClaimsRadio.setSelected(true);

        noClaimsRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVariableEntry(NO_SUPPLIED_HEADER_CLAIMS);
            }
        });

        claimsToAppendRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVariableEntry(SUPPLIED_PARTIAL_CLAIMS);
            }
        });

        fullJWTHeaderRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVariableEntry(SUPPLIED_FULL_JWT_HEADER);
            }
        });

    }

    protected void setPanelWithValues(String value, int jsonHeaderType) {
        switch (jsonHeaderType) {
            case (SUPPLIED_FULL_JWT_HEADER): {
                fullJWTHeaderRadio.setSelected(true);
                break;
            }
            case (SUPPLIED_PARTIAL_CLAIMS): {
                claimsToAppendRadio.setSelected(true);
                break;
            }
            default: {
                noClaimsRadio.setSelected(true);
            }
        }
        jsonHeaderVariable.setVariable(value == null ? "" : value);

        this.updateVariableEntry(jsonHeaderType);
    }

    protected JwtHeaderPanel.JwtHeaderValues getValueFromPanel() {
        return new JwtHeaderValues() {{
            if (fullJWTHeaderRadio.isSelected()) {
                setHeaderType(SUPPLIED_FULL_JWT_HEADER);
                setVariable(jsonHeaderVariable.getVariable());
            } else if (claimsToAppendRadio.isSelected()) {
                setHeaderType(SUPPLIED_PARTIAL_CLAIMS);
                setVariable(jsonHeaderVariable.getVariable());
            } else {
                setHeaderType(NO_SUPPLIED_HEADER_CLAIMS);
                setVariable("");
            }
        }};
    }

    public void setAssertion(Assertion assertion, Assertion previousAssertion) {
        this.myAssertion = assertion;
        if (this.myAssertion != null) {
            jsonHeaderVariable.setAssertion(this.myAssertion, previousAssertion);
        }
    }

    protected void setParent(JwtEncodePanel panel) {
        parent = panel;
    }

    private void updateVariableEntry(int headerType) {
        // Updates the entry fields based on the selected header.

        switch (headerType) {
            case (SUPPLIED_FULL_JWT_HEADER):
            case (SUPPLIED_PARTIAL_CLAIMS): {
                jsonHeaderVariable.setEnabled(true);
                headerVariableLabel.setEnabled(true);
                break;
            }
            default: {
                jsonHeaderVariable.setEnabled(false);
                headerVariableLabel.setEnabled(false);
            }
        }
        parent.updateSignatureAlgorithmPanel(headerType);
    }

    public class JwtHeaderValues {
        private String jsonHeader = null;
        private int    suppliedHeader = NO_SUPPLIED_HEADER_CLAIMS;

        public String getVariable() {
            return jsonHeader;
        }

        public void setVariable(String variable) {
            this.jsonHeader = variable;
        }

        public int getHeaderType() {
            return this.suppliedHeader;
        }

        public void setHeaderType(int headerType) {
            this.suppliedHeader = headerType;
        }
    }

}
