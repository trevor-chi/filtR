```
program → declaration* EOF ;

declaration → functionDecl | datasetDecl | statement ;

statement → expressionStmt
           | assignmentStmt
           | dropStmt
           | fillStmt
           | renameStmt
           | addColumnStmt
           | filterStmt
           | controlStmt
           | exportStmt
           | returnStmt
           | printStmt
           | block ;

dropStmt       → "drop" ("missing" | "columns" IDENTIFIER_LIST) "from"? IDENTIFIER ;
fillStmt       → "fill" ("blanks" | "missing") "in" IDENTIFIER "." IDENTIFIER "with" expression ;
renameStmt     → "rename" IDENTIFIER "." IDENTIFIER "to" IDENTIFIER ;
addColumnStmt  → "add" "column" IDENTIFIER "." IDENTIFIER "=" expression ;
filterStmt     → "filter" IDENTIFIER "where" expression "as" IDENTIFIER; 
exportStmt     → ("export" | "save") IDENTIFIER "to" STRING ;
returnStmt     → "return" expression ;
printStmt      → "print" expression;
expressionStmt → expression ;
assignmentStmt → "set" IDENTIFIER "=" expression ;

controlStmt → forStmt | ifStmt ;

forStmt     → "for" "each" ("row" | "column") "in" IDENTIFIER block ;
ifStmt      → "if" expression block ( "else" "if" expression block )* ( "else" block )? ;

functionDecl → "function" IDENTIFIER "(" parameters? ")" block ;
parameters → IDENTIFIER ( "," IDENTIFIER )* ;

datasetDecl → ("use" | "import") STRING "as" IDENTIFIER ;

expression     → logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "==" | "!=" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "-" | "!" ) unary | primary ;
primary        → NUMBER | STRING | IDENTIFIER | callExpr | "(" expression ")" ;

callExpr → IDENTIFIER "(" arguments? ")" ;

arguments → expression ( "," expression )* ;
block → "{" declaration* "}" ;

literal → NUMBER | STRING | BOOLEAN | NULL | RANGE ;

RANGE   → NUMBER ".." NUMBER ;
```