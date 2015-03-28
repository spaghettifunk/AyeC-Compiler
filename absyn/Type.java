// rtl package containing all intermediate instructions
package rtl; 

class Type 
{
    enum DataType 
    {
    	INT("INT"), CHAR("CHAR"), VOID("VOID");

        private String dataTypeName; 
        private DataType(String name) { 
            this.dataTypeName = name; 
        } 
        
        @Override 
        public String toString(){ 
            return dataTypeName; 
        }
    }

    enum SymbolType 
    {
        FUNC("FUNC"), VAR("VAR"), ARRAY("ARRAY"), CONST("CONST");

        private String symblTypeName; 
        private SymbolType(String name) { 
            this.symblTypeName = name; 
        } 
        
        @Override 
        public String toString(){ 
            return symblTypeName; 
        }
    }

    private SymbolType symbolType;
    private DataType dataType;
    private TreeEnv paramsTable;

    private int arraySize = 0;

    // RTL stuff
    private String location;
    private int ssaCount = 0;

    public Type(SymbolType st)
    {
        this.symbolType = st;
    }

    public void setDataType(DataType dt)
    {
        this.dataType = dt;
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public SymbolType getSymbolType() 
    {
        return symbolType;
    }

    public void setArraySize(int size)
    {
        this.arraySize = size;
    }

    public int getArraySize()
    {
    	return arraySize;
    }

    public void setParamsTable(TreeEnv pt)
    {
        this.paramsTable = pt;
    }

    public TreeEnv getParamsTable()
    {
        return paramsTable;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        if (ssaCount == 0)
            return location;
        else
            return location + "_" + ssaCount;
    }

    public String getLocationAndIncrement() {
        //ssaCount++;
        return getLocation();
    }    

    // DEBUG
    public String getTypeValue() 
    {
        return symbolType + " " + dataType + " " + arraySize;
    }
}