package rtl;

public class RTLifNode extends RTLNode 
{
	public RTLifNode(TreeEnv env)
	{
		super(env);
	}

	public String toRTL() throws RTLException
	{
		String flag = "";
		String rtl = "";

		String elseLabel = environment.getNextLabel("else");
		String endLabel = environment.getNextLabelNoIncrement("end");

		// check the condition Node		
		RTLExpressionNode conditionNode = (RTLExpressionNode)(getChildren().get(0));
		flag = environment.getNextTemporary(conditionNode.getResultDataType());
		rtl += conditionNode.toRTL(flag);

		// check the if condition, if == 0 go to else label
		RTLStatementNode statementThen = (RTLStatementNode)(getChildren().get(1));
		if(getChildren().size() > 2) {
			rtl += "beq " + flag + " " + 0 + " " + elseLabel + "\n";	
			rtl += statementThen.toRTL(flag) + "jump " + endLabel + "\n";
		} else {
			rtl += "beq " + flag + " " + 0 + " " + endLabel + "\n";
			rtl += statementThen.toRTL(flag);
		}

		// make block code of else statement
		if(getChildren().size() > 2)
		{
			rtl += elseLabel + ":\n";
			RTLStatementNode statementElse = (RTLStatementNode)(getChildren().get(2));
			rtl += statementElse.toRTL(flag);
		}

		//System.out.println("*** ");

		// add end label
		rtl += endLabel + ":\n";

		return rtl;
	}
}