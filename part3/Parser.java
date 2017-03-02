/* *** This file is given as part of the programming assignment. *** */
import java.util.List;
import java.util.ArrayList;


public class Parser {

    private class SymbolTable {   
        ArrayList<ArrayList<String>> stack = new ArrayList<ArrayList<String>>();
        // Get top of stack of symbol lists where inner blocks DO NOT inherit from outer blocks
        ArrayList<ArrayList<String>> stack_no_inheritance = new ArrayList<ArrayList<String>>();

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

        public void addVar(String varName) {
            ArrayList top = stack_no_inheritance.get(stack_no_inheritance.size() - 1);
            if (top.contains(varName)) {
                System.err.println("redeclaration of variable " + varName);
            } else {
                // Add to stack_no_inheritance
                top.add(varName);
                // Add to main stack
                if ( !top().contains(varName) ) {
                    top().add(varName);
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
        block();
    }

    private void block(){
        symTable.enterBlock();
        declaration_list();
        statement_list();
        symTable.exitBlock();
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

    private void declaration() {
        mustbe(TK.DECLARE);
        mustis(TK.ID);
        symTable.addVar(tok.string);
        scan();
        while( is(TK.COMMA) ) {
                scan();
                mustis(TK.ID);
                symTable.addVar(tok.string);
                scan();
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
        mustbe(TK.PRINT);
        expr();
    }
    
    private void assignment() {
        ref_id();
        mustbe(TK.ASSIGN);
        expr();
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
        } else {
            symTable.mustExist();
        }
        scan();
    }

    private void parse_do() {
        mustbe(TK.DO);
        guarded_command();
        mustbe(TK.ENDDO);
    }
    
    private void parse_if(){
        mustbe(TK.IF);
        guarded_command();
        while (is(TK.ELSEIF)){
            scan();
            guarded_command();
        }
        if (is(TK.ELSE)){
            scan();
            block();
        }
        mustbe(TK.ENDIF);
    }
        
    private void guarded_command(){
        expr();
        mustbe(TK.THEN);
        block();
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
            scan();
            expr();
            mustbe(TK.RPAREN);
        }
        else if (is(TK.TILDE) || is(TK.ID)){
            ref_id();
        }
        else if (is(TK.NUM)){
            scan();
        }
        else{
            parse_error("factor error");
        }
    }
    
    private void addop(){
        if (is(TK.PLUS) || is(TK.MINUS)){
            scan();
        }
        else{
            parse_error("addop error");
        }
    }
        
    private void multop(){
        if (is(TK.TIMES) || is(TK.DIVIDE)){
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
