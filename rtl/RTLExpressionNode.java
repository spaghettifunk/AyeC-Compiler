package rtl;

public class RTLExpressionNode extends RTLNode
{
	private ExpressionType type;
	private String value;
	private int arrIndex;
	private Type.DataType resultType;

	enum ExpressionType {LITERAL, VAR, ARR, BINARY, UNARY} 

	public RTLExpressionNode(TreeEnv env, ExpressionType type, String value, Type.DataType resultDataType)
	{
		super(env);
		this.type = type;
		this.value = value;
		this.resultType = resultDataType;

        // increment the offset
		// TreeEnv.incrementOffsetCounter();
	}

	public RTLExpressionNode(TreeEnv env, ExpressionType type, String value, int arrIndex, Type.DataType resultDataType)
	{
		this(env, type, value, resultDataType);
		this.arrIndex = arrIndex;
	}

	public boolean isSubexpression() 
	{
		if (type == ExpressionType.LITERAL
			|| type == ExpressionType.VAR
			|| type == ExpressionType.ARR) {
			return false;
		} else {
			return true;
		}
	}

	public void setResultDataType(Type.DataType t) 
	{
		resultType = t;
	}

	public Type.DataType getResultDataType() 
	{
		return resultType;
	}

	public String getValue() 
	{
		return value;
	}

	public int getArrayIndex() 
	{
		return arrIndex;
	}

	public ExpressionType getType() {
		return type;
	}

	public String toRTL() throws RTLException 
	{
		return toRTL(null);
	}

	public String toRTL(String target) throws RTLException
	{
		switch (type) {
		case LITERAL:
			// If we have literal expression but no temporary to save to, 
			// there's nothing to generate. Otherwise store to temporary,
			// so it can be used by parent expression
			if (target == null)
				return "";
			else
				return "store " + value + " " + target + "\n";

		case BINARY:
			// If we don't have a target to store our result, there's nothing
			// to generate. Assignment is an exception, as it only needs l and r
			if (target == null && value != "=")
				return "";

			String left = "???";
			String right = "???";
			String rtl = "";
			String operands;

			RTLNode leftNode = getChildren().get(0);
			RTLNode rightNode = getChildren().get(1);
			
			// Assignments need special treatment (SSA)
			if (value !=  "=") {
				StringBuilder sb = new StringBuilder();
				left = getLocation(leftNode, sb, false);
				right = getLocation(rightNode, sb, false);
				rtl += sb.toString();
			}

			switch (value) {
				case "+":
					// add t1 t2 t3; add t1 + t2 and store result in t3
					return rtl + "add " + left + " " + right + " " + target + "\n";
				case "-":
					return rtl + "sub " + left + " " + right + " " + target + "\n";
				case "*":
					return rtl + "mul " + left + " " + right + " " + target + "\n";
				case "/":
					return rtl + "div " + left + " " + right + " " + target + "\n";
				case "&&":
					return rtl + "and " + left + " " + right + " " + target + "\n";
				case "||":
					return rtl + "or " + left + " " + right + " " + target + "\n";
				case "&":
					return rtl + "and " + left + " " + right + " " + target + "\n";
				case "|":
					return rtl + "or " + left + " " + right + " " + target + "\n";
				case "<":
					// compare left and right, store result in target 
					return rtl + "slt " + target + " "+  left + " " + right + "\n";
				case ">":
					return rtl + "sgt " + left + " " + right + " " + target + "\n";
				case "<=":
				{
					String labelElseEq = environment.getNextLabel("else");
					String labelEnd = environment.getNextLabelNoIncrement("end");					
					
					// if a0 == a1
					rtl += "beq" + " " + left + " " + right + " " + labelElseEq + "\n";
					rtl += "slt " + target + " " +  left + " " + right + "\n";
					rtl += "jump " + labelEnd + "\n";
					rtl += labelElseEq + ":\n";
					rtl += "store 1 " + target + "\n";
					rtl += labelEnd + ":\n";

					return rtl;
				}
				case ">=":
				{
					String labelElseEq = environment.getNextLabel("else");
					String endLabel = environment.getNextLabelNoIncrement("end");
					
					// if a0 == a1
					rtl += "beq" + " " + left + " " + right + " " + labelElseEq + "\n";
					rtl += "sgt " + target + " " +  left + " " + right + "\n";
					rtl += "jump " + endLabel + "\n";
					rtl += labelElseEq + ":\n";
					rtl += "store 1 " + target + "\n";
					rtl += endLabel + ":\n";

					return rtl;
				}
				case "==":
				{
					String labelElseEq = environment.getNextLabel("else");
					String labelEndEq = environment.getNextLabelNoIncrement("end");

					rtl += "bne" + " " + left + " " + right + " " + labelElseEq + "\n";
					rtl += "store 1 " + target + "\n";
					rtl += "jump " + labelEndEq + "\n";
					rtl += labelElseEq + ":\n";
					rtl += "store 0 " + target + "\n";
					rtl += labelEndEq + ":\n";

					return rtl;
				}
				case "!=":
				{
					String labelElseNeq = environment.getNextLabel("else");
					String labelEndNeq = environment.getNextLabelNoIncrement("end");

					rtl += "beq" + " " + left + " " + right + " " + labelElseNeq + "\n";
					rtl += "store 1 " + target + "\n";
					rtl += "jump " + labelEndNeq + "\n";
					rtl += labelElseNeq + ":\n";
					rtl += "store 0 " + target + "\n";
					rtl += labelEndNeq + ":\n";

					return rtl;
				}
				case "=":
					// a = b; store b a
					StringBuilder sb = new StringBuilder();
					right = getLocation(rightNode, sb, false);
					left = getLocation(leftNode, sb, true);
					
					rtl += sb.toString();

					return rtl + "store " + right + " " + left + "\n";
				default:
					throw new RTLException("Error generating expression");
			}

		case UNARY:
			String unaryRtl = "";
			RTLExpressionNode node = (RTLExpressionNode)(getChildren().get(0));

			String zeroVal = "0";

			StringBuilder sb = new StringBuilder();
			String rightVal = getLocation(node, sb, false);
			unaryRtl += sb.toString();

			switch(value)
			{
				case "-":
					return unaryRtl + "sub " + zeroVal + " " + rightVal + " " + target + "\n";
				case "!":
					String labelElseEq = environment.getNextLabel("else");
					String labelEndEq = environment.getNextLabelNoIncrement("end");

					unaryRtl += "bne" + " " + zeroVal + " " + rightVal + " " + labelElseEq + "\n";
					unaryRtl += "store 1 " + target + "\n";
					unaryRtl += "jump " + labelEndEq + "\n";
					unaryRtl += labelElseEq + ":\n";
					unaryRtl += "store 0 " + target + "\n";
					unaryRtl += labelEndEq + ":\n";

					return unaryRtl;
				default:
					throw new RTLException("Error generating expression");
			}

		case VAR:
			StringBuilder sbv = new StringBuilder();
			String l = getLocation(sbv);
			return sbv.toString() + l + "\n";

		default:
			throw new RTLException("Unexpected expression type");
		}
	}

	public String getLocation(StringBuilder rtl) throws RTLException {
		return getLocation(this, rtl, false);
	}

	private String getLocation(RTLNode node, StringBuilder rtl, boolean increment) throws RTLException {
		String location = "???";

		// Node might be a function call. In that case, save return value to a
		// temporary and work with that
		if (node instanceof RTLFuncCallNode) {
			RTLFuncCallNode fNode = (RTLFuncCallNode) node;
			location = environment.getNextTemporary(environment.lookup(fNode.getIdentifier()).getDataType());
			rtl.append(fNode.toRTL());
			rtl.append("sret " + location + "\n");
			return location;
		}

		RTLExpressionNode expNode = (RTLExpressionNode) node;

		// If we have subexpressions, store their outcome in
		// temporary and work with that	
		if (expNode.isSubexpression()) {
			location = environment.getNextTemporary(expNode.getResultDataType());
			rtl.append(expNode.toRTL(location));
		} else {
			switch(expNode.getType()) {
			case VAR:
				if (increment)
					location = environment.lookup(expNode.getValue()).getLocationAndIncrement();
				else
					location = environment.lookup(expNode.getValue()).getLocation();

				break;
			
			case ARR:
				// If we don't have an index, treat as variable
				int arrIndex = expNode.getArrayIndex();

				if (arrIndex == -1) {
					if (increment)
						location = environment.lookup(expNode.getValue()).getLocationAndIncrement();
					else
						location = environment.lookup(expNode.getValue()).getLocation();

					break;
				}

				if (increment)
					location = environment.lookup(expNode.getValue()).getLocationAndIncrement();
				else
					location = environment.lookup(expNode.getValue()).getLocation();

				location += "(" + arrIndex + ")";

				break;

			case LITERAL:
				location = expNode.getValue();
				break;
			}
			
		}

		return location;
	}
}	