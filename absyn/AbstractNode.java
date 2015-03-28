// rtl package containing all intermediate instructions
package rtl; 

import java.util.*;

enum Id 
{
	PROGRAM, 
	VARDEC, SCALAR_TYPE, ARR_TYPE, FUNC, EXTERN,
	CHAR, INT, VOID,
	COMPOUND_STMNT, STMNT, 
	SIMPLE_COMPOUND_STMNT, EMPTY_STMNT, IF, WHILE, RETURN, 
	EFFECT, ASSIGN, BINARY,	UNARY,
	ARRAY, FCALL, DUMMY,
	IDENT, INTEGER_LITERAL, CHAR_LITERAL, PAREN, INCLUDE, FILE_NAME
}

enum Binop 
{
	OROR, ANDAND, EQ, NE,
	LT, GT, LTEQ, GTEQ, 
	PLUS, MINUS,
	MUL, DIV
}

enum Unop { NEG, NOT }

public abstract class AbstractNode 
{
	private Id id;
    private String label;
    private List<AbstractNode> children = new ArrayList<AbstractNode>();
    private Position position;
    private String value;
    
    public AbstractNode (AbstractNode n) 
    {
    	id = n.id;
    	label = n.label;
    	children = n.children;
    	position = n.position;
    	value = n.value;
    }

    public AbstractNode (Id _id) {
		id = _id;
    }

    public AbstractNode (Id _id, String _label) 
    {
		id = _id;
		label = _label;
    }

    public AbstractNode (Id _id, Token t) 
    {
		id = _id;
		label = t.image;
		value = t.image;
		position = Position.fromToken(t);
    }

    public void add(AbstractNode child)
    {
		children.add(child);
    }

    public int numberOfChildren() 
    {
		return children.size();
    }

    public AbstractNode getChild(int i) 
    {
		return children.get(i);
    }

    public List<AbstractNode> getChildren()
    {
    	return children;
    }

    public Id getId() 
    {
		return id;
    }

    public void setPosition(Position p) 
    {
		position = p;
    }

    public Position getPosition () 
    {
		return position;
    }

    public String getValue() 
    {
    	return value;
    }

    public void printHead(String prefix, String suffix) {
		if (label == null)
			System.out.print(prefix+id);
		else
			System.out.print(prefix+id + " ( " + label + " )");

		if (position != null) {
		    System.out.print(" @ "+position);
		}
		System.out.println(" "+suffix);
    }

    public void printChildren(String prefix) 
    {
		if (children != null) 
		{
		    for (AbstractNode n : children) 
		    {
				if (n != null) {
			    	n.print(prefix + " ");
				}
		    }
		}
    }

    public void print(String prefix, String suffix) {
		printHead(prefix, suffix);
		printChildren(prefix);
    }

    public void print(String prefix) {
		print(prefix, "");
    } 
}

class Node extends AbstractNode
{
    public Node (Node n) 
    {
    	super(n);
    }

    public Node (Id _id) {
		super(_id);
    }

    public Node (Id _id, String _label) 
    {
		super(_id, _label);
    }

    public Node (Id _id, Token t) 
    {
    	super(_id, t);
    }  
}

class DeclarationNode extends AbstractNode 
{
	Type type;
	AbstractNode declarator;

	public DeclarationNode(AbstractNode decNode) 
	{
		super(decNode);
		this.declarator = getChild(1);

		if(decNode.numberOfChildren() == 3)
		{
			type = new Type(Type.SymbolType.ARRAY);
			String arraySizeVal = getChild(2).getValue();

			if(arraySizeVal != null) {
				int size = Integer.valueOf(getChild(2).getValue());
				type.setArraySize(size);
			}
			else {
				type.setArraySize(-1);
			}
		}
		else if(decNode.numberOfChildren() == 2){
			type = new Type(Type.SymbolType.VAR);
		}			

		switch (getChild(0).getId()){
			case INT:
				type.setDataType(Type.DataType.INT);
				break;
			case CHAR:
				type.setDataType(Type.DataType.CHAR);
				break;
		}
	}

    public DeclarationNode (Id _id) {
		super(_id);
    }

    public DeclarationNode (Id _id, String _label) 
    {
		super(_id, _label);
    }

    public DeclarationNode (Id _id, Token t) 
    {
		super(_id, t);
    }

	public Type getType() {
		return type;
	}

	public String getDeclarator() {
		return declarator.getValue();
	}
}

class FunctionNode extends AbstractNode
{
	Type type;
	AbstractNode functionDeclarator;
	AbstractNode functionBody;

	List<AbstractNode> parameters;

	public FunctionNode(AbstractNode functionNode)
	{
		super(functionNode);

		type = new Type(Type.SymbolType.FUNC);

		// Return type
		switch(getChild(0).getId()) {
		case INT:
			type.setDataType(Type.DataType.INT);
			break;
		case CHAR:
			type.setDataType(Type.DataType.CHAR);
			break;
		case VOID:
			type.setDataType(Type.DataType.VOID);
			break;
		}


		parameters = new ArrayList<AbstractNode>();
		this.functionDeclarator = functionNode.getChild(1);

		int paramEnd;
		AbstractNode lastChild = functionNode.getChild(functionNode.numberOfChildren() - 1);

		if (lastChild.getId() == Id.VARDEC) {
			paramEnd = functionNode.numberOfChildren();
			this.functionBody = null;
		} else {
			paramEnd = functionNode.numberOfChildren() - 1;
			this.functionBody = functionNode.getChild(functionNode.numberOfChildren() - 1);
		}

		boolean noParams = false;
		for(int i = 2; i < paramEnd; i++)
		{
			Id paramType = functionNode.getChild(i).getId();
			if(paramType == Id.VOID){
				parameters.add(functionNode.getChild(i));
				noParams = true;
				break;
			} 
			else {
				DeclarationNode param = new DeclarationNode(functionNode.getChild(i));
				parameters.add(param);
			}
		}

		if (!noParams)
			type.setArraySize(paramEnd - 2);
		else
			type.setArraySize(0);

		
	}

	public FunctionNode (Id _id) {
		super(_id);
    }

    public FunctionNode (Id _id, String _label) 
    {
		super(_id, _label);
    }

    public FunctionNode (Id _id, Token t) 
    {
		super(_id, t);
    }

	public Type getType() {
		return type;
	}

	public String getFunctionDeclarator(){
		return functionDeclarator.getValue();
	}

	public AbstractNode getFunctionBody(){
		return functionBody;
	}

	public List<AbstractNode> getParameters(){
		return parameters;
	}
}

class IncludeFileNode extends AbstractNode
{
	public IncludeFileNode(AbstractNode includeNode) 
	{
		super(includeNode);
	}

	public IncludeFileNode (Id _id) {
		super(_id);
    }

    public IncludeFileNode (Id _id, String _label) 
    {
		super(_id, _label);
    }

    public IncludeFileNode (Id _id, Token t) 
    {
		super(_id, t);
    }
}