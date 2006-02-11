package com.l7tech.console.panels;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.policy.assertion.composite.AllAssertion;

import javax.swing.*;
import java.util.Arrays;

public class VariablesDialogTest {
    public static void main(String[] args) {
        final ComparisonAssertion comp = new ComparisonAssertion();

        AllAssertion ass = new AllAssertion(Arrays.asList(new Assertion[] {
            new RequestXpathAssertion(),
            new ResponseXpathAssertion(),
            new SamlBrowserArtifact(),
            new HttpRoutingAssertion(),
            comp,
        }
        ));

        VariablesDialog me = new VariablesDialog(null, comp, true);
        me.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        me.pack();
        me.setVisible(true);

        System.out.println("Chose " + me.getSelectedVariable());
    }
}
