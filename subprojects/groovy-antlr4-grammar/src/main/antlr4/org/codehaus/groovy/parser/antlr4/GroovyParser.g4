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
parser grammar GroovyParser;

options { tokenVocab = GroovyLexer; }

@header {
    import java.util.Arrays;
    import java.util.Set;
    import java.util.HashSet;
}

@members {
    private String currentClassName = null; // Used for correct constructor recognition.

    private boolean declarationRuleInExpressionEnabled = false;

    private boolean isDeclarationRuleInExpressionEnabled() {
        return declarationRuleInExpressionEnabled;
    }
    private void enableDeclarationRuleInExpression() {
        declarationRuleInExpressionEnabled = true;
    }
    private void disableDeclarationRuleInExpression() {
        declarationRuleInExpressionEnabled = false;
    }

    private boolean ellipsisEnabled = false;

    private boolean isEllipsisEnabled() {
        return ellipsisEnabled;
    }
    private void enableEllipsis() {
        ellipsisEnabled = true;
    }
    private void disableEllipsis() {
        ellipsisEnabled = false;
    }

    private static String createErrorMessageForStrictCheck(Set<String> s, String keyword) {
        if (VISIBILITY_MODIFIER_SET.contains(keyword)) {
            StringBuilder sb = new StringBuilder();
            for (String m : s) {
                if (VISIBILITY_MODIFIER_SET.contains(m)) {
                    sb.append(m + ", ");
                }
            }

            return sb.append(keyword) + " are not allowed to duplicate or define in the same time.";
        } else {
            return "duplicated " + keyword + " is not allowed.";
        }
    }

    private static final Set<String> VISIBILITY_MODIFIER_SET = new HashSet<String>(Arrays.asList("public", "protected", "private"));
    private static final String VISIBILITY_MODIFIER_STR = "VISIBILITY_MODIFIER";
    private static void collectModifier(Set<String> s, String modifier) {
        s.add(modifier);
    }
    private static boolean checkModifierDuplication(Set<String> s, String modifier) {
        if (VISIBILITY_MODIFIER_SET.contains(modifier)) {
            modifier = VISIBILITY_MODIFIER_STR;

            for (String m : s) {
                m = VISIBILITY_MODIFIER_SET.contains(m) ? VISIBILITY_MODIFIER_STR : m;

                if (m.equals(modifier)) {
                    return true;
                }
            }

            return false;
        } else {
            return s.contains(modifier);
        }
    }

}

compilationUnit: SHEBANG_COMMENT? (NL*)
                 packageDefinition? (NL | SEMICOLON)*
                 (importStatement (NL | SEMICOLON) | scriptPart (NL | SEMICOLON) | classDeclaration | (NL | SEMICOLON))* (NL | SEMICOLON)*
                 (scriptPart)? (NL | SEMICOLON)*
                 EOF;

scriptPart:
    methodDeclaration
    | statement
;

packageDefinition:
    (annotationClause (NL | annotationClause)*)? KW_PACKAGE (IDENTIFIER (DOT IDENTIFIER)*);
importStatement:
    (annotationClause (NL | annotationClause)*)? KW_IMPORT KW_STATIC? (IDENTIFIER (DOT IDENTIFIER)* (DOT MULT)?) (KW_AS IDENTIFIER)?;

classDeclaration
locals [Set<String> modifierSet = new HashSet<String>(), boolean isEnum=false]
:
    (
        (     annotationClause | classModifier {!checkModifierDuplication($modifierSet, $classModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierSet, $classModifier.text)}> {collectModifier($modifierSet, $classModifier.text);})
        (NL | annotationClause | classModifier {!checkModifierDuplication($modifierSet, $classModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierSet, $classModifier.text)}> {collectModifier($modifierSet, $classModifier.text);})*
    )? (AT KW_INTERFACE | KW_CLASS | KW_INTERFACE | KW_TRAIT | KW_ENUM {$isEnum=true;}) IDENTIFIER { currentClassName = $IDENTIFIER.text; }
    ({!$isEnum}? genericDeclarationList? extendsClause?
    |
    )
    implementsClause? (NL)*
    classBody[$isEnum];

classMember:
    constructorDeclaration | methodDeclaration | fieldDeclaration | objectInitializer | classInitializer | classDeclaration ;

enumConstant: IDENTIFIER (LPAREN argumentList RPAREN)?;

classBody[boolean isEnum]
    : LCURVE NL*
      ({$isEnum}? (enumConstant NL* COMMA NL*)* enumConstant NL* COMMA?
      |
      )
      (classMember | NL | SEMICOLON)*
      RCURVE;

implementsClause:  KW_IMPLEMENTS genericClassNameExpression (COMMA genericClassNameExpression)* ;
extendsClause:  KW_EXTENDS genericClassNameExpression ;

// Members
methodDeclaration
locals [Set<String> modifierAndDefSet = new HashSet<String>()]
:
    (
        (memberModifier {!checkModifierDuplication($modifierAndDefSet, $memberModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $memberModifier.text)}> {collectModifier($modifierAndDefSet, $memberModifier.text);} | annotationClause | KW_DEF {!$modifierAndDefSet.contains($KW_DEF.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $KW_DEF.text)}> {$modifierAndDefSet.add($KW_DEF.text);})
        (memberModifier {!checkModifierDuplication($modifierAndDefSet, $memberModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $memberModifier.text)}> {collectModifier($modifierAndDefSet, $memberModifier.text);} | annotationClause | KW_DEF {!$modifierAndDefSet.contains($KW_DEF.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $KW_DEF.text)}> {$modifierAndDefSet.add($KW_DEF.text);} | NL)* (
            (genericDeclarationList genericClassNameExpression) | typeDeclaration
        )?
    |
        genericClassNameExpression
    )
    (IDENTIFIER | STRING) LPAREN argumentDeclarationList RPAREN throwsClause? (KW_DEFAULT annotationParameter | methodBody)?
;

methodBody:
    LCURVE blockStatement? RCURVE
;

fieldDeclaration
locals [Set<String> modifierAndDefSet = new HashSet<String>()]
:
    (
        (memberModifier {!checkModifierDuplication($modifierAndDefSet, $memberModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $memberModifier.text)}> {collectModifier($modifierAndDefSet, $memberModifier.text);} | annotationClause | KW_DEF {!$modifierAndDefSet.contains($KW_DEF.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $KW_DEF.text)}> {$modifierAndDefSet.add($KW_DEF.text);})
        (memberModifier {!checkModifierDuplication($modifierAndDefSet, $memberModifier.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $memberModifier.text)}> {collectModifier($modifierAndDefSet, $memberModifier.text);} | annotationClause | KW_DEF {!$modifierAndDefSet.contains($KW_DEF.text)}?<fail={createErrorMessageForStrictCheck($modifierAndDefSet, $KW_DEF.text)}> {$modifierAndDefSet.add($KW_DEF.text);} | NL)* genericClassNameExpression?
        | genericClassNameExpression)
    singleDeclaration ( COMMA singleDeclaration)*
;
constructorDeclaration: { GrammarPredicates.isCurrentClassName(_input, currentClassName) }?
    VISIBILITY_MODIFIER? IDENTIFIER LPAREN argumentDeclarationList RPAREN throwsClause? LCURVE blockStatement? RCURVE ; // Inner NL 's handling.
objectInitializer: LCURVE blockStatement? RCURVE ;
classInitializer: KW_STATIC LCURVE blockStatement? RCURVE ;

typeDeclaration:
    (genericClassNameExpression | KW_DEF)
;

annotationClause: //FIXME handle assignment expression.
    AT genericClassNameExpression ( LPAREN ((annotationElementPair (COMMA annotationElementPair)*) | annotationElement)? RPAREN )?
;
annotationElementPair: IDENTIFIER ASSIGN NL* annotationElement ;
annotationElement: annotationParameter | annotationClause ;

genericDeclarationList:
    LT genericsDeclarationElement (COMMA genericsDeclarationElement)* GT
;

genericsDeclarationElement: genericClassNameExpression (KW_EXTENDS genericClassNameExpression (BAND genericClassNameExpression)* )? ;

throwsClause: KW_THROWS classNameExpression (COMMA classNameExpression)*;

argumentDeclarationList:
     (argumentDeclaration COMMA NL* )* { enableEllipsis(); } argumentDeclaration { disableEllipsis(); } | /* EMPTY ARGUMENT LIST */
     ;

argumentDeclaration:
    annotationClause* typeDeclaration? IDENTIFIER (ASSIGN NL* expression)? ;

blockStatement:
    (NL | SEMICOLON)+ (statement (NL | SEMICOLON)+)* statement? (NL | SEMICOLON)*
    | statement ((NL | SEMICOLON)+ statement)* (NL | SEMICOLON)*;

declarationRule: annotationClause* KW_FINAL? ( typeDeclaration singleDeclaration ( COMMA singleDeclaration)*
                                             | KW_DEF tupleDeclaration
                                             );
singleDeclaration: IDENTIFIER (ASSIGN NL* expression)?;
tupleDeclaration: LPAREN tupleVariableDeclaration (COMMA tupleVariableDeclaration)* RPAREN (ASSIGN NL* expression)?;
tupleVariableDeclaration: genericClassNameExpression? IDENTIFIER;
newInstanceRule: KW_NEW (classNameExpression (LT GT)? | genericClassNameExpression) (LPAREN argumentList? RPAREN) (classBody[false])?;
newArrayRule: KW_NEW classNameExpression (LBRACK INTEGER RBRACK)* ;

statement:
      declarationRule #declarationStatement
    | newArrayRule #newArrayStatement
    | newInstanceRule #newInstanceStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} (expression)? {disableDeclarationRuleInExpression();} SEMICOLON expression? SEMICOLON expression? RPAREN NL* statementBlock #classicForStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} typeDeclaration? IDENTIFIER {disableDeclarationRuleInExpression();} KW_IN expression RPAREN NL* statementBlock #forInStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} typeDeclaration  IDENTIFIER {disableDeclarationRuleInExpression();} COLON expression RPAREN NL* statementBlock #forColonStatement
    | KW_IF LPAREN expression RPAREN NL* statementBlock NL* (KW_ELSE NL* statementBlock)? #ifStatement
    | KW_WHILE LPAREN expression RPAREN NL* statementBlock #whileStatement
    | KW_SWITCH LPAREN expression RPAREN NL* LCURVE
        (
          (caseStatement | NL)*
          (KW_DEFAULT COLON (statement (SEMICOLON | NL) | SEMICOLON | NL)+)?
        )
      RCURVE #switchStatement
    |  tryBlock ((catchBlock+ finallyBlock?) | finallyBlock) #tryCatchFinallyStatement
    | (KW_CONTINUE | KW_BREAK) #controlStatement
    | KW_RETURN expression? #returnStatement
    | KW_THROW expression #throwStatement
    | KW_ASSERT expression ((COLON|COMMA) NL* expression)? #assertStatement
    | KW_SYNCHRONIZED LPAREN expression RPAREN NL* statementBlock # synchronizedStatement
    | cmdExpressionRule #commandExpressionStatement
    | expression #expressionStatement
    | IDENTIFIER COLON NL* statementBlock #labeledStatement
    ;

statementBlock:
    LCURVE blockStatement? RCURVE
    | statement ;

tryBlock: KW_TRY NL* LCURVE blockStatement? RCURVE NL*;
catchBlock: KW_CATCH NL* LPAREN ((classNameExpression (BOR classNameExpression)* IDENTIFIER) | IDENTIFIER) RPAREN NL* LCURVE blockStatement? RCURVE NL*;
finallyBlock: KW_FINALLY NL* LCURVE blockStatement? RCURVE;

caseStatement: (KW_CASE expression COLON (statement (SEMICOLON | NL) | SEMICOLON | NL)* );

cmdExpressionRule: (expression op=(DOT | SAFE_DOT | STAR_DOT))? IDENTIFIER ( argumentList IDENTIFIER)+ argumentList prop=IDENTIFIER? ;
pathExpression: (IDENTIFIER DOT)* IDENTIFIER ;
gstringPathExpression: IDENTIFIER (GSTRING_PATH_PART)* ;

closureExpressionRule: LCURVE NL* (argumentDeclarationList NL* CLOSURE_ARG_SEPARATOR NL*)? blockStatement? RCURVE ;
gstringExpressionBody:( gstringPathExpression
                      | LCURVE expression? RCURVE
                      | closureExpressionRule
                      );
gstring:  GSTRING_START gstringExpressionBody (GSTRING_PART  gstringExpressionBody)* GSTRING_END ;

// Special cases.
// 1. Command expression(parenthesis-less expressions)
// 2. Annotation paramenthers.. (inline constant)
// 3. Constant expressions.
// 4. class ones, for instanceof and as (type specifier)

annotationParameter:
    LBRACK (annotationParameter (COMMA annotationParameter)*)? RBRACK #annotationParamArrayExpression
    | pathExpression #annotationParamPathExpression //class, enum or constant field
    | genericClassNameExpression #annotationParamClassExpression //class
    | STRING #annotationParamStringExpression //primitive
    | DECIMAL #annotationParamDecimalExpression //primitive
    | INTEGER #annotationParamIntegerExpression //primitive
    | KW_NULL #annotationParamNullExpression //primitive
    | (KW_TRUE | KW_FALSE) #annotationParamBoolExpression //primitive
    | closureExpressionRule # annotationParamClosureExpression
;

expression:
      STRING  #constantExpression
    | gstring #gstringExpression
    | DECIMAL #constantDecimalExpression
    | INTEGER #constantIntegerExpression
    | KW_NULL #nullExpression
    | KW_THIS # thisExpression
    | KW_SUPER # superExpression
    | (KW_TRUE | KW_FALSE) #boolExpression
    | IDENTIFIER #variableExpression
    | newArrayRule #newArrayExpression
    | newInstanceRule #newInstanceExpression
    | closureExpressionRule #closureExpression
    | {isDeclarationRuleInExpressionEnabled()}?  declarationRule #declarationExpression
    | LBRACK NL* (expression (NL* COMMA NL* expression NL*)* COMMA?)?  NL* RBRACK #listConstructor
    | LBRACK NL* (COLON NL*| (mapEntry (NL* COMMA NL* mapEntry NL*)*) COMMA?) NL* RBRACK #mapConstructor
    | KW_SUPER LPAREN argumentList? RPAREN  #constructorCallExpression
    | expression NL* op=(DOT | SAFE_DOT | STAR_DOT | ATTR_DOT | MEMBER_POINTER) (selectorName | STRING | gstring) #fieldAccessExpression
    | LPAREN expression RPAREN #parenthesisExpression
    | MULT expression #spreadExpression
    | expression (DECREMENT | INCREMENT)  #postfixExpression

    | (PLUS | MINUS) expression #unaryExpression
    | (DECREMENT | INCREMENT) expression #prefixExpression
    | expression LBRACK (expression (COMMA expression)*)? RBRACK #indexExpression

    // exclude this and super to support this(...) and super(...) in the contructor
    | { !GrammarPredicates.isKeyword(_input, KW_THIS, KW_SUPER) }?      callExpressionRule       #callExpression
    | expression NL* op=(DOT | SAFE_DOT | STAR_DOT)  callExpressionRule       #callExpression

    | LPAREN genericClassNameExpression RPAREN expression #castExpression

    | (NOT | BNOT) expression #unaryExpression
    | expression POWER NL* expression #binaryExpression
    | expression (MULT | DIV | MOD) NL* expression #binaryExpression
    | expression (PLUS | MINUS) NL* expression #binaryExpression

    | expression (LSHIFT | GT GT | GT GT GT) NL* expression #binaryExpression
    | expression (RANGE | ORANGE) NL* expression #binaryExpression
    | expression KW_IN NL* expression #binaryExpression
    | expression KW_AS NL* genericClassNameExpression #binaryExpression
    | expression KW_INSTANCEOF NL* genericClassNameExpression #binaryExpression

    | expression SPACESHIP NL* expression #binaryExpression
    | expression GT NL* expression #binaryExpression
    | expression GTE NL* expression #binaryExpression
    | expression LT NL* expression #binaryExpression
    | expression LTE NL* expression #binaryExpression
    | expression EQUAL NL* expression #binaryExpression
    | expression UNEQUAL NL* expression #binaryExpression
    | expression FIND NL* expression #binaryExpression
    | expression MATCH NL* expression #binaryExpression
    | expression BAND NL* expression #binaryExpression
    |<assoc=right> expression XOR NL* expression #binaryExpression
    | expression BOR NL* expression #binaryExpression
    | expression NL* AND NL* expression #binaryExpression
    | expression NL* OR NL* expression #binaryExpression
    |<assoc=right> expression NL* QUESTION NL* expression NL* COLON NL* expression #ternaryExpression
    | expression NL* ELVIS NL* expression #elvisExpression

    |<assoc=right> expression (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | MULT_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | BAND_ASSIGN | XOR_ASSIGN | BOR_ASSIGN | LSHIFT_ASSIGN | RSHIFT_ASSIGN | RUSHIFT_ASSIGN) NL* expression #assignmentExpression
    |<assoc=right> LPAREN IDENTIFIER (COMMA IDENTIFIER)* RPAREN ASSIGN NL* expression #assignmentExpression
;

callExpressionRule: (selectorName | STRING | gstring | c=closureExpressionRule) LPAREN argumentList? RPAREN closureExpressionRule*
                  | { !GrammarPredicates.isFollowedByLPAREN(_input) }? (selectorName | STRING | gstring | c=closureExpressionRule) argumentList
                  ;

classNameExpression: { GrammarPredicates.isClassName(_input) }? (BUILT_IN_TYPE | IDENTIFIER (DOT IDENTIFIER)*) ;

genericClassNameExpression: classNameExpression genericList? (LBRACK RBRACK)? (ELLIPSIS { isEllipsisEnabled() }?<fail={ "The var-arg only be allowed to appear as the last parameter" }>)?;

genericList:
    LT genericListElement (COMMA genericListElement)* GT
;

genericListElement:
    genericClassNameExpression #genericsConcreteElement
    | QUESTION (KW_EXTENDS genericClassNameExpression | KW_SUPER genericClassNameExpression)? #genericsWildcardElement
;

mapEntry:
    STRING COLON expression
    | gstring COLON expression
    | selectorName COLON expression
    | LPAREN expression RPAREN COLON expression
    | MULT COLON expression
    | DECIMAL COLON expression
    | INTEGER COLON expression
;

classModifier:
VISIBILITY_MODIFIER | KW_STATIC | (KW_ABSTRACT | KW_FINAL) | KW_STRICTFP ;

memberModifier:
    VISIBILITY_MODIFIER | KW_STATIC | (KW_ABSTRACT | KW_FINAL) | KW_NATIVE | KW_SYNCHRONIZED | KW_TRANSIENT | KW_VOLATILE ;

argumentList: ( (closureExpressionRule)+ | argument (NL* COMMA NL* argument)*) ;

argument
: mapEntry
        | expression
        ;

selectorName
        : IDENTIFIER
        | kwSelectorName
        ;

kwSelectorName: KW_ABSTRACT | KW_AS | KW_ASSERT | KW_BREAK | KW_CASE | KW_CATCH | KW_CLASS | KW_CONST | KW_CONTINUE
                     | KW_DEF | KW_DEFAULT | KW_DO | KW_ELSE | KW_ENUM | KW_EXTENDS | KW_FALSE | KW_FINAL | KW_FINALLY
                     | KW_FOR | KW_GOTO | KW_IF | KW_IMPLEMENTS | KW_IMPORT | KW_IN | KW_INSTANCEOF | KW_INTERFACE
                     | KW_NATIVE | KW_NEW | KW_NULL | KW_PACKAGE
                     | KW_RETURN | KW_STATIC | KW_STRICTFP | KW_SUPER | KW_SWITCH | KW_SYNCHRONIZED | KW_THIS | KW_THREADSAFE | KW_THROW
                     | KW_THROWS | KW_TRANSIENT | KW_TRUE | KW_TRY | KW_VOLATILE | KW_WHILE
                     | BUILT_IN_TYPE | VISIBILITY_MODIFIER /* in place of KW_PRIVATE | KW_PROTECTED | KW_PUBLIC */
;