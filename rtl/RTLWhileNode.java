package rtl;

public class RTLWhileNode extends RTLNode
{
	public RTLWhileNode(TreeEnv env)
	{
		super(env);
	}

	public String toRTL() throws RTLException
	{
		String flag = "";
		String rtl = "";

		String loopLabel = environment.getNextLabel("loop");
		String endLabel = environment.getNextLabelNoIncrement("end");

		// add label for the loop
		rtl += loopLabel + ":\n";

		// check the condition Node
		RTLExpressionNode conditionNode = (RTLExpressionNode)(getChildren().get(0));
		

		// flag = environment.getNextTemporary(conditionNode.getResultDataType());
		// rtl += conditionNode.toRTL(flag);
		StringBuilder sb = new StringBuilder();
		String loc = conditionNode.getLocation(sb);
		rtl += sb.toString();

		rtl += "beq " + loc + " " + 0 + " " + endLabel + "\n";

		// get the body of while loop
		RTLStatementNode statementNode = (RTLStatementNode)(getChildren().get(1));
		rtl += statementNode.toRTL();

		rtl += "jump " + loopLabel + "\n";

		// add end label
		rtl += endLabel + ":\n";

		return rtl;
	}
}