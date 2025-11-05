package filtr;

import java.util.ArrayList;
import java.util.List;
// import java.util.Arrays;

import static filtr.TokenType.*;

public class Parser {
  private final List<Token> tokens;
  private int current = 0;
  
  private static class ParseError extends RuntimeException {}
  
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    
    return statements; 
  }
  
  private Expr expression() {
    return assignment();
  }
  
  private Stmt declaration() {
    try {
      if (match(FUNCTION)) return functionDeclaration();
      if (match(USE) || match(IMPORT)) return datasetDeclaration();
      
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }
  
  private Stmt datasetDeclaration() {
    Token keyword = previous();
    Token path = consume(STRING, "Expect string as path to a dataset");
    consume(AS, "Expect 'as' after dataset path.");
    Token alias = consume(IDENTIFIER, "Expect dataset alias.");
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Import(keyword, path, alias);
  }
  
  private Stmt functionDeclaration() {
    Token name = consume(IDENTIFIER, "Expect function name.");
    consume(LEFT_PAREN, "Expect '(' after function name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }
        
        parameters.add(
        consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    
    consume(RIGHT_PAREN, "Expect ')' after parameters.");
    consume(LEFT_BRACE, "Expect '{' before function body."); 
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }
  
  private Stmt statement() {
    if (match(FOR)) return forStatement();
    if (match(SET)) return assignmentStatement();
    if (match(DROP)) return dropStatement();
    if (match(ADD)) return addStatement();
    if (match(EXPORT) || match(SAVE)) return exportStatement();
    if (match(VIEW)) return viewStatement();
    if (match(RENAME)) return renameStatement();
    if (match(FILL)) return fillStatement();
    if (match(FILTER)) return filterStatement();
    if (match(IF)) return ifStatement();
    if (match(PRINT)) return printStatement();
    if (match(RETURN)) return returnStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
    
    return expressionStatement();
  }

  private Stmt viewStatement() {
    Token datasetName = consume(IDENTIFIER, "Expect dataset name after 'view'");
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.View(datasetName);
  }
  
  private Stmt filterStatement() {
    Token name = consume(IDENTIFIER, "Expect column name after 'filter'");
    consume(WHERE, "Expect 'where' after column name");
    Token columnName = consume(IDENTIFIER, "Expect column name in filter condition");
    if (!match(BANG_EQUAL, EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      throw error(previous(), "Expect condition after 'where'");
    }
    Token operator = previous();
    Expr condition = expression();
    consume(AS, "Expect 'as' after filter condition");
    Token alias = consume(IDENTIFIER, "Expect alias name after 'as'");
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Filter(name, columnName, operator, condition, alias);
  }
  

  private Stmt renameStatement() {
    Token datasetName = consume(IDENTIFIER, "Expect datasetname after 'rename'");
    consume(DOT, "Expect '.' after dataset name");
    Token oldName = consume(IDENTIFIER, "Expect column name to rename");
    consume(TO, "Expect 'to' after column name");
    Token newName = consume(STRING, "Expect string after 'to'");
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Rename(datasetName, oldName, newName);
  }

  private Stmt fillStatement() {
    Token next = peek();
    if (!next.type.equals(BLANKS) && !next.type.equals(MISSING)) {
      throw error(next, "Expect 'blanks' or 'missing' after 'fill'");
    }

    Token keyword = advance();
    consume(IN, "Expect 'in' after 'blanks' or 'missing'");
    Token datasetName = consume(IDENTIFIER, "Expect dataset name after 'in'");
    consume(DOT, "Expect '.' after dataset name");
    Token columnName = consume(IDENTIFIER, "Expect column name after '.'");
    consume(WITH, "Expect 'with' after column name");
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    
    return new Stmt.Fill(keyword, columnName, datasetName, value);
  }

  private Stmt exportStatement() {
    Token keyword = previous();
    Token datasetName = consume(IDENTIFIER, "Expect dataset name after 'export' or 'save'.");
    consume(TO, "Expect 'to' after 'datasetname'.");
    Token path = consume(STRING, "Expect valid path to save the dataset");
    consume(AS, "Expect 'as' after path.");
    if (!match(CSV, JSON)) {
      throw error(peek(), "Expect 'csv' or 'json' after 'as'.");
    }
    Token format = previous();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Export(keyword, datasetName, path, format);
  }

  private Stmt dropStatement() {
  Token keyword = consume(COLUMNS, "Expect 'columns' after 'drop'");
  List<Token> columns = new ArrayList<>();
  do {
    columns.add(consume(IDENTIFIER, "Expect column name"));
  } while (match(COMMA));
  consume(FROM, "Expect 'from' after column names");
  Token datasetName = consume(IDENTIFIER, "Expect dataset name after 'from'");
  consume(SEMICOLON, "Expect ';' after value.");
  return new Stmt.Drop(keyword, columns, datasetName);
  }

  private Stmt addStatement() {
    consume(COLUMN, "Expect 'column' after 'add'");
    Token columnName = consume(IDENTIFIER, "Expect column name after 'add'");
    consume(DOT, "Expect '.' after column name");
    Token fieldName = consume(IDENTIFIER, "Expect field name after '.'");
    consume(EQUAL, "Expect '=' after field name");
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.AddColumn(columnName, fieldName, value);
  }

  private Stmt assignmentStatement() {
    Token name = consume(IDENTIFIER, "Expect variable name after 'set'");
    consume(EQUAL, "Expect '=' after variable name");
    Expr value = expression();
    return new Stmt.Assign(name, value);
  }
  
  //forStmt     → "for" "each" ("row" | "column") "in" IDENTIFIER block ;
  private Stmt forStatement() {

    consume(EACH, "Expect 'each' after 'for'.");
    if (!match(ROW, COLUMN)) {
      throw error(peek(), "Expect 'row' or 'column' after 'for each'.");
    }
    Token mode = previous();
    consume(IN, "Expect 'in' after 'row' or 'column'.");
    Token dataset = consume(IDENTIFIER, "Expect dataset name after 'in'.");
    consume(LEFT_BRACE, "Expect '{' before for loop block.");
    return new Stmt.For(mode, dataset, new Stmt.Block(block()));
  }
  
  private Stmt ifStatement() {
    Expr condition = expression();
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }
    
    return new Stmt.If(condition, thenBranch, elseBranch);
  }
  
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
  
  private Stmt returnStatement() {
    //Token keyword = previous();
    
    Expr value = expression();
    
    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(value);
  }
  
  
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }
  
  private List<Stmt> block() {
    // consume(LEFT_BRACE, "Expect '{' at start of block.");
    List<Stmt> statements = new ArrayList<>();
    
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }
  
  private Expr assignment() {
    Expr expr = or();
    
    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      
      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
      }
      
      error(equals, "Invalid assignment target."); 
    }

    
    return expr;
  }
  
  private Expr or() {
    Expr expr = and();
    
    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr and() {
    Expr expr = equality();
    
    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr equality() {
    Expr expr = comparison();
    
    while (match(BANG_EQUAL, EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr comparison() {
    Expr expr = term();
    
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr term() {
    Expr expr = factor();
    
    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr factor() {
    Expr expr = unary();
    
    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    
    return call();
  }
  
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }
    
    Token paren = consume(RIGHT_PAREN,
    "Expect ')' after arguments.");
    
    return new Expr.Call(callee, paren, arguments);
  }
  
  private Expr call() {
    Expr expr = primary();
    
    while (true) { 
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER,
        "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }
    
    return expr;
  }
  
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    
    throw error(peek(), "Expect expression.");
  }
  
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    
    return false;
  }
  
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    
    throw error(peek(), message);
  }
  
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
  
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
  
  private boolean isAtEnd() {
    return peek().type == EOF;
  }
  
  private Token peek() {
    return tokens.get(current);
  }
  
  private Token previous() {
    return tokens.get(current - 1);
  }
  
  private ParseError error(Token token, String message) {
    filtR.error(token.line, message);
    return new ParseError();
  }
  
  private void synchronize() {
    advance();
    
    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;
      
      switch (peek().type) {
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
        return;
        default:
        break;
      }
      
      advance();
    }
  }
  
}
