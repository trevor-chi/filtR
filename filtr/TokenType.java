package filtr;

enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,       // ( ) { }
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,        // , . - + ; / *

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUNCTION,
    FOR, IF, OR, PRINT,
    RETURN, TRUE, WHILE, NOT,

    // specific to our language
    IMPORT, EXPORT, USE, AS, SAVE, VIEW,
    ADD, REMOVE, SET, WHERE, FILTER, RENAME, TO,
    MISSING, DROP, COLUMNS, COLUMN, ROW, ROWS, FROM, WITH, IN,
    FILL, BLANKS, NULL, EACH, CSV, JSON, REVIEW, RANGE,


    EOF
}
