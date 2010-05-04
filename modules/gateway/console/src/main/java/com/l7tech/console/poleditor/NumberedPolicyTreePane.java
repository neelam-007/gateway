package com.l7tech.console.poleditor;

import com.l7tech.console.tree.AssertionLineNumbersTree;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * This is a pane to hold the assertion-line-numbers tree and the policy tree.
 *
 * @author ghuang
 */
public class NumberedPolicyTreePane extends JPanel {

    //- PUBLIC
    
    public NumberedPolicyTreePane( final PolicyTree policyTree ) {
        this.policyTree = policyTree;
        init();
    }

    public boolean isNumbersVisible() {
        return assertionLineNumbersTree.isVisible();
    }

    public void setNumbersVisible( final boolean numbersVisible ) {
        assertionLineNumbersTree.setVisible( numbersVisible );

        // Update the assertion line numbers tree if assertion ordinals are required to be shown.
        if ( numbersVisible ) {
            scrollPane.setRowHeaderView( assertionNumbersPanel );
            assertionLineNumbersTree.updateOrdinalsDisplaying();
        } else {
            scrollPane.setRowHeaderView( null );
        }
    }

    //- PRIVATE

    private final PolicyTree policyTree;
    private AssertionLineNumbersTree assertionLineNumbersTree;
    private JPanel assertionNumbersPanel;
    private JScrollPane scrollPane;

    private void init() {
        setLayout( new BorderLayout() );

        assertionLineNumbersTree = new AssertionLineNumbersTree(policyTree);

        assertionNumbersPanel = new JPanel();
        assertionNumbersPanel.setBackground( assertionLineNumbersTree.getBackground() );
        assertionNumbersPanel.setLayout( new BoxLayout(assertionNumbersPanel, BoxLayout.Y_AXIS) );
        assertionNumbersPanel.add( assertionLineNumbersTree );
        assertionNumbersPanel.add( Box.createVerticalStrut(10000) );
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder( 0, 0, 0, 1, Color.lightGray ),
                BorderFactory.createEmptyBorder( 10, 0, 0, 0 ) );
        assertionNumbersPanel.setBorder( border );

        scrollPane = new JScrollPane();
        scrollPane.setViewportView( policyTree );

        add( scrollPane, BorderLayout.CENTER );
    }
}
