/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Tests {@link BetterComboBox}.
 *
 * @author rmak
 * @since SecureSpan 5.0
 */
public class BetterComboBoxTest extends JFrame {
    private JPanel _contentPane;
    private JTextField _jComboBoxMinimumWidthTextField;
    private JTextField _jComboBoxMinimumHeightTextField;
    private JTextField _jComboBoxMaximumWidthTextField;
    private JTextField _jComboBoxMaximumHeightTextField;
    private JTextField _jComboBoxPreferredWidthTextField;
    private JTextField _jComboBoxPreferredHeightTextField;
    private JTextField _jComboBoxWidthTextField;
    private JTextField _jComboBoxHeightTextField;
    private JButton _jComboBoxSetMinimumSizeButton;
    private JButton _jComboBoxSetMaximumSizeButton;
    private JButton _jComboBoxSetSizeButton;
    private JButton _jComboBoxSetPreferredSizeButton;
    private JTextField _betterComboBoxPreferredWidthTextField;
    private JTextField _betterComboBoxPreferredHeightTextField;
    private JTextField _betterComboBoxWidthTextField;
    private JTextField _betterComboBoxHeightTextField;
    private JTextField _betterComboBoxMinimumWidthTextField;
    private JTextField _betterComboBoxMinimumHeightTextField;
    private JTextField _betterComboBoxMaximumWidthTextField;
    private JTextField _betterComboBoxMaximumHeightTextField;
    private JButton _betterComboBoxSetMinimumSizeButton;
    private JButton _betterComboBoxSetMaximumSizeButton;
    private JButton _betterComboBoxSetPreferredSizeButton;
    private JButton _betterComboBoxSetSizeButton;
    private JButton _refreshButton;
    private JCheckBox _shortItemCheckBox;
    private JCheckBox _longItemCheckBox;
    private JCheckBox _20ItemsCheckBox;
    private JCheckBox _30ItemsCheckBox;
    private JButton _packButton;

    private static final String SHORT_ITEM = "short";
    private static final String LONG_ITEM = "looooooooooooooooooooooooooooooooooooooooooooooong";
    private static final String[] NUMBER_ITEMS = new String[50];
    {
        for (int i = 0; i < 50; ++i) {
            NUMBER_ITEMS[i] = Integer.toString(i);
        }
    }

    private final BetterComboBoxTest_JComboBoxFrame _jComboBoxFrame = new BetterComboBoxTest_JComboBoxFrame();
    private final BetterComboBoxTest_BetterComboBoxFrame _betterComboBoxFrame = new BetterComboBoxTest_BetterComboBoxFrame();

    public BetterComboBoxTest() {
        setTitle("BetterComboBoxTest");
        setContentPane(_contentPane);

        // Adds menu bar.
        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Adds Look & Feel menu.
        final JMenu lafMenu = new JMenu("Look & Feel");
        menuBar.add(lafMenu);
        final ActionListener lafActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    UIManager.setLookAndFeel(event.getActionCommand());
                    SwingUtilities.updateComponentTreeUI(BetterComboBoxTest.this);
                    SwingUtilities.updateComponentTreeUI(_jComboBoxFrame);
                    SwingUtilities.updateComponentTreeUI(_betterComboBoxFrame);
                    pack();
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException e) {
                }
            }
        };
        final ButtonGroup lafButtonGroup = new ButtonGroup();
        for (UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(lafInfo.getName());
            menuItem.setActionCommand(lafInfo.getClassName());
            menuItem.addActionListener(lafActionListener);
            lafMenu.add(menuItem);
            lafButtonGroup.add(menuItem);
            if (lafInfo.getClassName().equals(UIManager.getSystemLookAndFeelClassName())) {
                // Because menuItem.setSelected(true) does not trigger action events, we have to do it this way:
                menuItem.setSelected(false);
                menuItem.doClick();
            }
        }

        _jComboBoxSetMinimumSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _jComboBoxFrame.getComboBox().setMinimumSize(makeDimension(_jComboBoxMinimumWidthTextField, _jComboBoxMinimumHeightTextField));
            }
        });

        _jComboBoxSetMaximumSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _jComboBoxFrame.getComboBox().setMaximumSize(makeDimension(_jComboBoxMaximumWidthTextField, _jComboBoxMaximumHeightTextField));
            }
        });

        _jComboBoxSetPreferredSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _jComboBoxFrame.getComboBox().setPreferredSize(makeDimension(_jComboBoxPreferredWidthTextField, _jComboBoxPreferredHeightTextField));
            }
        });

        _jComboBoxSetSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _jComboBoxFrame.getComboBox().setSize(makeDimension(_jComboBoxWidthTextField, _jComboBoxHeightTextField));
            }
        });

        _betterComboBoxSetMinimumSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _betterComboBoxFrame.getComboBox().setMinimumSize(makeDimension(_betterComboBoxMinimumWidthTextField, _betterComboBoxMinimumHeightTextField));
            }
        });

        _betterComboBoxSetMaximumSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _betterComboBoxFrame.getComboBox().setMaximumSize(makeDimension(_betterComboBoxMaximumWidthTextField, _betterComboBoxMaximumHeightTextField));
            }
        });

        _betterComboBoxSetPreferredSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _betterComboBoxFrame.getComboBox().setPreferredSize(makeDimension(_betterComboBoxPreferredWidthTextField, _betterComboBoxPreferredHeightTextField));
            }
        });

        _betterComboBoxSetSizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _betterComboBoxFrame.getComboBox().setSize(makeDimension(_betterComboBoxWidthTextField, _betterComboBoxHeightTextField));
            }
        });

        _refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateJComboBoxFields();
                updateBetterComboBoxFields();
            }
        });

        _shortItemCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (_shortItemCheckBox.isSelected()) {
                    _jComboBoxFrame.getComboBox().insertItemAt(SHORT_ITEM, 0);
                    _betterComboBoxFrame.getComboBox().insertItemAt(SHORT_ITEM, 0);
                } else {
                    _jComboBoxFrame.getComboBox().removeItem(SHORT_ITEM);
                    _betterComboBoxFrame.getComboBox().removeItem(SHORT_ITEM);
                }
            }
        });

        _longItemCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (_longItemCheckBox.isSelected()) {
                    _jComboBoxFrame.getComboBox().addItem(LONG_ITEM);
                    _betterComboBoxFrame.getComboBox().addItem(LONG_ITEM);
                } else {
                    _jComboBoxFrame.getComboBox().removeItem(LONG_ITEM);
                    _betterComboBoxFrame.getComboBox().removeItem(LONG_ITEM);
                }
            }
        });

        _20ItemsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (_20ItemsCheckBox.isSelected()) {
                    for (int i = 0; i < 20; ++i) {
                        _jComboBoxFrame.getComboBox().addItem(NUMBER_ITEMS[i]);
                        _betterComboBoxFrame.getComboBox().addItem(NUMBER_ITEMS[i]);
                    }
                } else {
                    for (int i = 0; i < 20; ++i) {
                        _jComboBoxFrame.getComboBox().removeItem(NUMBER_ITEMS[i]);
                        _betterComboBoxFrame.getComboBox().removeItem(NUMBER_ITEMS[i]);
                    }
                }
            }
        });

        _30ItemsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (_30ItemsCheckBox.isSelected()) {
                    for (int i = 20; i < 50; ++i) {
                        _jComboBoxFrame.getComboBox().addItem(NUMBER_ITEMS[i]);
                        _betterComboBoxFrame.getComboBox().addItem(NUMBER_ITEMS[i]);
                    }
                } else {
                    for (int i = 20; i < 50; ++i) {
                        _jComboBoxFrame.getComboBox().removeItem(NUMBER_ITEMS[i]);
                        _betterComboBoxFrame.getComboBox().removeItem(NUMBER_ITEMS[i]);
                    }
                }
            }
        });

        _packButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BetterComboBoxTest.this.pack();
                _jComboBoxFrame.pack();
                _betterComboBoxFrame.pack();
                positionComboBoxFrames();
            }
        });

        _jComboBoxFrame.getComboBox().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateJComboBoxFields();
                }
            });

        _betterComboBoxFrame.getComboBox().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateBetterComboBoxFields();
                }
            });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
                _jComboBoxFrame.dispose();
                _betterComboBoxFrame.dispose();
            }
        });

        //
        // Initializes controls.
        //

        // Starts with short item unchecked.
        _shortItemCheckBox.doClick();
        if (_shortItemCheckBox.isSelected()) _shortItemCheckBox.doClick();

        // Starts with long item unchecked.
        _longItemCheckBox.doClick();
        if (_longItemCheckBox.isSelected()) _longItemCheckBox.doClick();

        updateJComboBoxFields();
        updateBetterComboBoxFields();

        // Positions the frames vertically and centers on screen.
        pack();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension overallSize = getSize();
        if (_jComboBoxFrame.getWidth() > overallSize.width) overallSize.width = _jComboBoxFrame.getWidth();
        if (_betterComboBoxFrame.getWidth() > overallSize.width) overallSize.width = _betterComboBoxFrame.getWidth();
        overallSize.height += _jComboBoxFrame.getHeight();
        overallSize.height += _betterComboBoxFrame.getHeight();
        final int left = screenSize.width > overallSize.width ? (screenSize.width - overallSize.width) / 2 : 0;
        int top = screenSize.height > overallSize.height ? (screenSize.height - overallSize.height) / 2 : 0;
        setLocation(left, top);
        positionComboBoxFrames();

        setVisible(true);
        _jComboBoxFrame.setVisible(true);
        _betterComboBoxFrame.setVisible(true);
        toFront();
    }

    private void updateJComboBoxFields() {
        updateSizeFields(_jComboBoxMinimumWidthTextField, _jComboBoxMinimumHeightTextField, _jComboBoxFrame.getComboBox().getMinimumSize());
        updateSizeFields(_jComboBoxMaximumWidthTextField, _jComboBoxMaximumHeightTextField, _jComboBoxFrame.getComboBox().getMaximumSize());
        updateSizeFields(_jComboBoxPreferredWidthTextField, _jComboBoxPreferredHeightTextField, _jComboBoxFrame.getComboBox().getPreferredSize());
        updateSizeFields(_jComboBoxWidthTextField, _jComboBoxHeightTextField, _jComboBoxFrame.getComboBox().getSize());
    }

    private void updateBetterComboBoxFields() {
        updateSizeFields(_betterComboBoxMinimumWidthTextField, _betterComboBoxMinimumHeightTextField, _betterComboBoxFrame.getComboBox().getMinimumSize());
        updateSizeFields(_betterComboBoxMaximumWidthTextField, _betterComboBoxMaximumHeightTextField, _betterComboBoxFrame.getComboBox().getMaximumSize());
        updateSizeFields(_betterComboBoxPreferredWidthTextField, _betterComboBoxPreferredHeightTextField, _betterComboBoxFrame.getComboBox().getPreferredSize());
        updateSizeFields(_betterComboBoxWidthTextField, _betterComboBoxHeightTextField, _betterComboBoxFrame.getComboBox().getSize());
    }

    private static void updateSizeFields(final JTextField widthTextField,
                                         final JTextField heightTextField,
                                         final Dimension dimension) {
        widthTextField.setText(Integer.toString(dimension.width));
        heightTextField.setText(Integer.toString(dimension.height));
    }

    private Dimension makeDimension(final JTextField widthTextField, final JTextField heightTextField) {
        Dimension dimension = null;
        if (widthTextField.getText().length() > 0 && heightTextField.getText().length() > 0) {
            dimension = new Dimension(Integer.parseInt(widthTextField.getText()), Integer.parseInt(heightTextField.getText()));
        }
        return dimension;
    }

    private void positionComboBoxFrames() {
        final int left = getLocation().x;
        int top = getLocation().y;
        top += getSize().height;
        _jComboBoxFrame.setLocation(left, top);
        top += _jComboBoxFrame.getHeight();
        _betterComboBoxFrame.setLocation(left, top);

    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final BetterComboBoxTest controlFrame = new BetterComboBoxTest();
            }
        });
    }
}
