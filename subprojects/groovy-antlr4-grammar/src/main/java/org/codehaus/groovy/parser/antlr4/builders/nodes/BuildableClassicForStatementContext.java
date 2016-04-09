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
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableClassicForStatementContext extends BuildableBaseNode {
    private GroovyParser.ClassicForStatementContext ctx;

    public BuildableClassicForStatementContext(ASTBuilder astBuilder, GroovyParser.ClassicForStatementContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Statement build() {

        ClosureListExpression expression = new ClosureListExpression();

        Boolean captureNext = false;
        for (ParseTree c : ctx.children) {
            // FIXME terrible logic.
            Boolean isSemicolon = c instanceof TerminalNode && (((TerminalNode)c).getSymbol().getText().equals(";") || ((TerminalNode)c).getSymbol().getText().equals("(") || ((TerminalNode)c).getSymbol().getText().equals(")"));
            if (captureNext && isSemicolon) expression.addExpression(EmptyExpression.INSTANCE);
            else if (captureNext && c instanceof GroovyParser.ExpressionContext)
                expression.addExpression(astBuilder.parseExpression((GroovyParser.ExpressionContext)c));
            captureNext = isSemicolon;
        }


        Parameter parameter = ForStatement.FOR_LOOP_DUMMY;
        return astBuilder.setupNodeLocation(new ForStatement(parameter, expression, astBuilder.parse(ctx.statementBlock())), ctx);
    }
}
