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

import org.antlr.v4.runtime.Token;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableIndexExpressionContext extends BuildableBaseNode {
    private GroovyParser.IndexExpressionContext ctx;

    public BuildableIndexExpressionContext(ASTBuilder astBuilder, GroovyParser.IndexExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        // parse the lhs
        Expression leftExpression = astBuilder.parseExpression(ctx.expression(0));
        int expressionCount = ctx.expression().size();
        List<Expression> expressions = new LinkedList<Expression>();
        Expression rightExpression = null;

        // parse the indices
        for (int i = 1; i < expressionCount; ++i) {
            expressions.add(astBuilder.parseExpression(ctx.expression(i)));
        }
        if (expressionCount == 2) {
            // If only one index, treat as single expression
            rightExpression = expressions.get(0);
            // unless it's a spread operator...
            if (rightExpression instanceof SpreadExpression) {
                ListExpression wrapped = new ListExpression();
                wrapped.addExpression(rightExpression);
                rightExpression = astBuilder.setupNodeLocation(wrapped, ctx.expression(1));
            }
        } else {
            // Otherwise, setup as list expression
            ListExpression listExpression = new ListExpression(expressions);
            listExpression.setWrapped(true);
            rightExpression = listExpression;
            // if nonempty, set location info for index list
            if (expressionCount > 2) {
                Token start = ctx.expression(1).getStart();
                Token stop = ctx.expression(expressionCount - 1).getStart();
                listExpression.setLineNumber(start.getLine());
                listExpression.setColumnNumber(start.getCharPositionInLine() + 1);
                listExpression.setLastLineNumber(stop.getLine());
                listExpression.setLastColumnNumber(stop.getCharPositionInLine() + 1 + stop.getText().length());
            }
        }
        BinaryExpression binaryExpression = new BinaryExpression(leftExpression, astBuilder.createToken(ctx.LBRACK(), 1), rightExpression);
        return astBuilder.setupNodeLocation(binaryExpression, ctx);
    }
}
