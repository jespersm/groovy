package org.codehaus.groovy.parser.antlr4.builders.nodes;

import groovy.lang.Closure;
import org.antlr.v4.runtime.tree.ParseTree;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/10.
 */
public class BuildableArgumentListContext extends BuildableBaseNode {
    private GroovyParser.ArgumentListContext ctx;

    public BuildableArgumentListContext(ASTBuilder astBuilder, GroovyParser.ArgumentListContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        final List<MapEntryExpression> mapArgs = new ArrayList<MapEntryExpression>();
        final List<Expression> expressions = new ArrayList<Expression>();
        if (ctx != null) {
            DefaultGroovyMethods.each(ctx.children, new Closure<Collection<? extends Expression>>(null, null) {
                public Collection<? extends Expression> doCall(ParseTree it) {
                    if (it instanceof GroovyParser.ArgumentContext) {
                        if (asBoolean(((GroovyParser.ArgumentContext)it).mapEntry())) {
                            mapArgs.add(astBuilder.parseExpression(((GroovyParser.ArgumentContext) it).mapEntry()));
                            return mapArgs;
                        } else {
                            expressions.add(astBuilder.parseExpression(((GroovyParser.ArgumentContext) it).expression()));
                            return expressions;
                        }
                    } else if (it instanceof GroovyParser.ClosureExpressionRuleContext) {
                        expressions.add(astBuilder.parseExpression((GroovyParser.ClosureExpressionRuleContext) it));
                        return expressions;
                    }
                    return null;
                }
            });
        }
        if (asBoolean(expressions)) {
            if (asBoolean(mapArgs))
                expressions.add(0, new MapExpression(mapArgs));
            return new ArgumentListExpression(expressions);
        } else {
            if (asBoolean(mapArgs))
                return new TupleExpression(new NamedArgumentListExpression(mapArgs));
            else return new ArgumentListExpression();
        }

    }
}
