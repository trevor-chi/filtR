## FILTR - A Data Cleanup Language - By Trevor Childers and Great Abhieyighan

FILTR is designed as a simpler and more intuitive alternative language to SQL. Meant to be used for data cleanup and transformation, the syntax and commands are intended to be more user friendly so that filtering and preparing data is as easy as possible.

## How do I run interpreter?
The REPL for the language can be ran from the 'main' function in the 'filtR.Java' file. This will run the interpreter and allow you to type commands into the terminal. If you want to execute the interpreter on a test file, you can do so by copying the command that is executed when runnning the REPL, and giving your test file as an argument. 

## Current Features
FILTR currently has these feature available <br><br>
• Dataset importing: use "file.csv" as myData; or import "file.json" as myData; <br><br>
• Dropping columns: drop columns colA, colB from myData; <br><br>
• Renaming columns: rename datasetName.oldColumnName to "newName"; <br><br>
• Adding new columns: add column myData.newCol = expression; (expression fills the new column with that value) <br><br>
• Ability to give a list of values when adding new columns so that the column is filled with specific values and not just one: add column myData.newCol = 10, 20, 30, 40, 50; <br><br>
• Filling blanks or nulls: fill blanks in datasetName.col with expression; or fill null in myData.score with expression where col operator expression; (the where clause is optional for 'fill) <br><br>
• Filtering rows into a new dataset: filter datasetName where age > 18 as adults; <br><br>
• Exporting datasets: export adults to "out.csv" as csv; or save data to "file.json" as json; <br><br>
• Assignments: set x = 10; <br><br>
• Printing values: print expression; <br><br>
• Blocks for grouping statements: { ... } <br><br>
• Conditional logic with if/else: if condition { ... } else if condition { ... } else { ... } <br><br>
• For-each loops over rows or columns: for each row as r in myData { ... } <br><br>
• Range statement: for range 1..10 as x { ... } (allows for use of ranges similar to python)<br><br>
• Function definitions: function name(params) { ... } <br><br>
• Expressions with arithmetic (+ - * /), comparisons (> >= < <=), equality (== !=), logical operators (and, or, !), and parentheses <br><br>
• Dot access for fields and columns: myData.column <br><br>
• Function calls: callName(arg1, arg2) <br><br>
• Literal types: numbers, strings, booleans, null <br><br>
• Review feature that looks for and returns null values and mismatched types that are present in the data set: review datasetName;
