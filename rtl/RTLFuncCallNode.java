package rtl;

import java.util.ArrayList;

public class RTLFuncCallNode extends RTLNode
{
	String ident;

	public RTLFuncCallNode(TreeEnv env, String ident)
	{
		super(env);
		this.ident = ident;
	}

	public String getIdentifier() {
		return ident;
	}

	public String toRTL() throws RTLException
	{
		String rtl = "";
		ArrayList<String> paramList = new ArrayList<String>();

		for (RTLNode c : getChildren()) 
		{
			if(c instanceof RTLFuncCallNode)
			{
				RTLFuncCallNode fNode = (RTLFuncCallNode) c;
				String location = environment.getNextTemporary(environment.lookup(fNode.getIdentifier()).getDataType());
				rtl += fNode.toRTL();
				rtl += "sret " + location + "\n";

				paramList.add(location);
				continue;
			}

			RTLExpressionNode child = (RTLExpressionNode) c;

			if (!child.isSubexpression()) {
				StringBuilder sb = new StringBuilder();
				paramList.add(child.getLocation(sb));
				rtl += sb.toString();
			} else {
				String t = environment.getNextTemporary(child.getResultDataType());
				rtl += child.toRTL(t);
				paramList.add(t);
			}
		}

		rtl += "call " + ident + "(";
		
		for (String param : paramList) {
			rtl += param + ",";	
		}

		if (paramList.size() != 0)
			rtl = rtl.substring(0, rtl.length() - 1);
		
		return  rtl + ")\n";
	}
}