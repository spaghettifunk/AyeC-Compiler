package rtl; 

import java.util.*;

public class MipsEnv {
	Map<String, Symbol> symTable =  new HashMap<String, Symbol>();
	MipsEnv parent = null;

    String envName;

    // Offsets
    private int localVarOffset = 0;

    private class Symbol {
    	public Type.DataType dataType;
    	public Type.SymbolType symbolType;
        public String address = "000";
    	public int arraySize = -1;
        public boolean isReference;

    	public Symbol(Type.DataType dt, Type.SymbolType st, int arraySize, String address) {
    		this.dataType = dt;
    		this.symbolType = st;
    		this.arraySize = arraySize;
            this.address = address;
            this.isReference = false;
    	}
    }

    public MipsEnv() { }

    public MipsEnv(String name) {
    	envName = name;
    }

    public MipsEnv(MipsEnv parent) {
        this.parent = parent;
    }

    public void insert(String name, Type.DataType dt, Type.SymbolType st) throws MIPSException
    {
    	insert(name, dt, st, -1);
    }

    public void insert(String s, Type.DataType dt, Type.SymbolType st, int arraySize) throws MIPSException 
    {
        if (symTable.get(s) != null)
            throw new MIPSException("Identifier "+s+" doubly defined");
        
        int totalOffset = (MipsConstants.RegisterSize * 9  + localVarOffset) * (-1);

        Symbol sym = new Symbol(dt, st, arraySize, totalOffset + "($fp)");
        if(st == Type.SymbolType.ARRAY && s.startsWith("param")) {
            sym.isReference = true;
        }

        symTable.put(s, sym);
    }

    public void insertGlobal(String name, Type.DataType dt, Type.SymbolType st) throws MIPSException
    {
        insertGlobal(name, dt, st, -1);
    }

    public void insertGlobal(String s, Type.DataType dt, Type.SymbolType st, int arraySize) throws MIPSException 
    {
        if (symTable.get(s) != null)
            throw new MIPSException("Identifier "+s+" doubly defined");

        symTable.put(s, new Symbol(dt, st, arraySize, s));
    }
    
    public Type.DataType lookupDataType(String s) 
    {   
        Symbol sym = symTable.get(s);

        if (sym != null) 
            return sym.dataType;
        else {
            if (parent != null){
                return parent.lookupDataType(s);
            }
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public Type.SymbolType lookupSymbolType(String s) 
    {   
        Symbol sym = symTable.get(s);

        if (sym != null) 
            return sym.symbolType;
        else {
            if (parent != null)
                return parent.lookupSymbolType(s);
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public int lookupArraySize(String s) 
    {   
        Symbol sym = symTable.get(s);

        if (sym != null) 
            return sym.arraySize;
        else {
            if (parent != null)
                return parent.lookupArraySize(s);
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public String lookupAddress(String s) 
    {   
        Symbol sym = symTable.get(s);

        if (sym != null) 
            return sym.address;
        else {
            if (parent != null)
                return parent.lookupAddress(s);
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public boolean lookupIsReference(String s) {
        Symbol sym = symTable.get(s);

        if (sym != null) 
            return sym.isReference;
        else {
            if (parent != null)
                return parent.lookupIsReference(s);
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public MipsEnv enter() 
    {
        return new MipsEnv(this);
    }

    public MipsEnv exit() 
    {
        return parent;
    }

    public int getLocalVarOffset() 
    {
        return localVarOffset;
    }

    public void addLocalVarOffset(int bytes) 
    {
        localVarOffset += bytes;
    }
}