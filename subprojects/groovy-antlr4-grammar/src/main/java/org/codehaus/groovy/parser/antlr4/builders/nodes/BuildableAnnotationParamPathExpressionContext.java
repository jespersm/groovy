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

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by Daniel on 2016/4/10.
 */
public class BuildableAnnotationParamPathExpressionContext extends BuildableBaseNode {
    private GroovyParser.AnnotationParamPathExpressionContext ctx;

    public BuildableAnnotationParamPathExpressionContext(ASTBuilder astBuilder, GroovyParser.AnnotationParamPathExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        GroovyParser.AnnotationParamPathExpressionContext c = DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamPathExpressionContext.class);
        return astBuilder.collectPathExpression(c.pathExpression());

    }
}
