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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.objectweb.asm.Opcodes;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableNewInstanceRuleContext extends BuildableBaseNode {
    private GroovyParser.NewInstanceRuleContext ctx;

    public BuildableNewInstanceRuleContext(ASTBuilder astBuilder, GroovyParser.NewInstanceRuleContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public ConstructorCallExpression build() {
        ClassNode creatingClass = asBoolean(ctx.genericClassNameExpression())
                ? astBuilder.parseExpression(ctx.genericClassNameExpression())
                : astBuilder.parseExpression(ctx.classNameExpression());
        if (asBoolean(ctx.LT())) creatingClass.setGenericsTypes(new GenericsType[0]);

        ConstructorCallExpression expression;
        if (!asBoolean(ctx.classBody())) {
            expression = astBuilder.setupNodeLocation(new ConstructorCallExpression(creatingClass, astBuilder.createArgumentList(ctx.argumentList())), ctx);
        } else {
            ClassNode outer = astBuilder.classes.peek();
            InnerClassNode classNode = new InnerClassNode(outer, outer.getName() + "$" + String.valueOf((astBuilder.anonymousClassesCount = ++astBuilder.anonymousClassesCount)), Opcodes.ACC_PUBLIC, ClassHelper.make(creatingClass.getName()));
            expression = astBuilder.setupNodeLocation(new ConstructorCallExpression(classNode, astBuilder.createArgumentList(ctx.argumentList())), ctx);
            expression.setUsingAnonymousInnerClass(true);
            classNode.setAnonymous(true);
            DefaultGroovyMethods.last(astBuilder.innerClassesDefinedInMethod).add(classNode);
            astBuilder.moduleNode.addClass(classNode);
            astBuilder.classes.add(classNode);
            astBuilder.parseClassBody(classNode, ctx.classBody());
            astBuilder.classes.pop();
        }

        return expression;
    }
}
