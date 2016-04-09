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
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableUnaryExpressionContext extends BuildableBaseNode {
    private GroovyParser.UnaryExpressionContext ctx;

    public BuildableUnaryExpressionContext(ASTBuilder astBuilder, GroovyParser.UnaryExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        Object node = null;
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class);
        if (DefaultGroovyMethods.isCase("-", op.getText())) {
            node = unaryMinusExpression(ctx.expression());
        } else if (DefaultGroovyMethods.isCase("+", op.getText())) {
            node = unaryPlusExpression(ctx.expression());
        } else if (DefaultGroovyMethods.isCase("!", op.getText())) {
            node = new NotExpression(astBuilder.parseExpression(ctx.expression()));
        } else if (DefaultGroovyMethods.isCase("~", op.getText())) {
            node = new BitwiseNegationExpression(astBuilder.parseExpression(ctx.expression()));
        } else {
            assert false : "There is no " + op.getText() + " handler.";
        }

        ((Expression)node).setColumnNumber(op.getSymbol().getCharPositionInLine() + 1);
        ((Expression)node).setLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastColumnNumber(op.getSymbol().getCharPositionInLine() + 1 + op.getText().length());
        return ((Expression)(node));
    }

    private Expression unaryMinusExpression(GroovyParser.ExpressionContext ctx) {
        // if we are a number literal then let's just parse it
        // as the negation operator on MIN_INT causes rounding to a long
        if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext) {
            return astBuilder.parseDecimal('-' + ((GroovyParser.ConstantDecimalExpressionContext)ctx).DECIMAL().getText(), ctx);
        } else if (ctx instanceof GroovyParser.ConstantIntegerExpressionContext) {
            return astBuilder.parseInteger('-' + ((GroovyParser.ConstantIntegerExpressionContext)ctx).INTEGER().getText(), ctx);
        } else {
            return new UnaryMinusExpression(astBuilder.parseExpression(ctx));
        }
    }


    private Expression unaryPlusExpression(GroovyParser.ExpressionContext ctx) {
        if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext || ctx instanceof GroovyParser.ConstantIntegerExpressionContext) {
            return astBuilder.parseExpression(ctx);
        } else {
            return new UnaryPlusExpression(astBuilder.parseExpression(ctx));
        }
    }
}
