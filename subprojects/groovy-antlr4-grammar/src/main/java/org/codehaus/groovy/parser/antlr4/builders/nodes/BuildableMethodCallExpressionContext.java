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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableMethodCallExpressionContext extends BuildableBaseNode {
    private GroovyParser.MethodCallExpressionContext ctx;

    public BuildableMethodCallExpressionContext(ASTBuilder astBuilder, GroovyParser.MethodCallExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        GroovyParser.SelectorNameContext methodSelector = ctx.selectorName();
        Expression method = methodSelector != null ? new ConstantExpression(methodSelector.getText()) : (
                ctx.STRING() != null ? astBuilder.parseConstantStringToken(ctx.STRING().getSymbol()) : astBuilder.parseExpression(ctx.gstring())
        );
        Expression argumentListExpression = astBuilder.createArgumentList(ctx.argumentList());
        MethodCallExpression expression = new MethodCallExpression(astBuilder.parseExpression(ctx.expression()), method, argumentListExpression);
        expression.setImplicitThis(false);
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        expression.setSpreadSafe(op.getSymbol().getType() == GroovyParser.STAR_DOT);
        expression.setSafe(op.getSymbol().getType() == GroovyParser.SAFE_DOT);
        return expression;
    }
}
