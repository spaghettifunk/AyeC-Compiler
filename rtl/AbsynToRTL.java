package rtl;

import java.util.*;

public class AbsynToRTL
{
	private RTLNode currentNode = null;

	public AbsynToRTL(RTLNode root) {
		this.currentNode = root;
	}

	public String generateIC()
	{
		assert currentNode != null;

		String rtl = "";

		List<RTLNode> children = currentNode.getChildren();
		try {
			for(RTLNode child : children)
			{
				rtl += child.toRTL();
			}	
		} catch (RTLException e) {
			System.out.println(e.getMessage());
			return "";
		}
		

		return rtl;
	}

	// private void translateExpression(AbstractNode expr)
	// {
	// 	assert expr != null;

	// 	List<AbstractNode> children = expr.getChildren();
	// 	for(AbstractNode child : children)
	// 	{
	// 		translateExpression(child);
	// 	}
	// }
}