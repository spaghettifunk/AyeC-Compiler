package rtl;

public class RTLDeclarationNode extends RTLNode
{
	private String declarator;
	private Type type;

	public RTLDeclarationNode(TreeEnv env, String decl, Type tp)
	{
		super(env);

		this.declarator = decl;
		this.type = tp;
	}

	public String getDeclarator() 
	{
		return declarator;
	}

	public String toRTL()
	{
		return toRTL(null);
	}

	public String toRTL(String location)
	{
		String rtl = "";
		Type type = environment.lookup(declarator);

		if (environment.isGlobalScope()) {
			rtl += declarator + ":\n";

			// Set location to label
			type.setLocation(declarator);
		} else {
			if (location == null)
				location = environment.getNextLocalLocation();

			rtl += "declare " + location + " ";
			//rtl += "(" + declarator + ") ";

			type.setLocation(location);
		}

		if (type.getSymbolType() == Type.SymbolType.ARRAY) {
			return rtl + "arr " + type.getDataType().toString() + " " + type.getArraySize() + "\n";
		} else {
			return rtl + "var " + type.getDataType().toString() + "\n";
		}
	}
}	