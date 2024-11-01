/**
 * The prior version, located within "LexerTestCaseFoo.java" included
 * type declarations for each global.  This lexed successfully as shown
 * in that file.
 * 
 * However, if you examine the Parser closely, you will see that we are
 * not yet working with types in our grammar (we will soon, but just not yet).
 * After types are introduced into our language, the complete "LexerTestCaseFoo.java"
 * will be functional.
 * 
 * Therefore, the example has been edited to parse using the grammar
 * as it is given at this point.  Notice, including types at this time
 * is a good example of where a ParserException is generated.  
 * Based upon our current grammar, the source would not be recognized.
 */
 
/*
<==== Test Case Foo ====>


The source or "input" for the example:

VAR i = -1;
VAL inc = 2;
FUN foo() DO
    WHILE i != 1 DO
        IF i > 0 DO
            print(\"bar\");
        END
        i = i + inc;
    END
END

<==== Character Index Positions ====>


for each line of our source in this example,
we label every index position
note:
 (a) the last char on each line is a newline
 (b) the index skip that occurs for escapes



0         1
012345678901                  =>   0  -to-   11
VAR i = -1;

        2
2345678901234                 =>  12  -to-   24
VAL inc = 2;

     3
5678901234567                 =>  25  -to-   37
FUN foo() DO

  4         5
89012345678901234567          =>  38  -to-   57
    WHILE i != 1 DO

  6         7
89012345678901234567          =>  58  -to-   77
        IF i > 0 DO

                        1
  8         9           0
890123456789012345 6789 0123  =>  78  -to-  103
            print(\"bar\");

      1
      1
456789012345                  => 104  -to-  115
        END

    1         1
    2         3
678901234567890123456         => 116  -to-  136
        i = i + inc;

   1
   4
78901234                      => 137  -to-  144
    END

567                           => 145  -to-  147
END


// source String represenation of the test case
String source = new String("VAR i = -1;\nVAL inc = 2;\nFUN foo() DO\n    WHILE i != 1 DO\n        IF i > 0 DO\n            print(\"bar\");\n        END\n        i = i + inc;\n    END\nEND");


<==== Tokens Generated by the Lexer ====>

Note, there are two formats that follow
 (1) Token list generated by using System.out.println(tokens);
     generating the list of tokens using the toString representaion
     of each token.
     
 (2) The "Arrays.list(" representation you could use to form a test case.


<==== (1) ====>

[IDENTIFIER=VAR@0, IDENTIFIER=i@4, OPERATOR==@6, INTEGER=-1@8, OPERATOR=;@10, IDENTIFIER=VAL@12, IDENTIFIER=inc@16, OPERATOR==@20, INTEGER=2@22, OPERATOR=;@23, IDENTIFIER=FUN@25, IDENTIFIER=foo@29, OPERATOR=(@32, OPERATOR=)@33, IDENTIFIER=DO@35, IDENTIFIER=WHILE@42, IDENTIFIER=i@48, OPERATOR=!=@50, INTEGER=1@53, IDENTIFIER=DO@55, IDENTIFIER=IF@66, IDENTIFIER=i@69, OPERATOR=>@71, INTEGER=0@73, IDENTIFIER=DO@75, IDENTIFIER=print@90, OPERATOR=(@95, STRING="bar"@96, OPERATOR=)@101, OPERATOR=;@102, IDENTIFIER=END@112, IDENTIFIER=i@124, OPERATOR==@126, IDENTIFIER=i@128, OPERATOR=+@130, IDENTIFIER=inc@132, OPERATOR=;@135, IDENTIFIER=END@141, IDENTIFIER=END@145]


<==== (2) ====>

*/





/*
<==== The AST Built from the List of Tokens ====>

Formatted using toString for readability first 
with constructor calls building the actual tree following below.

Ast.Source {
	globals = [ 
		Ast.Global { name = 'i', mutable = true, value = Optional[Ast.Expression.Literal{literal=-1}] }
		Ast.Global { name = 'inc', mutable = false, value = Optional[Ast.Expression.Literal{literal=2}] }
	]
 
	functions = [
		Ast.Function {
			name = 'foo', 
			parameters = [], 
			statements = [
				Ast.Statement.While {
					condition = Ast.Expression.Binary { 
						operator = '!=', 
						left = Ast.Expression.Access{receiver=Optional.empty, name='i'}, 
						right = Ast.Expression.Literal{literal=1}
					},
					statements = [
					    Ast.Statement.If { 
							condition = Ast.Expression.Binary { 
								operator = '>', 
								left = Ast.Expression.Access{receiver=Optional.empty, name='i'}, 
								right = Ast.Expression.Literal{literal=0}
							},
							thenStatements = [
								Ast.Statement.Expression { 
									expression = Ast.Expression.Function {
										name = 'print', 
										arguments = [ Ast.Expression.Literal{literal=bar} ]
									}
								}
							], 
							elseStatements=[]
						}, 
						Ast.Statement.Assignment { 
							receiver = Ast.Expression.Access{receiver=Optional.empty, name='i'}, 
							value = Ast.Expression.Binary { 
								operator = '+', 
								left = Ast.Expression.Access{receiver=Optional.empty, name='i'}, 
								right = Ast.Expression.Access{receiver=Optional.empty, name='inc'}
							}
						}
					]
				}
			]
		}
	]
}
*/

