// rtl package containing all intermediate instructions
package rtl; 

import java.util.*;
import java.io.*;

public class Semantic 
{
	private static boolean _DEBUG = false;
	private static RTLNode rtlTree = null;

    public static void error (String s) {
		System.out.println(s);
    }
    
    public static RTLNode checkProgram(AbstractNode program) 
    {
		TreeEnv env = new TreeEnv();
		rtlTree = new RTLNode(env);

		boolean programFlag = true;

		for (AbstractNode n : program.getChildren()) {
		    switch (n.getId()) 
			{
				case VARDEC:
					programFlag &= checkVarDec(n, env, rtlTree);
					break;
				case FUNC:
					programFlag &= checkFunc(n, env, rtlTree);
					break;
				case INCLUDE:
					programFlag &= checkInclude(n, env);
					break;
			}

			if(programFlag == false)
				return null;
		}

		if(_DEBUG == true){
			env.printSymbolTable();
		}

		if (programFlag)
			return rtlTree;
		else
			return null;
    }

    private static boolean checkVarDec(AbstractNode n, TreeEnv env, RTLNode rtlNode)
    {
    	try 
    	{
	    	DeclarationNode dn = new DeclarationNode(n);
	    	env.insert(dn.getDeclarator(), dn.getType(), dn.getPosition());

	    	// RTL stuff
	    	rtlNode.addChild(new RTLDeclarationNode(env, dn.getDeclarator(), dn.getType()));

	    } catch (SemanticException e) {
	    	System.out.println("*** " + e.getMessage() + " ***");
	    	return false;
	    }

    	return true;
    }

    private static boolean checkFunc(AbstractNode n, TreeEnv env, RTLNode rtlNode)
    {
    	try 
    	{
	    	FunctionNode fn = new FunctionNode(n);
			TreeEnv functionEnv = (TreeEnv)env.enter(fn.getFunctionDeclarator());

			RTLNode funcDecNode = new RTLFuncDecNode(env, fn.getFunctionDeclarator());
			rtlNode.addChild(funcDecNode);

			if(fn.getParameters().size() > 0)
			{
				if(fn.getParameters().get(0).getId() != Id.VOID) 
				{
					for(AbstractNode par : fn.getParameters())
					{
						//System.out.println("DEBUG: " + par.getDeclarator());
						if(checkVarDec(par, functionEnv, funcDecNode) == false) {
							System.out.println("Fail inserting: " + ((DeclarationNode)par).getDeclarator());
							return false;
						}
					}
				}
			}

			Type functionType = fn.getType();
			functionType.setParamsTable(functionEnv);

	    	env.insert(fn.getFunctionDeclarator(), functionType, fn.getPosition());
			
	    	if (fn.getFunctionBody() != null)
				return checkStmnt(fn.getFunctionBody(), functionEnv, funcDecNode);
			else
				return true;
	    } 
	    catch (SemanticException e) {
	    	System.out.println("*** " + e.getMessage() + " ***");
	    	return false;
	    }  	
    }

    private static boolean checkStmnt(AbstractNode stmnt, TreeEnv env, RTLNode rtlNode) 
    {
    	boolean statementFlag = true;

    	RTLStatementNode statementNode = new RTLStatementNode(env);
    	rtlNode.addChild(statementNode);

		for (AbstractNode n : stmnt.getChildren()) {
		    switch (n.getId()) 
			{
				case VARDEC:
					statementFlag &= checkVarDec(n, env, statementNode);
					break;
				case EMPTY_STMNT:
					break;
				case STMNT:
				case SIMPLE_COMPOUND_STMNT:
				case COMPOUND_STMNT:
					statementFlag &= checkStmnt(n, env, statementNode);
					break;
				case RETURN:
					try {
						// TODO: Check return type if inside other statement?
						RTLNode retNode = new RTLReturnNode(env);
						statementNode.addChild(retNode);
						if (checkExpType(n.getChild(0), n, env, retNode).getDataType() == env.lookup(env.getName()).getDataType()) {
							return true;	
						} else {
							System.out.println("Return type mismatch");
							return false;
						}
						
					} catch(Exception e) {
						System.out.println(e.getMessage());
						return false;
					} 
				default:
					statementFlag &= checkExp(n, stmnt, env, statementNode);
					break;
			}

			if(statementFlag == false)
				return false;
		}

		return statementFlag;
    }

    private static boolean checkExp (AbstractNode exp, AbstractNode parent, TreeEnv env, RTLNode rtlNode) 
    {
		switch (exp.getId()) {

			case INTEGER_LITERAL :
				rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.LITERAL, exp.getValue(), Type.DataType.INT));
			    return true;
			case CHAR_LITERAL:
				rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.LITERAL, exp.getValue(), Type.DataType.CHAR));
				return true;
			case IDENT :
				try{
			    	checkIdentifier(exp, env, rtlNode);
			    	return true;
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}
			
			case UNARY :
				try {
					checkExpType(exp.getChild(0), exp, env, rtlNode);
					return true;
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}

			case BINARY :
				try {
					checkExpType(exp, parent, env, rtlNode);
					return true;
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}

			case IF:
				try {
					// create rtl code
					RTLNode ifnode = new RTLifNode(env);
					rtlNode.addChild(ifnode);

					// Condition must be of type INT
					if (checkExpType(exp.getChild(0), exp, env, ifnode).getDataType() == Type.DataType.INT) 
					{
						// Check IF body
						boolean flag = true;
						int i = 0;
						for (AbstractNode n : exp.getChildren()) {
							// Skip condition
							if (++i == 1)
								continue;

							flag &= checkStmnt(n, env, ifnode);
						}

						return flag;
					} else {
						throw new SemanticException("Invalid expression type in condition");
					}
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}
			case WHILE:
				try {

					// create rtl code
					RTLWhileNode whileNode = new RTLWhileNode(env);
					rtlNode.addChild(whileNode);
					
					// Condition must be of type INT
					if (checkExpType(exp.getChild(0), exp, env, whileNode).getDataType() == Type.DataType.INT) 
					{
						// Check WHILE body
						boolean flag = true;
						int i = 0;
						for (AbstractNode n : exp.getChildren()) {
							if(++i == 1)
								continue;

							flag &= checkStmnt(n, env, whileNode);
						}

						return flag;
					} else {
						throw new SemanticException("Invalid expression type in condition");
					}
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}
			case PAREN:
				try {
	    			checkExpType(exp.getChild(0), exp, env, rtlNode);
	    			return true;
	    		} catch(Exception e) {
	    			System.out.println(e.getMessage());
					return false;
	    		}
			

			default :
				System.out.println("Invalid expression (" + exp.getId() + ")");
			    return false;
		}
    }

    private static Type checkIdentifier(AbstractNode exp, TreeEnv env, RTLNode rtlNode) throws SemanticException
    {
    	String name = exp.getValue();
	    Type type = env.lookup(name);
	    RTLNode funcCallNode = null;

	    if (type.getSymbolType() == Type.SymbolType.VAR)
	    	rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.VAR, name, type.getDataType()));
	    else if(type.getSymbolType() == Type.SymbolType.ARRAY) {
	    	// Be careful when we pass array as a func argument
	    	int arrSize;
	    	if (exp.numberOfChildren() > 0)
	    		arrSize = Integer.valueOf(exp.getChild(0).getValue());
	    	else
	    		arrSize = -1;

	    	rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.ARR, 
	    											name, arrSize, type.getDataType()));
	    } else {
	    	funcCallNode = new RTLFuncCallNode(env, name);
	    	rtlNode.addChild(funcCallNode);
	    }

	    if(exp.numberOfChildren() == 0 && type.getSymbolType() != Type.SymbolType.FUNC) {
	    	return type;
	    }

	    // Var can't have child nodes!
	    if (type.getSymbolType() == Type.SymbolType.VAR) {
	    	throw new SemanticException("Invalid use of identifier. Are you trying to access scalar value like an array or function?", exp.getPosition());
	    }

	    if (type.getSymbolType() == Type.SymbolType.ARRAY)
	    	return type;

	    if (exp.numberOfChildren() != type.getArraySize())
	    	throw new SemanticException("Parameter count mismatch " + exp.numberOfChildren() + " " + type.getArraySize(), exp.getPosition());

	    int i = 0;
	    for(AbstractNode child : exp.getChildren())
	    {
	    	Type pType = checkExpType(child, exp, env, funcCallNode);
	    	Type param = type.getParamsTable().lookup(i);

	    	if(pType.getDataType() != param.getDataType())
	    		throw new SemanticException("Parameter type mismatch", exp.getPosition());
	    	i++;
	    }

	    return type;
    }

    private static Type checkExpType(AbstractNode exp, AbstractNode parent, TreeEnv env, RTLNode rtlNode) throws SemanticException
    {
    	switch (exp.getId()) 
    	{
    		case PAREN:
    			return checkExpType(exp.getChild(0), exp, env, rtlNode);
    		case IDENT:
    			return checkIdentifier(exp, env, rtlNode);
			case UNARY:
				RTLExpressionNode unaryNode = new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.UNARY, exp.getValue(), Type.DataType.INT);
				rtlNode.addChild(unaryNode);

				{
					Type resultType = checkExpType(exp.getChild(0), exp, env, unaryNode);
					unaryNode.setResultDataType(resultType.getDataType());
	    			return resultType;	
				}
				
    		case BINARY:
    			// Check data types
    			RTLExpressionNode rtlChild = new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.BINARY, exp.getValue(), Type.DataType.INT);
    			rtlNode.addChild(rtlChild);
    			Type t1 = checkExpType(exp.getChild(0), exp, env, rtlChild);
    			Type t2 = checkExpType(exp.getChild(1), exp, env, rtlChild);

    			if (exp.getValue() == "=") 
    			{
    				if (t1.getSymbolType() == Type.SymbolType.CONST || t1.getSymbolType() == Type.SymbolType.FUNC)
    					throw new SemanticException("Invalid assignment", exp.getPosition());

    				// Assigning a = x[0]
    				if (t1.getSymbolType() == Type.SymbolType.ARRAY && exp.getChild(0).numberOfChildren() == 0
    					&& t2.getSymbolType() == Type.SymbolType.ARRAY && exp.getChild(1).numberOfChildren() == 0)
    					throw new SemanticException("Invalid assignment from array to array");

    				// Assigning int a[10]; a = 5;
    				if (t1.getSymbolType() == Type.SymbolType.ARRAY && exp.getChild(0).numberOfChildren() == 0
    					&& t2.getSymbolType() != Type.SymbolType.ARRAY)
    					throw new SemanticException("Invalid assignment to array type");

    				// Assigning int a[10]; a = 5;
    				if (t1.getSymbolType() != Type.SymbolType.ARRAY
    					&& t2.getSymbolType() == Type.SymbolType.ARRAY && exp.getChild(1).numberOfChildren() == 0)
    					throw new SemanticException("Invalid assignment of array type");
    			}

    			if (exp.getValue() != "=") {
    				if (exp.getChild(0).numberOfChildren() == 0 && t1.getSymbolType() == Type.SymbolType.ARRAY)
    					throw new SemanticException("Arithmetic/comparison on array not allowed");
    			}

    			Type resultType;

    			if (t1.getDataType() == t2.getDataType()) {
    				resultType = t1;
    			} else {
    				// Allow assignment from CHAR to INT
    				if (t1.getDataType() == Type.DataType.INT && t2.getDataType() == Type.DataType.CHAR)
    					resultType = t1;
    				else if (t1.getDataType() == Type.DataType.CHAR && t2.getDataType() == Type.DataType.INT)
    					resultType = t1;
    				else
    					throw new SemanticException("Type mismatch: " + t1.getDataType().toString() + ", " + t2.getDataType().toString(), exp.getPosition());
    			}

    			// Comparisons always result in INT
    			switch(exp.getValue()) {
    			case "<":
    			case ">":
    			case "<=":
    			case ">=":
    			case "==":
    			case "!=":
    				Type t = new Type(Type.SymbolType.CONST);
    				t.setDataType(Type.DataType.INT);
    				resultType = t;
    			default:
    				// Keep resultType;
    			}

    			rtlChild.setResultDataType(resultType.getDataType());

    			return resultType;


    		case INTEGER_LITERAL:
    			rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.LITERAL, exp.getValue(), Type.DataType.INT));
    			Type treti = new Type(Type.SymbolType.CONST);
    			treti.setDataType(Type.DataType.INT);
			    return treti;

			case CHAR_LITERAL:
				int asciiVal;

				if (exp.getValue().charAt(1) != '\\') {
					asciiVal = (int) exp.getValue().charAt(1);
				} else {
					switch(exp.getValue().charAt(2)) {
					case '0':
						asciiVal = 0;
						break;
					case 't':
						asciiVal = 9;
						break;
					case 'n':
						asciiVal = 10;
						break;
					case 'f':
						asciiVal = 12;
						break;
					case 'r':
						asciiVal = 13;
						break;
					default:
						throw new SemanticException("Unsupported escapte character \\" + exp.getValue().charAt(2));
					}
				}

				rtlNode.addChild(new RTLExpressionNode(env, RTLExpressionNode.ExpressionType.LITERAL, String.valueOf(asciiVal), Type.DataType.CHAR));
				Type tretc = new Type(Type.SymbolType.CONST);
    			tretc.setDataType(Type.DataType.CHAR);
			    return tretc;

			default:
				throw new SemanticException("Invalid expression (" + exp.getId() + ")");
			
    	}
    }

    private static boolean checkInclude(AbstractNode exp, TreeEnv env)
    {
    	IncludeFileNode ifn = new IncludeFileNode(exp);
    	// env.insert();

    	return true;
    }
}