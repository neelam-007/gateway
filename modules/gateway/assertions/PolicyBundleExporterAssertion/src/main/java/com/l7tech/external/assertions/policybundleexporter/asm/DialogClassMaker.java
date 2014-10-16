package com.l7tech.external.assertions.policybundleexporter.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;

/**
 * Generate the Dialog class for the Policy Bundle Installer (using ASM byte code library).
 */
public class DialogClassMaker extends AbstractClassMaker {

    public DialogClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName, final String folderName) {
        super(subPackageInstallerName, installerCamelName, camelName, folderName);
    }

    /*
      L7 replaced:
       - "com/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog" with dialogClassName
       - "(Lcom/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog;)V" with "(L" + dialogClassName + ";)V"
       - "(Lcom/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog;) with "(L" + dialogClassName + ";)" + "
       - "Lcom/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog;" with "L" + dialogClassName + ";"
       - "com/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog$1" with dialog$1ClassName
       - "Lcom/l7tech/external/assertions/simplepolicybundleinstaller/SimplePolicyBundleInstallerAssertion;" with "L" + assertionClassName + ";"
       - Simple Policy Bundle with " + name + "
    */

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerDialog
    */
    public byte[] generate() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, dialogClassName, null, "com/l7tech/console/panels/bundles/BundleInstallerDialog", null);

        cw.visitInnerClass(dialog$1ClassName, null, null, 0);

        {
            fv = cw.visitField(ACC_PROTECTED + ACC_STATIC, "BASE_FOLDER_NAME", "Ljava/lang/String;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/awt/Frame;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, dialogClassName, "BASE_FOLDER_NAME", "Ljava/lang/String;");
            mv.visitLdcInsn(Type.getType("L" + assertionClassName + ";"));
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/console/panels/bundles/BundleInstallerDialog", "<init>", "(Ljava/awt/Frame;Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitTypeInsn(NEW, "javax/swing/JButton");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("Button in Customizable Button Panel");
            mv.visitMethodInsn(INVOKESPECIAL, "javax/swing/JButton", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(NEW, "java/awt/BorderLayout");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/awt/BorderLayout", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/JButton", "setLayout", "(Ljava/awt/LayoutManager;)V", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(NEW, dialog$1ClassName);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, dialog$1ClassName, "<init>", "(L" + dialogClassName + ";)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/JButton", "addActionListener", "(Ljava/awt/event/ActionListener;)V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, dialogClassName, "customizableButtonPanel", "Ljavax/swing/JPanel;");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/JPanel", "add", "(Ljava/awt/Component;)Ljava/awt/Component;", false);
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "getSizingPanelPreferredSize", "()Ljava/awt/Dimension;", null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$000", "(L" + dialogClassName + ";)" + "Ljavax/swing/JPanel;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, dialogClassName, "customizableButtonPanel", "Ljavax/swing/JPanel;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitLdcInsn(folderName);// TODO add space between words, e.g. "Simple Policy Bundle"
            mv.visitFieldInsn(PUTSTATIC, dialogClassName, "BASE_FOLDER_NAME", "Ljava/lang/String;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerDialog
    */
    public byte[] generate$1() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_7, ACC_SUPER, dialog$1ClassName, null, "java/lang/Object", new String[] { "java/awt/event/ActionListener" });

        cw.visitOuterClass(dialogClassName, "<init>", "(Ljava/awt/Frame;)V");

        cw.visitInnerClass(dialog$1ClassName, null, null, 0);

        {
            fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", "L" + dialogClassName + ";", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(0, "<init>", "(L" + dialogClassName + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, dialog$1ClassName, "this$0", "L" + dialogClassName + ";");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "actionPerformed", "(Ljava/awt/event/ActionEvent;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, dialog$1ClassName, "this$0", "L" + dialogClassName + ";");
            mv.visitMethodInsn(INVOKESTATIC, dialogClassName, "access$000", "(L" + dialogClassName + ";)" + "Ljavax/swing/JPanel;", false);
            mv.visitLdcInsn("Button action performed.");
            mv.visitMethodInsn(INVOKESTATIC, "javax/swing/JOptionPane", "showMessageDialog", "(Ljava/awt/Component;Ljava/lang/Object;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
