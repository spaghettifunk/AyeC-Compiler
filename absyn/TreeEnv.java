// rtl package containing all intermediate instructions
package rtl; 

import java.util.*;

class TreeEnv implements Env
{
    private static int labelCounter = 0; 

    private int localCount = 0;
    private int paramCount = 0;
    private int temporariesCounter = 0;

    Map<String, Type> symTable =  new HashMap<String, Type>();
    ArrayList<String> positionList = new ArrayList<String>();
    TreeEnv parent;
    Type result;
    String envName;

    ArrayList<RTLDeclarationNode> temporaries = new ArrayList<RTLDeclarationNode>();

    private static boolean _DEBUG = false;

    public TreeEnv() {
        parent = null;
        this.envName = "Program";
    }

    public TreeEnv(TreeEnv parent, String name) {
        this.parent = parent;
        this.envName = name;
    }

    public boolean isGlobalScope() {
        return parent == null;
    }

    public String getNextLocalLocation()
    {
        return "local" + localCount++;
    }

    public String getNextParamLocation()
    {
        return "param" + paramCount++;
    }
    
    public void insert(String s, Type t, Position p) throws SemanticException 
    {
        if (_DEBUG)
	       System.out.println(envName + ": inserting " + s);

        if (symTable.get(s) != null)
            throw new SemanticException("Identifier "+s+" doubly defined", p);
        
        symTable.put(s,t);

        // keep track of position
        positionList.add(s);
    }
    
    public Type lookup(String s) 
    {
        if (_DEBUG)
            System.out.println(envName + ": lookup: "+s);
        
        Type r = symTable.get(s);

        if (r != null) 
            return r;
        else {
            if (_DEBUG)
                System.out.println("not found, checking parent");

            if (parent != null)
                return parent.lookup(s);
            else
                throw new IllegalStateException("Symbol " + s + " not defined");
        }
    }

    public Type lookup(int pos)
    {
        if(_DEBUG)
            System.out.println(envName + ": lookup: " + pos);
        
        return lookup(positionList.get(pos));
    }

    public String getSymbolAt(int pos) 
    {
        return positionList.get(pos);
    }

    public int getCount() 
    {
        return positionList.size();
    }

    public void setResult(Type t) 
    {
        if (parent != null)
            result = t;
        else
            throw new IllegalStateException("No result type in global environment");
    }

    public Type getResult() 
    {
	    if (parent != null)
            return result;
        
        throw new IllegalStateException("No result type in global environment");
    }

    public Env enter(String name) 
    {
	   return new TreeEnv(this, name);
    }

    public String getName() 
    {
        return envName;
    }

    public void printSymbolTable()
    {
        System.out.println("\nPrinting Symbol Table of: " + this.envName);
        for (Map.Entry<String, Type> entry : symTable.entrySet()) 
        {
            System.out.println("  " + entry.getKey() + " : " + entry.getValue().getTypeValue());
        }
    }

    private int getNextLabelCounter()
    {
        return labelCounter++;
    }

    private int getNextTemporariesCounter()
    {
        return temporariesCounter++;
    }

    private String makeLabel()
    {
        return "L" + getNextLabelCounter() + " ";
    }

    // return a temporary with the counter incremented by 1
    public String getNextTemporary(Type.DataType type) 
    {
        String name = "t" + getNextTemporariesCounter();

        Type t = new Type(Type.SymbolType.VAR);
        t.setDataType(type);
        temporaries.add(new RTLDeclarationNode(this, name, t));

        try {
            insert(name, t, null);
        } catch (Exception e) {
            System.out.println("Error creating temporary");
        }

        return name;
    }

    public String getNextLabel(String prefix)
    {   
        getNextLabelCounter();
        return getNextLabelNoIncrement(prefix);
    }

    public String getNextLabelNoIncrement(String prefix)
    {
        if(prefix == null)
            return makeLabel();
        else
            return prefix + labelCounter;
    }

    public ArrayList<RTLDeclarationNode> getTemporaries() {
        return temporaries;
    }
}
