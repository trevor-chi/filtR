package filtr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.Data;

import filtr.dataset.*;

import filtr.Expr.Call;
import filtr.Expr.Get;
import filtr.Expr.Set;
import filtr.Stmt.AddColumn;
import filtr.Stmt.Assign;
import filtr.Stmt.Drop;
import filtr.Stmt.Export;
import filtr.Stmt.Fill;
import filtr.Stmt.Filter;
import filtr.Stmt.For;
import filtr.Stmt.Function;
import filtr.Stmt.Import;
import filtr.Stmt.Range;
import filtr.Stmt.Rename;
import filtr.Stmt.Return;
import filtr.Stmt.Review;
import filtr.Stmt.View;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    
    private Environment environment = new Environment();
    
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        
        return evaluate(expr.right);
    }
    
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case BANG:
            return !isTruthy(right);
            case MINUS:
            checkNumberOperand(expr.operator, right);
            return -(double)right;
        }
        
        // Unreachable.
        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    
    private void checkNumberOperands(Token operator,
    Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }
    
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        
        return a.equals(b);
    }
    
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    
    void executeBlock(List<Stmt> statements,
    Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
    
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }
    
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }
    
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }
    
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case MINUS:
            checkNumberOperands(expr.operator, left, right);
            return (double)left - (double)right;
            case PLUS:
            if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
            } 
            
            if (left instanceof String && right instanceof String) {
                return (String)left + (String)right;
            }
            throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
            case GREATER:
            checkNumberOperands(expr.operator, left, right);
            
            return (double)left > (double)right;
            case GREATER_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double)left >= (double)right;
            case LESS:
            checkNumberOperands(expr.operator, left, right);
            return (double)left < (double)right;
            case LESS_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL: return isEqual(left, right);
            
            case SLASH:
            checkNumberOperands(expr.operator, left, right);
            return (double)left / (double)right;
            case STAR:
            checkNumberOperands(expr.operator, left, right);
            return (double)left * (double)right;
        }
        
        // Unreachable.
        return null;
    }
    
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            filtR.runtimeError(error);
        }
    }
    
    private String stringify(Object object) {
        if (object == null) return "nil";
        
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        
        return object.toString();
    }
    
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        FiltrFunction function = new FiltrFunction(stmt, environment,
                                            false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    
    @Override
    public Void visitForStmt(For stmt) {
        
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        
        String mode = stmt.mode.lexeme;

        switch (mode) {
            case "row":
                for (Map<String, Object> row : dataset.getRows()) {
                    Environment forEnv = new Environment(environment);
                    forEnv.define(stmt.name.lexeme, row);
                    executeBlock(((Stmt.Block)stmt.body).statements, forEnv);
                }
                break;
            case "column":
                for (String columnName : dataset.getColumns()) {
                    Environment forEnv = new Environment(environment);
                    forEnv.define(stmt.name.lexeme, columnName);
                    executeBlock(((Stmt.Block)stmt.body).statements, forEnv);
                }
                break;
            default:
                throw new RuntimeError(stmt.mode, "Invalid for loop mode: " + mode);
        }

        return null;
    }
    
    @Override
    public Void visitDropStmt(Drop stmt) {
        // drops the dataset column name
        // firstly, gets the dataset from the environment
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        List<String> columnNames = new ArrayList<>();
        for (Token token : stmt.identifiers) {
            columnNames.add(token.lexeme);
        }
        try {
            dataset.dropColumn(columnNames);
        } catch (IllegalArgumentException e) {
            throw new RuntimeError(stmt.identifiers.get(0), e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public Void visitFillStmt(Fill stmt) {
        // fills the dataset column name's missing values with the given value
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        String columnName;

        try {
            // Check if the column name is a variable in the environment
            Object colObj = environment.get(stmt.column);
            columnName = colObj.toString();
        } catch (RuntimeError e) {
            // Otherwise, it's a literal column name in the dataset
            columnName = stmt.column.lexeme;
        }
        
        Object value = evaluate(stmt.value);

        String conditionColumn = stmt.conditionColumn != null ? stmt.conditionColumn.lexeme : null;
        String operator = stmt.operator != null ? stmt.operator.lexeme : null;
        Object expression = stmt.expression != null ? evaluate(stmt.expression) : null;

        // needs a try catch
        try {
            dataset.fillValues(columnName, value, conditionColumn, operator, expression, stmt.keyword.lexeme);
        } catch (IllegalArgumentException e) {
            throw new RuntimeError(stmt.keyword, e.getMessage());
        }
        return null;
    }
    
    @Override
    public Void visitRenameStmt(Rename stmt) {
        
        // renames the dataset column name
        // firstly, gets the dataset from the environment
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        // System.out.println(stmt.column);
        String oldName = stmt.column.lexeme;
        String newName = stmt.newName.lexeme.substring(1, stmt.newName.lexeme.length() - 1);
        try {
            dataset.renameColumn(oldName, newName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeError(stmt.column, e.getMessage());
        }
        
        return null;
    }
    
    @Override
public Void visitAddColumnStmt(AddColumn stmt) {
    Dataset dataset;
    try {
        dataset = (Dataset) environment.get(stmt.dataset);
    } catch (Exception e) {
        throw new RuntimeError(stmt.dataset, "not a dataset: " + stmt.dataset.lexeme);
    }
    String newCol = stmt.column.lexeme;
    List<Expr> values = stmt.value;

    // CASE 1: value is a binary expression like "test.Age > 22"
    if (values.size() == 1 && values.get(0) instanceof Expr.Binary bin) {
        // Left should be a column reference: test.Age
        if (!(bin.left instanceof Expr.Get)) {
            throw new RuntimeError(stmt.column, 
                "Left side of condition must be a column reference");
        }

        Expr.Get getExpr = (Expr.Get) bin.left;

        // dataset.column
        String baseColumn = getExpr.name.lexeme;

        // operator
        String operator = bin.operator.lexeme;

        // right value (could be literal or another column)
        Object rightValue = evaluate(bin.right);

        // Call the new addColumn overload
        try {
            dataset.addColumn(newCol, baseColumn, operator, rightValue);
        } catch (IllegalArgumentException e) {
            throw new RuntimeError(stmt.column, e.getMessage());
        }
        return null;
    }

    // now values is a list of expressions to be evaluated for each row
    List<Object> columnValues = new ArrayList<>();
    for (Expr valueExpr : values) {
        columnValues.add(evaluate(valueExpr));
    }
    try {
        dataset.addColumn(newCol, columnValues);
    } catch (IllegalArgumentException e) {
        throw new RuntimeError(stmt.column, e.getMessage());
    }
    return null;
}
    
    @Override
    public Void visitFilterStmt(Filter stmt) {
        /* first gets the dataset from the environment, then applies the filter
        then stores the result back in the environment. if the variable name already existed,
        then we override it. */
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        String columnName = stmt.columnName.lexeme;
        String operator = stmt.operator.lexeme;
        Object value = evaluate(stmt.expression);
        System.out.println("Filtering dataset " + stmt.dataset.lexeme + " on column " + columnName + " " + operator + " " + value);
        Dataset filteredDataset = dataset.filterDataset(columnName, operator, value);
        environment.define(stmt.newName.lexeme, filteredDataset);
        return null;
    }
    
    @Override
    public Void visitExportStmt(Export stmt) {
        Dataset dataset = (Dataset) environment.get(stmt.dataset);
        try {
            dataset.exportDataset(stmt.path.lexeme, stmt.dataset.lexeme, stmt.format.lexeme);
            System.out.println("Exported dataset to " + stmt.path.lexeme);
        } catch (IOException e) {
            throw new RuntimeError(stmt.path, "Failed to export dataset to path: " + stmt.path.lexeme + " ");
        }
        
        return null;
    }
    
    @Override
    public Void visitImportStmt(Import stmt) {
        String path = (String) stmt.path.literal;
        try {
            Dataset dataset = DatasetLoader.load(path);
            environment.define(stmt.newName.lexeme, dataset);
            System.out.println("Imported dataset: " + dataset);
            return null;
        } catch (IOException e) {
            throw new RuntimeError(stmt.path, "Failed to import dataset. " + e.getMessage());
        }
    }
    
    @Override
    public Void visitAssignStmt(Assign stmt) {
        Object value = evaluate(stmt.value);
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new filtr.Return(value);
    }
    
    @Override
    public Void visitViewStmt(View stmt) {
        try {
            Dataset dataset = (Dataset) environment.get(stmt.dataset);
            dataset.viewDataset();
            return null;
        } catch (Exception e) {
            throw new RuntimeError(stmt.dataset, "not a dataset: " + stmt.dataset.lexeme);
        }
    }
    
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { 
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof FiltrCallable)) {
            throw new RuntimeError(expr.paren,
                "Can only call functions and classes.");
        }

        FiltrCallable function = (FiltrCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }
    

    // check if we have a string, and if we do, convert to a token so we can do things like dataset.ColumnName
    @Override
    public Object visitGetExpr(Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof Dataset) {
            Dataset dataset = (Dataset) object;
            String columnName = expr.name.lexeme;
            
            if (!dataset.getColumns().contains(columnName)) {
                throw new RuntimeError(expr.name,
                "Dataset does not have column: " + columnName);
            }
            
            List<Object> columnValues = new ArrayList<>();
            for (Map<String, Object> row : dataset.getRows()) {
                columnValues.add(row.get(columnName));
            }
            
            return columnValues;
        }
        
        throw new RuntimeError(expr.name,
        "Only datasets have properties.");
    }
    
    @Override
    public Object visitSetExpr(Set expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSetExpr'");
    }

    @Override
    public Void visitReviewStmt(Review stmt) {
        try {
            Dataset dataset = (Dataset) environment.get(stmt.dataset);
            dataset.reviewDataset();
            return null;
        } catch (Exception e) {
            throw new RuntimeError(stmt.dataset, "not a dataset: " + stmt.dataset.lexeme);
        }
    }

    @Override
    public Void visitRangeStmt(Range stmt) {
        int start = Integer.parseInt(stmt.start.lexeme);
        int end = Integer.parseInt(stmt.end.lexeme);
        for (int i = start; i <= end; i++) {
            Environment rangeEnv = new Environment(environment);
            rangeEnv.define(stmt.name.lexeme, i);
            executeBlock(((Stmt.Block)stmt.body).statements, rangeEnv);
        }
        return null;
    }
    
}