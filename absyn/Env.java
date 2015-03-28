// rtl package containing all intermediate instructions
package rtl; 

interface Env {
    void insert(String s, Type t, Position p) throws SemanticException;
    Type lookup(String s);
    void setResult(Type t);
    Type getResult();
    Env enter(String name);
}
