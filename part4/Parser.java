/* *** This file is given as part of the programming assignment. *** */
import java.util.List;
import java.util.ArrayList;


public class Parser {

    private class SymbolTable {   
        ArrayList<ArrayList<String>> stack = new ArrayList<ArrayList<String>>();
        // Get top of stack of symbol lists where inner blocks DO NOT inherit from outer blocks
        ArrayList<ArrayList<String>> stack_no_inheritance = new ArrayList<ArrayList<String>>();

        private void print_declare(String var_name) {
            System.out.print("int x_");
            System.out.println(var_name + ";");
            System.out.print("int* x_");
            System.out.println(Integer.toString(stack.size() - 1) + "_" + var_name + " = &x_" + var_name + ";");
        }

        public void print_variable(String var_name) {
            System.out.print("x_" + var_name);
        }

        public void print_variable(String var_name, Integer scopingOperator) {
            Integer level = (stack.size() - 1) - scopingOperator;
            System.out.print("*x_" + Integer.toString(level) + "_" + var_name);
        }

        public void print_variable_global(String var_name) {
            System.out.print("*x_0_" + var_name);
        }

        private ArrayList top() {
            return stack.get(stack.size() - 1);
        }

        public void enterBlock() {
            if ( !stack.isEmpty() ) {
                ArrayList top = new ArrayList( top() );
                stack.add( top );
            } else {
                stack.add( new ArrayList() );
            }

            stack_no_inheritance.add( new ArrayList() );
        }

        public void exitBlock() {
            stack.remove(stack.size() - 1);
            stack_no_inheritance.remove(stack_no_inheritance.size() - 1);
        }

        public void addVar(String var_name) {
            ArrayList top = stack_no_inheritance.get(stack_no_inheritance.size() - 1);
            if (top.contains(var_name)) {
                System.err.println("redeclaration of variable " + var_name);
            } else {
                // Add to stack_no_inheritance
                top.add(var_name);
                print_declare(var_name);
                // Add to main stack
                if ( !top().contains(var_name) ) {
                    top().add(var_name);
                }
            }
        }

        public void mustExist() {
            if (top().contains(tok.string)) {
                return;
            } else {
                System.err.println(tok.string + " is an undeclared variable on line " + tok.lineNumber);
                System.exit(1);
            }
        }

        // Parameters: 0 is current scope, 1 is 1 block up, etc.
        public void mustExist(Boolean global, Integer level) {
            // Get Level in List
            String error_message_level = Integer.toString(level);
            level = stack.size() - level - 1;
            if (level < 0) {
                System.err.println("no such variable ~" + error_message_level + tok.string + " on line " + tok.lineNumber);
                System.exit(1);
            }

            if (global) {
                level = 0;
            }
            if (stack_no_inheritance.get(level).contains(tok.string)) {
                return;
            } else {
                if (global) {
                    System.err.println("no such variable ~" + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }
                System.err.println("no such variable ~" + error_message_level + tok.string + " on line " + tok.lineNumber);
                System.exit(1);
            }
        }
    }

    SymbolTable symTable = new SymbolTable();

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    private Scan scanner;
    Parser(Scan scanner) {
        this.scanner = scanner;
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
    }

    private void program() {
        System.out.println("#include <stdio.h>\nint main()");
        block();
    }

    private void block(){
        System.out.println("{");
        symTable.enterBlock();
        declaration_list();
        statement_list();
        symTable.exitBlock();
        System.out.println("}");
    }

    private void declaration_list() {
        // below checks whether tok is in first set of declaration.
        // here, that's easy since there's only one token kind in the set.
        // in other places, though, there might be more.
        // so, you might want to write a general function to handle that.
        while( is(TK.DECLARE) ) {
            declaration();
        }
    }

    private void declaration_helper() {
        mustis(TK.ID);
        symTable.addVar(tok.string);
        scan();
    }

    private void declaration() {
        mustbe(TK.DECLARE);
        declaration_helper();
        while( is(TK.COMMA) ) {
                scan();
                declaration_helper();
            }
    }

    //ADD
    private void statement_list() {
        //statement_list ::= {statement}
        //statement ::= assignment | print | do | if
        while( is(TK.TILDE) || is(TK.PRINT) || is(TK.DO) || is(TK.IF) || is (TK.ID)){ 
            statement();
        }
    }
    
    private void statement() {
        //statement ::= assignment | print | do | if
        if (is(TK.TILDE) || is(TK.ID)){ //~ or id
            assignment();
        }
        else if (is(TK.PRINT)){ //!
            print();
        }
        else if (is(TK.IF)){ //[
            parse_if();
        }
        else if (is(TK.DO)){ //<
            parse_do();
        }
        else{
            parse_error("statement error");
        }
    }
    
    private void print() {
        System.out.println("printf(\"%d\\n\", ");
        mustbe(TK.PRINT);
        expr();
        System.out.println(");");
    }
    
    private void assignment() {
        ref_id();
        System.out.println("=");
        mustbe(TK.ASSIGN);
        expr();
        System.out.println(";");
    }
        
    private void ref_id() {
        Integer level = 0;
        Boolean global = false;
        Boolean useScoping = is(TK.TILDE);
        if (is(TK.TILDE)) {
            scan();
            if (is(TK.NUM)){
                level = Integer.parseInt(tok.string);
                scan();
            } else {
                global = true;
            }
        }
        mustis(TK.ID);
        if (useScoping) {
            symTable.mustExist(global, level);
            if (global) {
                symTable.print_variable_global(tok.string);
            } else {
                symTable.print_variable(tok.string, level);
            }
        } else {
            symTable.mustExist();
            symTable.print_variable(tok.string);
        };
        scan();
    }

    private void parse_do() {
        mustbe(TK.DO);
        System.out.println("while ");
        guarded_command();
        mustbe(TK.ENDDO);
    }
    
    private void parse_if(){
        mustbe(TK.IF);
        System.out.print("if ");
        guarded_command();
        while (is(TK.ELSEIF)){
            scan();
            System.out.print("else if ");
            guarded_command();
        }
        if (is(TK.ELSE)){
            scan();
            System.out.print("else ");
            block();
        }
        mustbe(TK.ENDIF);
    }
    
    private void declare_and_assign(){
        // declare_and_assign ::= '@' id '=' expr
        mustbe(TK.DECLARE);
        declaration_helper();
        mustis(TK.ASSIGN);
        System.out.print(" = ");
        scan();
        expr();
    }

    private void increment(){
        // increment ::= id '+' | id '-'
        mustis(TK.ID);
        System.out.print(tok.string);
        scan();
        if ( is(TK.PLUS) ) {
            System.out.print("++");
        } else if ( is (TK.MINUS)) {
            System.out.print("--");
        } else {
            parse_error("increment error");
        }
        scan();
    }

    // guarded_command ::= expr ':' block | declare_and_assign ':' expr ':' increment ':' block
    private void guarded_command(){
        // For Loop
        if ( is(TK.DECLARE) ) {
            System.out.print("for (");
            declare_and_assign();
            System.out.print(";");
            mustbe(TK.THEN);
            expr();
            System.out.print(";");
            mustbe(TK.THEN);
            increment();
            System.out.println(";){");
            mustbe(TK.THEN);
            block();
            System.out.println("\n}");
        }
        // While Loop
        else {
            System.out.print("( ");
            expr();
            mustbe(TK.THEN);
            System.out.println(" <= 0 )");
            block();
        }
    }
    
    private void expr(){
        //expr ::= term { addop term }
        term();
        while(is(TK.PLUS) || is(TK.MINUS)){
            addop();
            term();
        }
    }
    
    private void term(){
        //term ::= factor { multop factor }
        factor();
        while(is(TK.TIMES) || is(TK.DIVIDE)){
            multop();
            factor();
        }
    }
    
    private void factor(){
        if (is(TK.LPAREN)){
            System.out.print(tok.string);  //(
            scan();
            expr();
            System.out.print(tok.string); // )
            mustbe(TK.RPAREN);
        }
        else if (is(TK.TILDE) || is(TK.ID)){
            ref_id();
        }
        else if (is(TK.NUM)){
            System.out.print(tok.string); //number
            scan();
        }
        else{
            parse_error("factor error");
        }
    }
    
    private void addop(){
        if (is(TK.PLUS) || is(TK.MINUS)){
            System.out.print(tok.string); //+ or -
            scan();
        }
        else{
            parse_error("addop error");
        }
    }
        
    private void multop(){
        if (is(TK.TIMES) || is(TK.DIVIDE)){
            System.out.print(tok.string); //* or /  
            scan();    
        }
        else{
            parse_error("multop error");
        }
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustis(TK tk) {
        if( tok.kind != tk ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                        tok);
            parse_error( "missing token (mustbe)" );
        }
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( tok.kind != tk ) {
            System.err.println( "mustbe: want " + tk + ", got " +
                        tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line "
                    + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}
