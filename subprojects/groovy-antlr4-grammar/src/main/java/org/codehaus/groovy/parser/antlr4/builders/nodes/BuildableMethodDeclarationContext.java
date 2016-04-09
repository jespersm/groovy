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
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;

import java.util.List;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableMethodDeclarationContext extends BuildableBaseNode {
    private GroovyParser.MethodDeclarationContext ctx;

    public BuildableMethodDeclarationContext(ASTBuilder astBuilder, GroovyParser.MethodDeclarationContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public MethodNode build() {

        return astBuilder.parseMethodDeclaration(null, ctx, new Closure<MethodNode>(this, this) {
                    public MethodNode doCall(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx, String methodName, int modifiers, ClassNode returnType, Parameter[] params, ClassNode[] exceptions, Statement statement, List<InnerClassNode> innerClassesDeclared) {

                        final MethodNode methodNode = new MethodNode(methodName, modifiers, returnType, params, exceptions, statement);
                        methodNode.setGenericsTypes(astBuilder.parseGenericDeclaration(ctx.genericDeclarationList()));
                        methodNode.setAnnotationDefault(true);

                        return methodNode;
                    }
                }
        );
    }
}
