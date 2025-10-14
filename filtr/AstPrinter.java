package filtr;

class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }
    
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme,
        expr.left, expr.right);
    }
    
    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }
    
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }
    
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize("var", expr);
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize("get", expr.object);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(call ");
        builder.append(expr.callee.accept(this));
        for (Expr argument : expr.arguments) {
            builder.append(" ");
            builder.append(argument.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize("set " + expr.name.lexeme, expr.object, expr.value);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        
        return builder.toString();
    }
    
    
    public static void main(String[] args) {
        Token plus = new Token(TokenType.PLUS, "+", null, 1);
    Token star = new Token(TokenType.STAR, "*", null, 1);

    // Build expression: (18 + 2) * 3
    Expr expression = new Expr.Binary(
        new Expr.Binary(
            new Expr.Literal(18),
            plus,
            new Expr.Literal(2)
        ),
        star,
        new Expr.Literal(3)
    );

    System.out.println(new AstPrinter().print(expression));
    

    Token dataset = new Token(TokenType.IDENTIFIER, "customers", null, 1);
    Token column = new Token(TokenType.IDENTIFIER, "age", null, 1);

    //Stmt addColumn = new Stmt.AddColumn(dataset, column, expression);

    System.out.println("Add column statement test:");
    System.out.println("Dataset: " + dataset.lexeme);
    System.out.println("Column: " + column.lexeme);
    System.out.println("Expression: " + new AstPrinter().print(expression));
    //System.out.println(new AstPrinter().print(addColumn));

    }
    
}