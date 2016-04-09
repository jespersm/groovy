/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.parser.antlr4.builders.nodes;

import groovy.lang.Closure;
import org.codehaus.groovy.antlr.EnumHelper;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableClassDeclarationContext extends BuildableBaseNode {
    private GroovyParser.ClassDeclarationContext ctx;

    public BuildableClassDeclarationContext(ASTBuilder astBuilder, GroovyParser.ClassDeclarationContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public ClassNode build() {
        boolean isEnum = asBoolean(ctx.KW_ENUM());

        final ClassNode parentClass = asBoolean(astBuilder.classes) ? astBuilder.classes.peek() : null;
        ClassNode[] interfaces = asBoolean(ctx.implementsClause())
                ? DefaultGroovyMethods.asType(collect(ctx.implementsClause().genericClassNameExpression(), new Closure<ClassNode>(this, this) {
            public ClassNode doCall(GroovyParser.GenericClassNameExpressionContext it) {return astBuilder.parseExpression(it);}
        }), ClassNode[].class)
                : new ClassNode[0];

        ClassNode classNode;
        if (parentClass != null) {
            String string = parentClass.getName() + "$" + String.valueOf(ctx.IDENTIFIER());
            classNode = new InnerClassNode(parentClass, string, Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
        } else {
            final String name = astBuilder.moduleNode.getPackageName();
            classNode = isEnum ? EnumHelper.makeEnumNode(ctx.IDENTIFIER().getText(), Modifier.PUBLIC, interfaces, null)
                    : new ClassNode((name != null && asBoolean(name) ? name : "") + String.valueOf(ctx.IDENTIFIER()), Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
        }


        astBuilder.setupNodeLocation(classNode, ctx);
        astBuilder.attachAnnotations(classNode, ctx.annotationClause());

        if (asBoolean(ctx.KW_TRAIT())) {
            astBuilder.attachTraitTransformAnnotation(classNode);
        }

        astBuilder.moduleNode.addClass(classNode);
        if (asBoolean(ctx.extendsClause()))
            (classNode).setSuperClass(astBuilder.parseExpression(ctx.extendsClause().genericClassNameExpression()));

        if (asBoolean(ctx.implementsClause()))
            (classNode).setInterfaces(interfaces);

        if (!isEnum) {
            (classNode).setGenericsTypes(astBuilder.parseGenericDeclaration(ctx.genericDeclarationList()));
            (classNode).setUsingGenerics((classNode.getGenericsTypes() != null && classNode.getGenericsTypes().length != 0) || (classNode).getSuperClass().isUsingGenerics() || DefaultGroovyMethods.any(classNode.getInterfaces(), new Closure<Boolean>(this, this) {
                public Boolean doCall(ClassNode it) {return it.isUsingGenerics();}
            }));
        }


        classNode.setModifiers(astBuilder.parseClassModifiers(ctx.classModifier()) |
                (isEnum ? (Opcodes.ACC_ENUM | Opcodes.ACC_FINAL)
                        : ((asBoolean(ctx.KW_INTERFACE())
                        ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT
                        : 0)
                )
                )
        );

        classNode.setSyntheticPublic((classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0);
        classNode.setModifiers(classNode.getModifiers() & ~Opcodes.ACC_SYNTHETIC);// FIXME Magic with synthetic modifier.


        if (asBoolean(ctx.AT())) {
            classNode.addInterface(ClassHelper.Annotation_TYPE);
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_ANNOTATION);
        }


        astBuilder.classes.add(classNode);
        astBuilder.parseClassBody(classNode, ctx.classBody());
        astBuilder.classes.pop();

        if (classNode.isInterface()) { // FIXME why interface has null mixin
            try {
                // FIXME Hack with visibility.
                Field field = classNode.getClass().getDeclaredField("mixins");
                field.setAccessible(true);
                field.set(classNode, null);
            } catch (IllegalAccessException e) {
                log.warning(astBuilder.createExceptionMessage(e));
            } catch (NoSuchFieldException e) {
                log.warning(astBuilder.createExceptionMessage(e));
            }
        }
        return classNode;
    }

    private Logger log = Logger.getLogger(BuildableClassDeclarationContext.class.getName());
}
