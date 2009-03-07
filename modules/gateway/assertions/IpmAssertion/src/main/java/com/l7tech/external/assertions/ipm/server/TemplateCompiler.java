package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import com.l7tech.util.Codegen;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically generates a Java class that will do expansion of the specified IPM template.
 * A new TemplateCompiler instance must be created for each template compilation attempt.
 */
public class TemplateCompiler {
    private static final AtomicLong counter = new AtomicLong(100000);
    private static final String PACKAGE_NAME = CompiledTemplate.class.getPackage().getName();
    private static final String COMPILED_TEMPLATE_CLASSNAME = CompiledTemplate.class.getName();
    private static final Pattern PICTURE_PATTERN = Pattern.compile("(.+?)\\((\\d+)\\)");

    private final String templateStr;
    private final String templateSimpleName;
    private final String templateClassName;
    private final List<WorkUnit> workUnits = new ArrayList<WorkUnit>();
    private final List<LiteralLineOfCodeWorkUnit> fieldDeclarations = new ArrayList<LiteralLineOfCodeWorkUnit>();
    private final List<LiteralLineOfCodeWorkUnit> staticInit = new ArrayList<LiteralLineOfCodeWorkUnit>();
    private final Map<String, String> byteIdentifiers = new HashMap<String, String>();
    private final Map<String, String> charIdentifiers = new HashMap<String, String>();
    private String javaSource;
    int nextIdentifierId = 1;
    boolean attemptedCompile = false;


    /**
     * Prepare to compile the specified IPM template.
     *
     * @param template the template to compile.  Required.
     */
    public TemplateCompiler(String template) {
        this.templateStr = template;
        this.templateSimpleName = "DynamicCompiledTemplate" + counter.incrementAndGet();
        this.templateClassName = PACKAGE_NAME + '.' + templateSimpleName;
    }

    /**
     * Attempt to compile the current IPM template.
     *
     * @return a new CompiledTemplate instance that can be used to expand IPM messages into XML.  Never null.
     * @throws TemplateCompilerException if a CompiledTemplate instance could not be produced.
     */
    public CompiledTemplate compile() throws TemplateCompilerException {
        if (attemptedCompile)
            throw new IllegalStateException("compile() has already been called on this instance");
        attemptedCompile = true;
        Document doc = parseXml(templateStr);
        javaSource = generateSource(templateSimpleName, doc);
        return compileSource(templateClassName, javaSource);
    }

    String getJavaSource() {
        return javaSource;
    }

    private static CompiledTemplate compileSource(String templateClassName, String javaSource) throws TemplateCompilerException {
        Codegen codegen = new Codegen(templateClassName, javaSource);
        try {
            codegen.addLoadableClass(CompiledTemplate.class);
            codegen.addLoadableClass(CompiledTemplate.InputBufferEmptyException.class);
            codegen.addLoadableClass(CompiledTemplate.OutputBufferFullException.class);
            Class templateClass = codegen.compile(CompiledTemplate.class.getClassLoader());
            Object obj = templateClass.newInstance();
            if (!(obj instanceof CompiledTemplate))
                throw new TemplateCompilerException("Compilation resulted in object of unexpected type: " + obj.getClass());
            return (CompiledTemplate)obj;
        } catch (Codegen.CompileException e) {
            throw new TemplateCompilerException(makeCompileDetailMessage(codegen.getDiagnostics()), e);
        } catch (ClassNotFoundException e) {
            throw new TemplateCompilerException("Compilation failed to produce the expected class: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new TemplateCompilerException("Compilation failed to produce a usable class: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new TemplateCompilerException("Compilation failed to produce a usable class: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new TemplateCompilerException("Compilation failed because a needed resource could not be read: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static String makeCompileDetailMessage(List<? extends Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder("Compilation failed: ");

        for (Diagnostic diagnostic : diagnostics) {
            sb.append('[');
            sb.append(" code=").append(diagnostic.getCode());
            sb.append(" kind=").append(diagnostic.getKind());
            sb.append(" position=").append(diagnostic.getPosition());
            sb.append(" startPosition=").append(diagnostic.getStartPosition());
            sb.append(" endPosition=").append(diagnostic.getEndPosition());
            sb.append(" source=").append(diagnostic.getSource());
            sb.append(" message=\"").append(diagnostic.getMessage(null)).append('\"');
            sb.append("]\n");
        }

        return sb.toString();
    }

    private String generateSource(String templateSimpleName, Document doc) throws TemplateCompilerException {

        analyzeTemplate(doc);

        StringWriter writer = new StringWriter();
        PrintWriter out = null;
        try {
            out = new PrintWriter(writer);

            genHeader(out, PACKAGE_NAME, COMPILED_TEMPLATE_CLASSNAME, templateSimpleName);
            genCharsWorkUnits(out);
            genMiddle(out, COMPILED_TEMPLATE_CLASSNAME);
            genBytesWorkUnits(out);
            genFooter(out);

            return writer.toString();
        } finally {
            if (out != null) out.close();
        }
    }

    private void analyzeTemplate(Document doc) throws TemplateCompilerException {
        ElementCursor cursor = new DomElementCursor(doc);
        cursor.moveToDocumentElement();
        collectElement(cursor, 2, 0);
    }

    private void collectElement(ElementCursor cursor, int indent, final int depth) throws TemplateCompilerException {
        String type = cursor.getAttributeValue("pic");
        Integer occurs = getIntAttr(cursor, "occurs");
        if (occurs == null) occurs = 1;

        if (occurs > 1) {
            addWorkUnit(new BeginForLoopWorkUnit(indent, "i" + depth, occurs));
            indent++;
        }

        String prefix = cursor.getPrefix();
        String localName = cursor.getLocalName();
        String elname = prefix == null ? localName : prefix + ':' + localName;
        addEmitOpenTag(indent, cursor.asDomElement());
        if (type != null && type.length() > 0)
            addEmitCopiedChars(indent, type);
        try {
            final int subindent = indent;
            cursor.visitChildElements(new ElementCursor.Visitor() {
                public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                    try {
                        collectElement(ec, subindent, depth + 1);
                    } catch (TemplateCompilerException e) {
                        throw new InvalidDocumentFormatException(e);
                    }
                }
            });
        } catch (InvalidDocumentFormatException e) {
            TemplateCompilerException tce = ExceptionUtils.getCauseIfCausedBy(e, TemplateCompilerException.class);
            if (tce != null) throw tce;
            throw new TemplateCompilerException("Invalid template format: " + ExceptionUtils.getMessage(e), e);
        }
        addEmitCloseTag(indent, elname);

        if (occurs > 1) {
            indent--;
            addWorkUnit(new EndBlockWorkUnit(indent));
        }
    }

    // Add a work unit that emits an open tag
    private void addEmitOpenTag(int indent, Element element) {
        element = (Element)element.cloneNode(false);
        element.removeAttribute("occurs");
        element.removeAttribute("pic");
        try {
            String elStr = XmlUtil.nodeToString(element);
            elStr = elStr.replaceAll("\\<\\?.*\\?\\>", "");
            elStr = elStr.replaceAll("\\<\\/.*", "");
            if (elStr.startsWith("<?")) throw new RuntimeException("xml decl");
            addEmitConstant(indent, elStr);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    // Add a work unit that emits a close tag
    private void addEmitCloseTag(int indent, String elname) {
        addEmitConstant(indent, "</" + elname + ">\n");
    }

    // Add a work unit that emits a constant string
    private void addEmitConstant(int indent, String constant) {
        addWorkUnit(new EmitConstantWorkUnit(indent, constant));
    }

    private void addEmitCopiedChars(int indent, String picture) throws TemplateCompilerException {
        final Matcher matcher = PICTURE_PATTERN.matcher(picture);
        if (!matcher.matches())
            throw new TemplateCompilerException("Unrecognized 'type' attribute: " + picture);
        String countStr = matcher.group(2);
        int count;
        try {
            count = Integer.parseInt(countStr);
            assert count >= 0;
        } catch (NumberFormatException nfe) {
            throw new TemplateCompilerException("Unrecognized 'type' attribute: " + picture, nfe);
        }
        addWorkUnit(new CopyCharsWorkUnit(indent, count));
    }

    private void addWorkUnit(WorkUnit workUnit) {
        workUnits.add(workUnit);
    }

    private static Integer getIntAttr(ElementCursor cursor, String attrName) throws TemplateCompilerException {
        try {
            final String val = cursor.getAttributeValue(attrName);
            return val == null || val.length() < 1 ? null : Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            throw new TemplateCompilerException("Attribute " + attrName + " of element " + cursor.getLocalName() + " is not an integer: " + ExceptionUtils.getMessage(nfe), nfe);
        }
    }

    private static void genHeader(PrintWriter out, String packageName, String compiledTemplateClassName, String templateSimpleName) {
        out.printf("package %s;\n\n", packageName);
        out.printf("public class %s extends %s {\n", templateSimpleName, compiledTemplateClassName);
        out.printf("    protected void doExpand() throws InputBufferEmptyException, OutputBufferFullException {\n");
    }

    private void genCharsWorkUnits(PrintWriter out) {
        Deque<WorkUnit> units = new LinkedList<WorkUnit>(workUnits);
        while (!units.isEmpty()) {
            WorkUnit unit = units.removeFirst();
            unit.generate(false, out, units);
        }
    }

    private static void genMiddle(PrintWriter out, String compiledTemplateClassName) {
        out.printf("    }\n\n");
        out.printf("    protected void doExpandBytes() throws %s.InputBufferEmptyException, %s.OutputBufferFullException, java.io.IOException {\n", compiledTemplateClassName, compiledTemplateClassName);
    }

    private void genBytesWorkUnits(PrintWriter out) {
        Deque<WorkUnit> units = new LinkedList<WorkUnit>(workUnits);
        while (!units.isEmpty()) {
            WorkUnit unit = units.removeFirst();
            unit.generate(true, out, units);
        }
    }

    private void genFooter(PrintWriter out) {
        out.printf("    }\n\n");
        for (LiteralLineOfCodeWorkUnit unit : fieldDeclarations)
            unit.generate(false, out);
        openStatic(out);
        int statements = 0;
        for (LiteralLineOfCodeWorkUnit unit : staticInit) {
            unit.generate(false, out);
            if (statements++ > 50) {
                // Avoid making any single static initializer block too big
                statements = 0;
                closeStatic(out);
                openStatic(out);
            }
        }
        closeStatic(out);
        out.printf("}\n");
    }


    private static void openStatic(PrintWriter out) {
        out.println();
        out.printf("    static {\n");
        out.printf("        try {\n");
    }

    private static void closeStatic(PrintWriter out) {
        out.printf("        } catch (Exception e) {\n");
        out.printf("            throw new ExceptionInInitializerError(e);\n");
        out.printf("        }\n");
        out.printf("    }\n");
    }

    private static Document parseXml(String template) throws TemplateCompilerException {
        try {
            return XmlUtil.stringToDocument(template);
        } catch (SAXException e) {
            throw new TemplateCompilerException("Template is not well formed XML: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private abstract static class WorkUnit {
        protected final int indent;

        protected WorkUnit(int indent) {
            this.indent = indent;
        }

        protected void printIndent(PrintWriter out) {
            for (int i = 0; i < indent; ++i)
                out.print("    ");
        }

        public abstract void generate(boolean bytesMode, PrintWriter out);

        public void generate(boolean bytesMode, PrintWriter out, Deque<WorkUnit> next) {
            generate(bytesMode, out);
        }
    }

    private static class CopyCharsWorkUnit extends WorkUnit {
        private final int numToCopy;

        private CopyCharsWorkUnit(int indent, int numToCopy) {
            super(indent);
            this.numToCopy = numToCopy;
        }

        public void generate(boolean bytesMode, PrintWriter out) {
            printIndent(out);
            out.print(bytesMode ? "cpyb" : "copy");
            out.print('(');
            out.print(numToCopy);
            out.print(");\n");
        }
    }

    /** @noinspection NonStaticInnerClassInSecureContext*/
    private class EmitConstantWorkUnit extends WorkUnit {
        private final String constant;

        private EmitConstantWorkUnit(int indent, String constant) {
            super(indent);
            this.constant = constant;
        }

        public void generate(boolean bytesMode, PrintWriter out) {
            doGenerate(bytesMode, this.constant, out);
        }

        public void generate(boolean bytesMode, PrintWriter out, Deque<WorkUnit> next) {
            // As an optimization, we will combine outselves with any immediately-following EmitConstant work units
            StringBuilder folded = new StringBuilder(constant);
            while (next.peekFirst() instanceof EmitConstantWorkUnit) {
                EmitConstantWorkUnit other = (EmitConstantWorkUnit)next.removeFirst();
                folded.append(other.constant);
            }
            doGenerate(bytesMode, folded.toString(), out);
        }

        private void doGenerate(boolean bytesMode, String constant, PrintWriter out) {
            printIndent(out);

            String identifier = bytesMode
                                ? getByteArrayIdentifier(constant)
                                : getCharArrayIdentifier(constant);


            if (bytesMode)
                out.print("writb(" + identifier + ");\n");
            else
                out.print("write(" + identifier + ");\n");
        }
    }

    private String getByteArrayIdentifier(String constant) {
        return getArrayIdentifier(constant, "byte[]", "B", byteIdentifiers, "getBytes(\"UTF-8\")");
    }

    private String getCharArrayIdentifier(String constant) {
        return getArrayIdentifier(constant, "char[]", "C", charIdentifiers, "toCharArray()");
    }

    private String getArrayIdentifier(String constant, String type, String identifierPrefix, Map<String, String> cache, String conversionCode) {
        String identifier = cache.get(constant);
        if (identifier != null) return identifier;

        identifier = identifierPrefix + nextIdentifierId++;
        String escapedConstant = constant.replaceAll("\\\"", "\\\\\"").replaceAll("\n", "\\\\\\n"); // TODO quote other special chars etc
        String declaration = "private static final " + type + ' ' + identifier + ';';
        String initializer = identifier + " = \"" + escapedConstant + "\"." + conversionCode + ';';
        fieldDeclarations.add(new LiteralLineOfCodeWorkUnit(1, declaration));
        staticInit.add(new LiteralLineOfCodeWorkUnit(3, initializer));
        cache.put(constant, identifier);
        return identifier;
    }

    private static class LiteralLineOfCodeWorkUnit extends WorkUnit {
        private final String code;

        private LiteralLineOfCodeWorkUnit(int indent, String code) {
            super(indent);
            this.code = code;
        }

        public void generate(boolean bytesMode, PrintWriter out) {
            printIndent(out);
            out.println(code);
        }
    }

    private static class BeginForLoopWorkUnit extends WorkUnit {
        private final String indexVar;
        private final int limit;

        private BeginForLoopWorkUnit(int indent, String indexVar, int limit) {
            super(indent);
            this.indexVar = indexVar;
            this.limit = limit;
        }

        public void generate(boolean bytesMode, PrintWriter out) {
            printIndent(out);
            out.print("for (int ");
            out.print(indexVar);
            out.print(" = 0; ");
            out.print(indexVar);
            out.print(" < ");
            out.print(limit);
            out.print("; ");
            out.print(indexVar);
            out.print("++) {");
            out.println();
        }
    }

    private static class EndBlockWorkUnit extends LiteralLineOfCodeWorkUnit {
        private EndBlockWorkUnit(int indent) {
            super(indent, "}");
        }
    }
}
