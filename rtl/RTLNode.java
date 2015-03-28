package rtl;

import java.util.*;

public class RTLNode
{
	private  List<RTLNode> children = null;
	protected TreeEnv environment = null;

	public RTLNode(TreeEnv env)
	{
		children = new ArrayList<RTLNode>();
		this.environment = env;
	}

	public void addChild(RTLNode node)
	{
		assert this.children != null;

		this.children.add(node);
	}

	public List<RTLNode> getChildren()
	{
		assert this.children != null;

		return this.children;
	}

	public RTLNode getChild(int index) throws Exception
	{
		assert this.children != null;

		if(this.children.size() == index)
			throw new Exception();

		return this.children.get(index);
	}

	public String toRTL() throws RTLException {
		//TODO
		return "TODO ";
	}

	public void print() {
		print("");
	}

	public void print(String prefix) {
		if (this instanceof RTLExpressionNode)
			System.out.println(prefix + " - " + this.getClass() + " = " + ((RTLExpressionNode)this).getValue());
		else
			System.out.println(prefix + " - " + this.getClass());
		
		for (RTLNode c : getChildren())
			c.print(prefix + "+");
	}
}