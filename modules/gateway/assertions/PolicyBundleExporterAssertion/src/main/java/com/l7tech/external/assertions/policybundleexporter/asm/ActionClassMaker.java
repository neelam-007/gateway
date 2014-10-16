package com.l7tech.external.assertions.policybundleexporter.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;

/**
 * Generate the Action class for the Policy Bundle Installer (using ASM byte code library).
 */
public class ActionClassMaker extends AbstractClassMaker {

    final String iconResourcePath;

    public ActionClassMaker(final String subPackageInstallerName, final String installerCamelName, final String camelName, final String iconResourcePath) {
        super(subPackageInstallerName, installerCamelName, camelName);
        this.iconResourcePath = iconResourcePath;
    }

    /*
      L7 replaced:
        - "com/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerAction" with actionClassName
        - "(Lcom/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerAction;)V" with "(L" + actionClassName + ";)V"
        - "Lcom/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerAction;" with "L" + actionClassName + ";"
        - "com/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerAction$1" with action$1ClassName
        - "com/l7tech/external/assertions/simplepolicybundleinstaller/console/SimplePolicyBundleInstallerDialog" with dialogClassName
        - Simple Policy Bundle with " + name + "
     */

    /*
      The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
      ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
      com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerAction
     */
    public byte[] generate() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, actionClassName, null, "com/l7tech/console/action/SecureAction", null);

        cw.visitInnerClass(action$1ClassName, null, null, 0);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, "com/l7tech/gateway/common/security/rbac/AttemptedCreate");
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETSTATIC, "com/l7tech/objectmodel/EntityType", "FOLDER", "Lcom/l7tech/objectmodel/EntityType;");
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/gateway/common/security/rbac/AttemptedCreate", "<init>", "(Lcom/l7tech/objectmodel/EntityType;)V", false);
            mv.visitMethodInsn(INVOKESPECIAL, "com/l7tech/console/action/SecureAction", "<init>", "(Lcom/l7tech/gateway/common/security/rbac/AttemptedOperation;)V", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/l7tech/console/util/TopComponents", "getInstance", "()Lcom/l7tech/console/util/TopComponents;", false);
            mv.visitLdcInsn("servicesAndPolicies.tree");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/console/util/TopComponents", "getComponent", "(Ljava/lang/String;)Ljava/awt/Component;", false);
            mv.visitTypeInsn(CHECKCAST, "com/l7tech/console/tree/ServicesAndPoliciesTree");
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(NEW, action$1ClassName);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, action$1ClassName, "<init>", "(L" + actionClassName + ";)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/console/tree/ServicesAndPoliciesTree", "addTreeSelectionListener", "(Ljavax/swing/event/TreeSelectionListener;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "getName", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, dialogClassName, "getSelectedFolderAndGoid", "()Lcom/l7tech/util/Pair;", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "com/l7tech/util/Pair", "left", "Ljava/lang/Object;");
            Label l0 = new Label();
            mv.visitJumpInsn(IFNULL, l0);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Install " + camelName + " in ");   // TODO add space between words, e.g. "Simple Policy Bundle"
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "com/l7tech/util/Pair", "left", "Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitFieldInsn(GETSTATIC, dialogClassName, "BASE_FOLDER_NAME", "Ljava/lang/String;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l0);
            mv.visitFrame(F_APPEND,1, new Object[] {"com/l7tech/util/Pair"}, 0, null);
            mv.visitLdcInsn("Install " + camelName);   // TODO add space between words, e.g. "Simple Policy Bundle"
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "performAction", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, dialogClassName);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESTATIC, "com/l7tech/console/util/TopComponents", "getInstance", "()Lcom/l7tech/console/util/TopComponents;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/l7tech/console/util/TopComponents", "getTopParent", "()Ljava/awt/Frame;", false);
            mv.visitMethodInsn(INVOKESPECIAL, dialogClassName, "<init>", "(Ljava/awt/Frame;)V", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, dialogClassName, "pack", "()V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "com/l7tech/gui/util/Utilities", "centerOnScreen", "(Ljava/awt/Window;)V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "com/l7tech/gui/util/DialogDisplayer", "display", "(Ljavax/swing/JDialog;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "iconResource", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitLdcInsn(iconResourcePath);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$000", "(L" + actionClassName + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, actionClassName, "setActionValues", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /*
       The following code was generated: java -cp ./lib/repository/asm/asm-5.0.3.jar;./lib/tools/asm-util-5.0.3.jar;
       ./idea-classes/production/SimplePolicyBundleInstallerAssertion org.objectweb.asm.util.ASMifier
       com.l7tech.external.assertions.simplepolicybundleinstaller.console.SimplePolicyBundleInstallerAction$1
     */
    public byte[] generate$1() throws IOException {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_7, ACC_SUPER, action$1ClassName, null, "java/lang/Object", new String[] { "javax/swing/event/TreeSelectionListener" });

        cw.visitOuterClass(actionClassName, "<init>", "()V");

        cw.visitInnerClass(action$1ClassName, null, null, 0);

        {
            fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", "L" + actionClassName + ";", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(0, "<init>", "(L" + actionClassName + ";)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, action$1ClassName, "this$0", "L" + actionClassName + ";");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "valueChanged", "(Ljavax/swing/event/TreeSelectionEvent;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, action$1ClassName, "this$0", "L" + actionClassName + ";");
            mv.visitMethodInsn(INVOKESTATIC, actionClassName, "access$000", "(L" + actionClassName + ";)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
