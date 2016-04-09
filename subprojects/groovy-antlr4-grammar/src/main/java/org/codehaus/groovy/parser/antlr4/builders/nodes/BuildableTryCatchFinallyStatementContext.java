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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableTryCatchFinallyStatementContext extends BuildableBaseNode {
    private GroovyParser.TryCatchFinallyStatementContext ctx;

    public BuildableTryCatchFinallyStatementContext(ASTBuilder astBuilder, GroovyParser.TryCatchFinallyStatementContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Statement build() {
        Object finallyStatement;

        GroovyParser.BlockStatementContext finallyBlockStatement = ctx.finallyBlock() != null ? ctx.finallyBlock().blockStatement() : null;
        if (finallyBlockStatement != null) {
            BlockStatement fbs = new BlockStatement();
            astBuilder.unpackStatement(fbs, astBuilder.parseStatement(finallyBlockStatement));
            finallyStatement = astBuilder.setupNodeLocation(fbs, finallyBlockStatement);

        } else finallyStatement = EmptyStatement.INSTANCE;

        final TryCatchStatement statement = new TryCatchStatement(astBuilder.parseStatement(DefaultGroovyMethods.asType(ctx.tryBlock().blockStatement(), GroovyParser.BlockStatementContext.class)), (Statement)finallyStatement);
        DefaultGroovyMethods.each(ctx.catchBlock(), new Closure<List<GroovyParser.ClassNameExpressionContext>>(null, null) {
            public List<GroovyParser.ClassNameExpressionContext> doCall(GroovyParser.CatchBlockContext it) {
                final Statement catchBlock = astBuilder.parseStatement(DefaultGroovyMethods.asType(it.blockStatement(), GroovyParser.BlockStatementContext.class));
                final String var = it.IDENTIFIER().getText();

                List<GroovyParser.ClassNameExpressionContext> classNameExpression = it.classNameExpression();
                if (!asBoolean(classNameExpression))
                    statement.addCatch(astBuilder.setupNodeLocation(new CatchStatement(new Parameter(ClassHelper.OBJECT_TYPE, var), catchBlock), it));
                else {
                    DefaultGroovyMethods.each(classNameExpression, new Closure<Object>(null, null) {
                        public void doCall(GroovyParser.ClassNameExpressionContext it) {
                            statement.addCatch(astBuilder.setupNodeLocation(new CatchStatement(new Parameter(astBuilder.parseExpression(DefaultGroovyMethods.asType(it, GroovyParser.ClassNameExpressionContext.class)), var), catchBlock), it));
                        }
                    });
                }
                return null;
            }
        });
        return statement;

    }
}
