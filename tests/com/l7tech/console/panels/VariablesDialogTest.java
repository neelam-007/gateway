package com.l7tech.console.panels;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.policy.assertion.composite.AllAssertion;

import javax.swing.*;
import java.util.Arrays;

public class VariablesDialogTest {
    public static void main(String[] args) {
        final HttpBasic basic = new HttpBasic();

        AllAssertion ass = new AllAssertion(Arrays.asList(
                new RequestXpathAssertion(),
                new ResponseXpathAssertion(),
                new SamlBrowserArtifact(),
                new HttpRoutingAssertion(),
                basic));

        VariablesDialog me = new VariablesDialog(null, basic, true);
        me.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        me.pack();
        me.setVisible(true);

        System.out.println("Chose " + me.getSelectedVariable());
    }
}
