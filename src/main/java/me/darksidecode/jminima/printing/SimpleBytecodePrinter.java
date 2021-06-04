/*
 * Copyright 2021 German Vekhorev (DarksideCode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.jminima.printing;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings ("DuplicatedCode")
@RequiredArgsConstructor
public class SimpleBytecodePrinter implements BytecodePrinter, Opcodes {

    private static final String INDENT = "    "; // 4 spaces
    private static final String MARGIN_X2 = "\n\n\n"; // 2 empty lines

    private static final int INSN_OFFSET_PADDING = 15;
    private static final int OPCODE_PADDING      = 10;
    private static final int OPCODE_NAME_PADDING = 55;

    private static final Map<Integer, Function<Class<?>, String>> accFlags = new LinkedHashMap<>();
    private static final Map<Integer, String> opcodes = new HashMap<>();

    static {
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //   Access modifiers.
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        accFlags.put(ACC_PUBLIC, type -> "PUBLIC");
        accFlags.put(ACC_PRIVATE, type -> "PRIVATE");
        accFlags.put(ACC_PROTECTED, type -> "PROTECTED");
        accFlags.put(ACC_STATIC, type -> "STATIC");
        accFlags.put(ACC_FINAL, type -> "FINAL");
        accFlags.put(ACC_SYNCHRONIZED | ACC_SUPER, type
                -> type == MethodNode.class ? "SYNCHRONIZED" : "SUPER");
        accFlags.put(ACC_BRIDGE | ACC_VOLATILE, type
                -> type == MethodNode.class ? "BRIDGE" : "VOLATILE");
        accFlags.put(ACC_VARARGS | ACC_TRANSIENT, type
                -> type == MethodNode.class ? "VARARGS" : "TRANSIENT");
        accFlags.put(ACC_NATIVE, type -> "NATIVE");
        accFlags.put(ACC_INTERFACE, type -> "INTERFACE");
        accFlags.put(ACC_ABSTRACT, type -> "ABSTRACT");
        accFlags.put(ACC_STRICT, type -> "STRICT");
        accFlags.put(ACC_SYNTHETIC, type -> "SYNTHETIC");
        accFlags.put(ACC_ANNOTATION, type -> "ANNOTATION");
        accFlags.put(ACC_ENUM, type -> "ENUM");
        accFlags.put(ACC_MODULE, type -> "MODULE");
        accFlags.put(ACC_RECORD, type -> "RECORD");
        accFlags.put(ACC_DEPRECATED, type -> "DEPRECATED");

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //   Opcodes.
        //   TODO: return type-specified names (like with access modifiers, above).
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        opcodes.put(F_NEW, "F_NEW");
        opcodes.put(T_BOOLEAN, "T_BOOLEAN or H_PUTSTATIC or F_SAME1 or ICONST_1 (*)");
        opcodes.put(T_CHAR, "T_CHAR or H_INVOKEVIRTUAL or ICONST_2 (*)");
        opcodes.put(T_FLOAT, "T_FLOAT or H_INVOKESTATIC or ICONST_3 (*)");
        opcodes.put(T_DOUBLE, "T_DOUBLE or H_INVOKESPECIAL or ICONST_4 (*)");
        opcodes.put(T_BYTE, "T_BYTE or H_NEWINVOKESPECIAL or ICONST_5 (*)");
        opcodes.put(T_SHORT, "T_SHORT or H_INVOKEINTERFACE or LCONST_0 (*)");
        opcodes.put(T_INT, "T_INT or LCONST_1 (*)");
        opcodes.put(T_LONG, "T_LONG or FCONST_0 (*)");
        opcodes.put(H_GETFIELD, "H_GETFIELD or F_APPEND or ACONST_NULL (*)");
        opcodes.put(H_GETSTATIC, "H_GETSTATIC or F_CHOP or ICONST_M1 (*)");
        opcodes.put(H_PUTFIELD, "H_PUTFIELD or F_SAME or ICONST_0 (*)");
        opcodes.put(F_FULL, "F_FULL or NOP (*)");
        opcodes.put(FCONST_1, "FCONST_1");
        opcodes.put(FCONST_2, "FCONST_2");
        opcodes.put(DCONST_0, "DCONST_0");
        opcodes.put(DCONST_1, "DCONST_1");
        opcodes.put(BIPUSH, "BIPUSH");
        opcodes.put(SIPUSH, "SIPUSH");
        opcodes.put(LDC, "LDC");
        opcodes.put(ILOAD, "ILOAD");
        opcodes.put(LLOAD, "LLOAD");
        opcodes.put(FLOAD, "FLOAD");
        opcodes.put(DLOAD, "DLOAD");
        opcodes.put(ALOAD, "ALOAD");
        opcodes.put(IALOAD, "IALOAD");
        opcodes.put(LALOAD, "LALOAD");
        opcodes.put(FALOAD, "FALOAD");
        opcodes.put(DALOAD, "DALOAD");
        opcodes.put(AALOAD, "AALOAD");
        opcodes.put(BALOAD, "BALOAD");
        opcodes.put(CALOAD, "CALOAD");
        opcodes.put(SALOAD, "SALOAD");
        opcodes.put(ISTORE, "ISTORE");
        opcodes.put(LSTORE, "LSTORE");
        opcodes.put(FSTORE, "FSTORE");
        opcodes.put(DSTORE, "DSTORE");
        opcodes.put(ASTORE, "ASTORE");
        opcodes.put(IASTORE, "IASTORE");
        opcodes.put(LASTORE, "LASTORE");
        opcodes.put(FASTORE, "FASTORE");
        opcodes.put(DASTORE, "DASTORE");
        opcodes.put(AASTORE, "AASTORE");
        opcodes.put(BASTORE, "BASTORE");
        opcodes.put(CASTORE, "CASTORE");
        opcodes.put(SASTORE, "SASTORE");
        opcodes.put(POP, "POP");
        opcodes.put(POP2, "POP2");
        opcodes.put(DUP, "DUP");
        opcodes.put(DUP_X1, "DUP_X1");
        opcodes.put(DUP_X2, "DUP_X2");
        opcodes.put(DUP2, "DUP2");
        opcodes.put(DUP2_X1, "DUP2_X1");
        opcodes.put(DUP2_X2, "DUP2_X2");
        opcodes.put(SWAP, "SWAP");
        opcodes.put(IADD, "IADD");
        opcodes.put(LADD, "LADD");
        opcodes.put(FADD, "FADD");
        opcodes.put(DADD, "DADD");
        opcodes.put(ISUB, "ISUB");
        opcodes.put(LSUB, "LSUB");
        opcodes.put(FSUB, "FSUB");
        opcodes.put(DSUB, "DSUB");
        opcodes.put(IMUL, "IMUL");
        opcodes.put(LMUL, "LMUL");
        opcodes.put(FMUL, "FMUL");
        opcodes.put(DMUL, "DMUL");
        opcodes.put(IDIV, "IDIV");
        opcodes.put(LDIV, "LDIV");
        opcodes.put(FDIV, "FDIV");
        opcodes.put(DDIV, "DDIV");
        opcodes.put(IREM, "IREM");
        opcodes.put(LREM, "LREM");
        opcodes.put(FREM, "FREM");
        opcodes.put(DREM, "DREM");
        opcodes.put(INEG, "INEG");
        opcodes.put(LNEG, "LNEG");
        opcodes.put(FNEG, "FNEG");
        opcodes.put(DNEG, "DNEG");
        opcodes.put(ISHL, "ISHL");
        opcodes.put(LSHL, "LSHL");
        opcodes.put(ISHR, "ISHR");
        opcodes.put(LSHR, "LSHR");
        opcodes.put(IUSHR, "IUSHR");
        opcodes.put(LUSHR, "LUSHR");
        opcodes.put(IAND, "IAND");
        opcodes.put(LAND, "LAND");
        opcodes.put(IOR, "IOR");
        opcodes.put(LOR, "LOR");
        opcodes.put(IXOR, "IXOR");
        opcodes.put(LXOR, "LXOR");
        opcodes.put(IINC, "IINC");
        opcodes.put(I2L, "I2L");
        opcodes.put(I2F, "I2F");
        opcodes.put(I2D, "I2D");
        opcodes.put(L2I, "L2I");
        opcodes.put(L2F, "L2F");
        opcodes.put(L2D, "L2D");
        opcodes.put(F2I, "F2I");
        opcodes.put(F2L, "F2L");
        opcodes.put(F2D, "F2D");
        opcodes.put(D2I, "D2I");
        opcodes.put(D2L, "D2L");
        opcodes.put(D2F, "D2F");
        opcodes.put(I2B, "I2B");
        opcodes.put(I2C, "I2C");
        opcodes.put(I2S, "I2S");
        opcodes.put(LCMP, "LCMP");
        opcodes.put(FCMPL, "FCMPL");
        opcodes.put(FCMPG, "FCMPG");
        opcodes.put(DCMPL, "DCMPL");
        opcodes.put(DCMPG, "DCMPG");
        opcodes.put(IFEQ, "IFEQ");
        opcodes.put(IFNE, "IFNE");
        opcodes.put(IFLT, "IFLT");
        opcodes.put(IFGE, "IFGE");
        opcodes.put(IFGT, "IFGT");
        opcodes.put(IFLE, "IFLE");
        opcodes.put(IF_ICMPEQ, "IF_ICMPEQ");
        opcodes.put(IF_ICMPNE, "IF_ICMPNE");
        opcodes.put(IF_ICMPLT, "IF_ICMPLT");
        opcodes.put(IF_ICMPGE, "IF_ICMPGE");
        opcodes.put(IF_ICMPGT, "IF_ICMPGT");
        opcodes.put(IF_ICMPLE, "IF_ICMPLE");
        opcodes.put(IF_ACMPEQ, "IF_ACMPEQ");
        opcodes.put(IF_ACMPNE, "IF_ACMPNE");
        opcodes.put(GOTO, "GOTO");
        opcodes.put(JSR, "JSR");
        opcodes.put(RET, "RET");
        opcodes.put(TABLESWITCH, "TABLESWITCH");
        opcodes.put(LOOKUPSWITCH, "LOOKUPSWITCH");
        opcodes.put(IRETURN, "IRETURN");
        opcodes.put(LRETURN, "LRETURN");
        opcodes.put(FRETURN, "FRETURN");
        opcodes.put(DRETURN, "DRETURN");
        opcodes.put(ARETURN, "ARETURN");
        opcodes.put(RETURN, "RETURN");
        opcodes.put(GETSTATIC, "GETSTATIC");
        opcodes.put(PUTSTATIC, "PUTSTATIC");
        opcodes.put(GETFIELD, "GETFIELD");
        opcodes.put(PUTFIELD, "PUTFIELD");
        opcodes.put(INVOKEVIRTUAL, "INVOKEVIRTUAL");
        opcodes.put(INVOKESPECIAL, "INVOKESPECIAL");
        opcodes.put(INVOKESTATIC, "INVOKESTATIC");
        opcodes.put(INVOKEINTERFACE, "INVOKEINTERFACE");
        opcodes.put(INVOKEDYNAMIC, "INVOKEDYNAMIC");
        opcodes.put(NEW, "NEW");
        opcodes.put(NEWARRAY, "NEWARRAY");
        opcodes.put(ANEWARRAY, "ANEWARRAY");
        opcodes.put(ARRAYLENGTH, "ARRAYLENGTH");
        opcodes.put(ATHROW, "ATHROW");
        opcodes.put(CHECKCAST, "CHECKCAST");
        opcodes.put(INSTANCEOF, "INSTANCEOF");
        opcodes.put(MONITORENTER, "MONITORENTER");
        opcodes.put(MONITOREXIT, "MONITOREXIT");
        opcodes.put(MULTIANEWARRAY, "MULTIANEWARRAY");
        opcodes.put(IFNULL, "IFNULL");
        opcodes.put(IFNONNULL, "IFNONNULL");
    }

    @NonNull
    private final ClassNode cls;

    private final StringBuilder sb = new StringBuilder();

    private String cache;

    private int currentIndent;

    @Override
    public String getHumanReadableString() {
        if (cache == null) {
            printWholeClass();
            cache = sb.toString();
        }

        return cache;
    }

    @Override
    public void increaseIndent() {
        currentIndent++;
    }

    @Override
    public void reduceIndent() {
        currentIndent--;
    }

    @Override
    public StringBuilder getBuilder() {
        return sb;
    }
    
    @Override
    public StringBuilder indent() {
        for (int i = 0; i < currentIndent; i++)
            sb.append(INDENT);
        return sb;
    }

    private void printWholeClass() {
        indent().append("CLASS ").append(cls.name).append(' ');
        printAccessString(cls.access, ClassNode.class);
        sb.append(MARGIN_X2);
        currentIndent++;

        indent().append("Version: ").append(cls.version).append(MARGIN_X2);

        if (cls.sourceFile != null)
            indent().append("Source file: ").append(cls.sourceFile).append(MARGIN_X2);

        if (cls.sourceDebug != null)
            indent().append("Source debug: ").append(cls.sourceDebug).append(MARGIN_X2);

        if (cls.signature != null)
            indent().append("Signature: ").append(cls.signature).append(MARGIN_X2);

        if (cls.superName != null)
            indent().append("Extends: ").append(cls.superName).append(MARGIN_X2);

        if (cls.interfaces != null && !cls.interfaces.isEmpty()) {
            indent().append("Implements directly:\n");
            increaseIndent();

            for (String itf : cls.interfaces)
                indent().append("- ").append(itf).append('\n');

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (cls.module != null)
            indent().append("Module: ").append(cls.module).append(MARGIN_X2);

        if (cls.outerClass != null)
            indent().append("Outer class: ").append(cls.outerClass).append(MARGIN_X2);

        if (cls.outerMethod != null)
            indent().append("Outer method: ").append(cls.outerMethod).append(MARGIN_X2);

        if (cls.outerMethodDesc != null)
            indent().append("Outer method descriptor: ").append(cls.outerMethodDesc).append(MARGIN_X2);

        if (cls.visibleAnnotations != null && !cls.visibleAnnotations.isEmpty()) {
            indent().append("Visible annotations:");
            printAnnotations(cls.visibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (cls.invisibleAnnotations != null && !cls.invisibleAnnotations.isEmpty()) {
            indent().append("Invisible annotations:");
            printAnnotations(cls.invisibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (cls.visibleTypeAnnotations != null && !cls.visibleTypeAnnotations.isEmpty()) {
            indent().append("Visible type annotations:");
            printAnnotations(cls.visibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (cls.invisibleTypeAnnotations != null && !cls.invisibleTypeAnnotations.isEmpty()) {
            indent().append("Invisible type annotations:");
            printAnnotations(cls.invisibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (cls.attrs != null && !cls.attrs.isEmpty()) {
            indent().append("Attributes:\n");
            printAttributes(cls.attrs);
            sb.append(MARGIN_X2);
        }

        if (cls.innerClasses != null && !cls.innerClasses.isEmpty()) {
            indent().append("Inner classes:\n");
            increaseIndent();

            for (InnerClassNode inner : cls.innerClasses) {
                indent().append("- ").append(inner.name).append(' ');
                printAccessString(inner.access, ClassNode.class);
                sb.append('\n');
            }

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (cls.nestHostClass != null)
            indent().append("Nest host class: ").append(cls.nestHostClass).append(MARGIN_X2);

        if (cls.nestMembers != null && !cls.nestMembers.isEmpty()) {
            indent().append("Nest members:\n");
            increaseIndent();

            for (String member : cls.nestMembers)
                indent().append("- ").append(member).append('\n');

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (cls.permittedSubclasses != null && !cls.permittedSubclasses.isEmpty()) {
            indent().append("Permitted subclasses:\n");
            increaseIndent();

            for (String subclass : cls.permittedSubclasses)
                indent().append("- ").append(subclass).append('\n');

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (cls.recordComponents != null && !cls.recordComponents.isEmpty()) {
            indent().append("Record components:");

            for (RecordComponentNode rec : cls.recordComponents)
                printRecordComponent(rec);

            sb.append(MARGIN_X2);
        }

        if (cls.fields != null && !cls.fields.isEmpty()) {
            indent().append("Fields:");

            for (FieldNode fld : cls.fields)
                printField(fld);

            sb.append(MARGIN_X2);
        }

        if (cls.methods != null && !cls.methods.isEmpty()) {
            indent().append("Methods:");

            for (MethodNode mtd : cls.methods)
                printMethod(mtd);

            sb.append(MARGIN_X2);
        }

        sb.append(MARGIN_X2);
        sb.append("END\n");
    }

    private void printAnnotations(List<? extends AnnotationNode> annotations) {
        increaseIndent();

        for (AnnotationNode anno : annotations) {
            sb.append(MARGIN_X2);
            indent().append("- @").append(anno.desc.replace("\n", "\\n"));

            if (anno.values != null && !anno.values.isEmpty()) {
                sb.append(" (");

                if (anno.values.size() % 2 == 0) {
                    for (int i = 0; i < anno.values.size() - 1; i += 2) {
                        Object val = anno.values.get(i + 1);

                        sb.append(anno.values.get(i)).append(" = ");
                        sb.append(val instanceof String ? "\"" + val + "\"" : val);
                        sb.append(" (value type: ").append(val.getClass().getSimpleName()).append(')');
                        sb.append(", ");
                    }

                    sb.delete(sb.length() - 2, sb.length()); // delete trailing comma+space
                } else
                    sb.append("invalid annotation values");

                sb.append(')');

                if (anno instanceof TypeAnnotationNode) {
                    TypeAnnotationNode typeAnno = (TypeAnnotationNode) anno;
                    sb.append("{ TYPE ANNOTATION | type reference: ").append(typeAnno.typeRef)
                            .append(", type path: ").append(typeAnno.typePath.toString()).append(" }");

                    if (anno instanceof LocalVariableAnnotationNode) {
                        LocalVariableAnnotationNode lVarAnno = (LocalVariableAnnotationNode) anno;

                        sb.append("{ LOCAL VARIABLE ANNOTATION | indexed: ")
                                .append(lVarAnno.start.size()).append(" times").append(" }");
                    }
                }
            }
        }

        reduceIndent();
    }
    
    private void printAttributes(List<Attribute> attributes) {
        increaseIndent();

        for (Attribute attr : attributes) {
            indent().append("- ").append(attr.type);

            if (attr.isCodeAttribute())
                sb.append(" (code attribute)");

            if (attr.isUnknown())
                sb.append(" (unknown attribute)");

            sb.append('\n');
        }

        reduceIndent();
    }
    
    private void printRecordComponent(RecordComponentNode rec) {
        increaseIndent();

        sb.append(MARGIN_X2);
        indent().append("RECORD COMPONENT ").append(rec.name).append(' ');
        sb.append(MARGIN_X2);

        increaseIndent();

        if (rec.descriptor != null)
            indent().append("Descriptor: ").append(rec.descriptor).append(MARGIN_X2);

        if (rec.signature != null)
            indent().append("Signature: ").append(rec.signature).append(MARGIN_X2);

        if (rec.visibleAnnotations != null && !rec.visibleAnnotations.isEmpty()) {
            indent().append("Visible annotations:");
            printAnnotations(rec.visibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (rec.invisibleAnnotations != null && !rec.invisibleAnnotations.isEmpty()) {
            indent().append("Invisible annotations:");
            printAnnotations(rec.invisibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (rec.visibleTypeAnnotations != null && !rec.visibleTypeAnnotations.isEmpty()) {
            indent().append("Visible type annotations:");
            printAnnotations(rec.visibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (rec.invisibleTypeAnnotations != null && !rec.invisibleTypeAnnotations.isEmpty()) {
            indent().append("Invisible type annotations:");
            printAnnotations(rec.invisibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (rec.attrs != null && !rec.attrs.isEmpty()) {
            indent().append("Attributes:\n");
            printAttributes(rec.attrs);
            sb.append(MARGIN_X2);
        }

        reduceIndent();
        reduceIndent();
    }

    private void printField(FieldNode fld) {
        increaseIndent();

        sb.append(MARGIN_X2);
        indent().append("FIELD ").append(fld.name).append(' ');
        printAccessString(fld.access, FieldNode.class);
        sb.append(MARGIN_X2);

        increaseIndent();

        if (fld.desc != null)
            indent().append("Descriptor: ").append(fld.desc).append(MARGIN_X2);

        if (fld.signature != null)
            indent().append("Signature: ").append(fld.signature).append(MARGIN_X2);

        if (fld.value != null)
            indent().append("Initial value: ")
                    .append(fld.value instanceof String ? "\"" + fld.value + "\"" : fld.value)
                    .append(MARGIN_X2);

        if (fld.visibleAnnotations != null && !fld.visibleAnnotations.isEmpty()) {
            indent().append("Visible annotations:");
            printAnnotations(fld.visibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (fld.invisibleAnnotations != null && !fld.invisibleAnnotations.isEmpty()) {
            indent().append("Invisible annotations:");
            printAnnotations(fld.invisibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (fld.visibleTypeAnnotations != null && !fld.visibleTypeAnnotations.isEmpty()) {
            indent().append("Visible type annotations:");
            printAnnotations(fld.visibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (fld.invisibleTypeAnnotations != null && !fld.invisibleTypeAnnotations.isEmpty()) {
            indent().append("Invisible type annotations:");
            printAnnotations(fld.invisibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (fld.attrs != null && !fld.attrs.isEmpty()) {
            indent().append("Attributes:\n");
            printAttributes(fld.attrs);
            sb.append(MARGIN_X2);
        }

        reduceIndent();
        reduceIndent();
    }

    private void printMethod(MethodNode mtd) {
        increaseIndent();

        sb.append(MARGIN_X2);
        indent().append("METHOD ").append(mtd.name).append(' ');
        printAccessString(mtd.access, MethodNode.class);
        sb.append(MARGIN_X2);

        increaseIndent();

        if (mtd.desc != null)
            indent().append("Descriptor: ").append(mtd.desc).append(MARGIN_X2);

        if (mtd.signature != null)
            indent().append("Signature: ").append(mtd.signature).append(MARGIN_X2);

        if (mtd.exceptions != null && !mtd.exceptions.isEmpty()) {
            indent().append("Throws:\n");
            increaseIndent();

            for (String ex : mtd.exceptions)
                indent().append("- ").append(ex).append('\n');

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (mtd.parameters != null && !mtd.parameters.isEmpty()) {
            indent().append("Parameters:\n");
            increaseIndent();

            for (ParameterNode param : mtd.parameters)
                indent().append("- ").append(param.name).append('\n');

            reduceIndent();
            sb.append(MARGIN_X2);
        }

        if (mtd.visibleAnnotations != null && !mtd.visibleAnnotations.isEmpty()) {
            indent().append("Visible annotations:");
            printAnnotations(mtd.visibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (mtd.invisibleAnnotations != null && !mtd.invisibleAnnotations.isEmpty()) {
            indent().append("Invisible annotations:");
            printAnnotations(mtd.invisibleAnnotations);
            sb.append(MARGIN_X2);
        }

        if (mtd.visibleTypeAnnotations != null && !mtd.visibleTypeAnnotations.isEmpty()) {
            indent().append("Visible type annotations:");
            printAnnotations(mtd.visibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (mtd.invisibleTypeAnnotations != null && !mtd.invisibleTypeAnnotations.isEmpty()) {
            indent().append("Invisible type annotations:");
            printAnnotations(mtd.invisibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (mtd.attrs != null && !mtd.attrs.isEmpty()) {
            indent().append("Attributes:\n");
            printAttributes(mtd.attrs);
            sb.append(MARGIN_X2);
        }

        if (mtd.annotationDefault != null)
            indent().append("Annotation default: ").append(mtd.annotationDefault).append(MARGIN_X2);

        if (mtd.visibleAnnotableParameterCount > 0)
            indent().append("Visible annotation parameters count: ")
                    .append(mtd.visibleAnnotableParameterCount).append(MARGIN_X2);

        if (mtd.invisibleAnnotableParameterCount > 0)
            indent().append("Invisible annotation parameters count: ")
                    .append(mtd.invisibleAnnotableParameterCount).append(MARGIN_X2);

        indent().append("Max stack: ").append(mtd.maxStack).append('\n');
        indent().append("Max locals: ").append(mtd.maxLocals).append(MARGIN_X2);

        if (mtd.localVariables != null && !mtd.localVariables.isEmpty()) {
            indent().append("Local variables:");

            for (LocalVariableNode lVar : mtd.localVariables)
                printLocalVariable(lVar);

            sb.append(MARGIN_X2);
        }

        if (mtd.tryCatchBlocks != null && !mtd.tryCatchBlocks.isEmpty()) {
            indent().append("Try/catch blocks:");

            for (TryCatchBlockNode tc : mtd.tryCatchBlocks)
                printTryCatchBlock(tc);

            sb.append(MARGIN_X2);
        }

        if (mtd.instructions != null && mtd.instructions.size() > 0) {
            indent().append("Instructions:").append(MARGIN_X2);
            increaseIndent();
            indent().append(padWithSpaces("Offset", INSN_OFFSET_PADDING))
                    .append(padWithSpaces("Opcode", OPCODE_PADDING))
                    .append(padWithSpaces("Opcode name or variants", OPCODE_NAME_PADDING))
                    .append("Operand (instruction target)")
                    .append("\n\n"); // 1 empty line

            for (AbstractInsnNode insn : mtd.instructions)
                printInstruction(insn);

            reduceIndent();
        }

        sb.append(MARGIN_X2);

        reduceIndent();
        reduceIndent();
    }

    private void printLocalVariable(LocalVariableNode lVar) {
        increaseIndent();
        sb.append(MARGIN_X2);
        indent().append("LOCAL Var_").append(lVar.index).append(MARGIN_X2);
        increaseIndent();

        if (lVar.name != null)
            indent().append("Name: ").append(lVar.name).append(MARGIN_X2);

        if (lVar.desc != null)
            indent().append("Descriptor: ").append(lVar.desc).append(MARGIN_X2);

        if (lVar.signature != null)
            indent().append("Signature: ").append(lVar.signature).append(MARGIN_X2);

        indent().append("Start offset: #").append(lVar.start == null
                ? "N/A" : resolveInstructionOffset(lVar.start)).append(MARGIN_X2);
        indent().append("End offset: #").append(lVar.end == null
                ? "N/A" : resolveInstructionOffset(lVar.end)).append(MARGIN_X2);

        reduceIndent();
        reduceIndent();
    }

    private void printTryCatchBlock(TryCatchBlockNode tc) {
        increaseIndent();
        sb.append(MARGIN_X2);

        if (tc.type != null)
            indent().append("CATCH ").append(tc.type);
        else
            indent().append("FINALLY");

        sb.append(MARGIN_X2);
        increaseIndent();

        if (tc.visibleTypeAnnotations != null && !tc.visibleTypeAnnotations.isEmpty()) {
            indent().append("Visible type annotations:");
            printAnnotations(tc.visibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        if (tc.invisibleTypeAnnotations != null && !tc.invisibleTypeAnnotations.isEmpty()) {
            indent().append("Invisible type annotations:");
            printAnnotations(tc.invisibleTypeAnnotations);
            sb.append(MARGIN_X2);
        }

        indent().append("Start offset: #").append(tc.start == null
                ? "N/A" : resolveInstructionOffset(tc.start)).append('\n');
        indent().append("End offset: #").append(tc.end == null
                ? "N/A" : resolveInstructionOffset(tc.end)).append('\n');
        indent().append("Handler offset: #").append(tc.handler == null
                ? "N/A" : resolveInstructionOffset(tc.handler)).append('\n');

        reduceIndent();
        reduceIndent();
    }

    private void printInstruction(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        String opcodeName = opcodes.get(opcode);
        int offset = resolveInstructionOffset(insn);
        String result = padWithSpaces("#" + offset, INSN_OFFSET_PADDING)
                      + padWithSpaces("0x" + Integer.toString(opcode, 16), OPCODE_PADDING);

        if (opcodeName == null)
            result += "(invalid opcode)";
        else {
            result += padWithSpaces(inverseCase(opcodeName), OPCODE_NAME_PADDING);

            switch (insn.getType()) {
                case AbstractInsnNode.FIELD_INSN:
                    FieldInsnNode fin = (FieldInsnNode) insn;
                    result += fin.owner + "#" + fin.name + " (" + fin.desc + ")";
                    break;

                case AbstractInsnNode.METHOD_INSN:
                    MethodInsnNode min = (MethodInsnNode) insn;
                    result += min.owner + "#" + min.name + " (" + min.desc + ")";
                    break;

                case AbstractInsnNode.VAR_INSN:
                    VarInsnNode vin = (VarInsnNode) insn;
                    result += "Var_" + vin.var;
                    break;

                case AbstractInsnNode.TYPE_INSN:
                    TypeInsnNode tin = (TypeInsnNode) insn;
                    result += tin.desc;
                    break;

                case AbstractInsnNode.JUMP_INSN:
                    JumpInsnNode jin = (JumpInsnNode) insn;
                    result += "Label_" + resolveInstructionOffset(jin.label);
                    break;

                case AbstractInsnNode.LDC_INSN:
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    result += ldc.cst instanceof String ? "\"" + ldc.cst + "\"" : ldc.cst;
                    break;

                case AbstractInsnNode.INT_INSN:
                    result += resolveIntValue(insn);
                    break;

                case AbstractInsnNode.IINC_INSN:
                    IincInsnNode iinc = (IincInsnNode) insn;
                    result += "Var_" + iinc.var + " (+" + iinc.incr + ")";
                    break;

                case AbstractInsnNode.FRAME:
                    FrameNode fn = (FrameNode) insn;
                    result += "stack: "    + (fn.stack == null ? "N/A" : fn.stack.size())
                            + ", locals: " + (fn.local == null ? "N/A" : fn.local.size());
                    break;

                case AbstractInsnNode.LABEL:
                    LabelNode ln = (LabelNode) insn;
                    result += "Label_" + resolveInstructionOffset(ln);
                    break;
            }
        }

        indent().append(result).append('\n');
    }

    private static String padWithSpaces(String s, int finalLength) {
        StringBuilder result = new StringBuilder(s);

        for (int i = 0; i < finalLength - s.length(); i++)
            result.append(' ');

        return result.toString();
    }

    private void printAccessString(int access, Class<?> type) {
        sb.append('[');
        boolean anyFlagValid = false;

        for (int mod : accFlags.keySet()) {
            if ((access & mod) != 0) {
                sb.append(accFlags.get(mod).apply(type)).append(' ');
                anyFlagValid = true;
            }
        }

        if (anyFlagValid)
            sb.deleteCharAt(sb.length() - 1); // delete trailing space
        else
            sb.append("INVALID ACCESS MODIFIERS: 0x")
                    .append(Integer.toString(access, 16).toUpperCase());

        sb.append(']');
    }

    private static int resolveInstructionOffset(AbstractInsnNode insn) {
        int offset = 0;

        while (insn.getPrevious() != null) {
            insn = insn.getPrevious();
            offset++;
        }

        return offset;
    }

    private static int resolveIntValue(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            default:
                return ((IntInsnNode) insn).operand;

            case ICONST_M1: return -1;
            case ICONST_0 : return  0;
            case ICONST_1 : return  1;
            case ICONST_2 : return  2;
            case ICONST_3 : return  3;
            case ICONST_4 : return  4;
            case ICONST_5 : return  5;
        }
    }

    private static String inverseCase(String s) {
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char orig = chars[i];
            chars[i] = Character.isUpperCase(orig)
                     ? Character.toLowerCase(orig)
                     : Character.toUpperCase(orig);
        }

        return new String(chars);
    }

}
