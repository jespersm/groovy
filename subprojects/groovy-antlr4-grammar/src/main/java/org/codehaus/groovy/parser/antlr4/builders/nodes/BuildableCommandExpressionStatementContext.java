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

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableCommandExpressionStatementContext extends BuildableBaseNode {
    private GroovyParser.CommandExpressionStatementContext ctx;

    public BuildableCommandExpressionStatementContext(ASTBuilder astBuilder, GroovyParser.CommandExpressionStatementContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Statement build() {
        Expression expression = null;
        List<List<ParseTree>> list = DefaultGroovyMethods.collate(ctx.cmdExpressionRule().children, 2);
        for (List<ParseTree> c : list) {
            final Iterator<ParseTree> iterator = c.iterator();
            ParseTree c1 = iterator.hasNext() ? iterator.next() : null;
            ParseTree c0 = iterator.hasNext() ? iterator.next() : null;

            if (c.size() == 1) expression = new PropertyExpression(expression, c1.getText());
            else {
                assert c0 instanceof GroovyParser.ArgumentListContext;
                if (c1 instanceof TerminalNode) {
                    expression = new MethodCallExpression(expression, ((TerminalNode)c1).getText(), astBuilder.createArgumentList((GroovyParser.ArgumentListContext)c0));
                    ((MethodCallExpression)expression).setImplicitThis(false);
                } else if (c1 instanceof GroovyParser.PathExpressionContext) {
                    String methodName;
                    boolean implicitThis;
                    ArrayList<Object> objects = astBuilder.parsePathExpression((GroovyParser.PathExpressionContext)c1);
                    expression = (Expression)objects.get(0);
                    methodName = (String)objects.get(1);
                    implicitThis = (Boolean)objects.get(2);

                    expression = new MethodCallExpression(expression, methodName, astBuilder.createArgumentList((GroovyParser.ArgumentListContext)c0));
                    ((MethodCallExpression)expression).setImplicitThis(implicitThis);
                }

            }

        }


        return astBuilder.setupNodeLocation(new ExpressionStatement(expression), ctx);
    }
}
