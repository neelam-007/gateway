package com.l7tech.test.util;

import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A container that makes it easy to interactively launch tiny GUI tests.
 */
public class InteractiveGuiTester implements GuiTestLauncher {
    private static final Logger logger = Logger.getLogger(InteractiveGuiTester.class.getName());

    public void startTest(final Object testHolder) throws GuiTestException {
        showTestWindow(testHolder, findTestMethods(testHolder));
    }

    private static Collection<Method> findTestMethods(Object testHolder) throws GuiTestException {
        final Collection<Method> testMethods = Functions.grep(Arrays.asList(testHolder.getClass().getMethods()),
                new Functions.Unary<Boolean, Method>() {
                    public Boolean call(Method method) {
                        return method.getAnnotation(GuiTestMethod.class) != null;
                    }
                });

        if (testMethods.isEmpty())
            throw new GuiTestException("No methods found in class " + testHolder.getClass() + " that have annotation " + GuiTestMethod.class);
        return testMethods;
    }

    private static void showTestWindow(final Object testHolder, final Collection<Method> testMethods) throws GuiTestException {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final JFrame main = new JFrame("GuiCredentialManagerTest");
                    main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    Container cp = main.getContentPane();
                    main.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

                    Collection<JButton> buttons = Functions.map(testMethods, new Functions.Unary<JButton, Method>() {
                        public JButton call(Method method) {
                            return methodButton(testHolder, method, main);
                        }
                    });

                    for (JButton button : buttons)
                        cp.add(button);

                    Utilities.equalizeButtonSizes(buttons.toArray(new JButton[buttons.size()]));

                    main.pack();
                    Utilities.centerOnScreen(main);
                    main.setVisible(true);
                }
            });
        } catch (InterruptedException e) {
            throw new GuiTestException(e);
        } catch (InvocationTargetException e) {
            throw new GuiTestException(e);
        }
    }

    private static JButton methodButton(final Object targetObject, final Method method, final Frame parent) {
        JButton button = new JButton(method.getName());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (method.getParameterTypes().length > 0)
                        method.invoke(targetObject, parent);
                    else
                        method.invoke(targetObject);
                } catch (IllegalAccessException e1) {
                    logger.log(Level.WARNING, e1.getMessage(), e1);
                    reportError(e1);
                } catch (InvocationTargetException e1) {
                    logger.log(Level.WARNING, e1.getMessage(), e1);
                    reportError(e1);
                }
            }
        });
        return button;
    }

    private static void reportError(Throwable e1) {
        JOptionPane.showMessageDialog(null, "Error: " + ExceptionUtils.getMessage(e1), "Unable to run test", JOptionPane.ERROR_MESSAGE);
    }
}
