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

import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.SpreadMapExpression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.InvalidSyntaxException;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;

import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableMapEntryContext extends BuildableBaseNode {
    private GroovyParser.MapEntryContext ctx;

    public BuildableMapEntryContext(ASTBuilder astBuilder, GroovyParser.MapEntryContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public MapEntryExpression build() {
        Expression keyExpr;
        Expression valueExpr;
        List<GroovyParser.ExpressionContext> expressions = ctx.expression();
        if (expressions.size() == 1) {
            valueExpr = astBuilder.parseExpression(expressions.get(0));
            if (asBoolean(ctx.MULT())) {
                // This is really a spread map entry.
                // This is an odd construct, SpreadMapExpression does not extend MapExpression, so we workaround
                keyExpr = astBuilder.setupNodeLocation(new SpreadMapExpression(valueExpr), ctx);
            } else {
                if (asBoolean(ctx.STRING())) {
                    keyExpr = new ConstantExpression(astBuilder.parseString(ctx.STRING()));
                } else if (asBoolean(ctx.selectorName())) {
                    keyExpr = new ConstantExpression(ctx.selectorName().getText());
                } else if (asBoolean(ctx.gstring())) {
                    keyExpr = astBuilder.parseExpression(ctx.gstring());
                } else if (asBoolean(ctx.INTEGER())) {
                    keyExpr = astBuilder.parseInteger(ctx.INTEGER().getText(), ctx);
                } else if (asBoolean(ctx.DECIMAL())) {
                    keyExpr = astBuilder.parseDecimal(ctx.DECIMAL().getText(), ctx);
                } else {
                    throw new CompilationFailedException(CompilePhase.PARSING.getPhaseNumber(), astBuilder.sourceUnit,
                                new InvalidSyntaxException("Unsupported map key type! " + String.valueOf(ctx), ctx));

                }
            }
        } else {
            keyExpr = astBuilder.parseExpression(expressions.get(0));
            valueExpr = astBuilder.parseExpression(expressions.get(1));
        }

        return astBuilder.setupNodeLocation(new MapEntryExpression(keyExpr, valueExpr), ctx);
    }
}
