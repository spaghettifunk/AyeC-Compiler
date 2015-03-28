package rtl;

import java.util.*;

public class RTLFuncDecNode extends RTLNode
{
	String ident;

	public RTLFuncDecNode(TreeEnv env, String ident)
	{
		super(env);
		this.ident = ident;
	}

	public String toRTL() throws RTLException
	{
		String rtl = "func_" + ident + ":\n";

		List<RTLNode> children = getChildren();

		for (int i = 0; i < environment.lookup(ident).getArraySize(); i++) {
			rtl += ((RTLDeclarationNode)(children.get(i))).toRTL(environment.getNextParamLocation());
		}

		String bodyRtl = "";
		String declRtl = "";
		String temporaryRtl = "";
		for (int i = environment.lookup(ident).getArraySize(); i < children.size(); i++) {
			RTLNode c = children.get(i);

			if (c instanceof RTLDeclarationNode) {
				declRtl += c.toRTL();
			} else {
				bodyRtl += c.toRTL();
			}
		}

		for (RTLDeclarationNode d : environment.lookup(ident).getParamsTable().getTemporaries()) {
			temporaryRtl += d.toRTL(d.getDeclarator());
		}

		rtl += declRtl + temporaryRtl + bodyRtl;
		
		rtl += "endfunc\n";

		return rtl;
	}
}