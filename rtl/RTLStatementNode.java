package rtl;

public class RTLStatementNode extends RTLNode
{
	public RTLStatementNode(TreeEnv env)
	{
		super(env);
	}

	public String toRTL() throws RTLException
	{
		return toRTL(null);
	}

	public String toRTL(String flag) throws RTLException
	{
		String rtl = "";

		for (RTLNode child : getChildren()) {
			rtl += child.toRTL();
		}

		return rtl;
	}	
}