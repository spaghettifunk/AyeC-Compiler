package rtl;

import java.util.*;
import java.io.*; 

public class RTLtoMIPS
{
	private String fileName = "";
	private String intermediateCode = "";

	private String currentRegister = "";
	private String dataAsm = "";
	private String textAsm = "";
	private String mipsCode = "";
	private String lastLabel = null;

	// State vars
	private boolean insideFunc = false;
	private boolean prologueComplete = false;
	private String funcName = "";
	private int frameSize;

	public RTLtoMIPS(String fn, String ic)
	{
		assert fn != "";
		assert ic != "";

		this.fileName = fn;
		this.intermediateCode = ic;
	}

	public String produceMIPSCode() throws IOException
	{
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("results/" + this.fileName + ".asm"), "utf-8"))) 
		{
			// example how to write in the new file
			String mipsCode = parseIntermediateCode();
    		writer.write(mipsCode);
    		return mipsCode;

		} catch (IOException ex) {
    		System.out.println(ex.getMessage());
		} catch (MIPSException ex) {
			System.out.println(ex.getMessage());
		}

		return "";
	}

	private String parseIntermediateCode() throws MIPSException 
	{
		MipsEnv env = new MipsEnv();

		String[] lines = this.intermediateCode.split("\\n");

		for (String line : lines)
		{
			if (lastLabel != null && lastLabel.startsWith("func_")) {
				if (insideFunc) {
					// If there was no return but we find next function, 
					// enter new environment and do clean-up
					textAsm += generateEpilogue();
					env = env.exit();
					env = env.enter();
				} else {
					// We came from global scope
					env = env.enter();
					insideFunc = true;
					funcName = lastLabel;
				}

				prologueComplete = false;

				textAsm += "\n";
				if (lastLabel.equals("func_main")) {
					textAsm += "main:\n";
					textAsm += lastLabel + ":\n";
				} else {
					textAsm += lastLabel + ":\n";
				}

				lastLabel = null;
			} else if(lastLabel != null && 
					 (lastLabel.startsWith("loop") || 
					  lastLabel.startsWith("else") || 
					  lastLabel.startsWith("end")))
			{
				textAsm += lastLabel + ":\n";
				lastLabel = null;
			}

			String delims = "[ ]+";
			String[] tokens = line.split(delims);
			String rtlInstr = tokens[0];

			String mipsInstruction = "";

			// Need to take care of prologue after all declarations
			if (insideFunc && !prologueComplete && !rtlInstr.equals("declare")) {
				

				// Generate syscall bodies
				if (funcName.equals("func_putstring")) {
					textAsm += generatePrologue(env);
					textAsm += generatePutstringFunc();
				} else if (funcName.equals("func_putint")) {
					// Do nothing
				} else if (funcName.equals("func_getstring")){
					textAsm += generatePrologue(env);
					textAsm += generateGetstringFunc();
				} else {
					textAsm += generatePrologue(env);
				}

				prologueComplete = true;
			}

			// predefined instructions
			switch(rtlInstr)
			{
				// loads and stores
				case "load":
					textAsm += getLoad(tokens);
					break;
				case "store":
					textAsm += getStore(tokens, env);
					break;

				// arithmetic
				case "add":
					textAsm += getAddInstructions(tokens, env);
					break;
				case "sub":
					textAsm += getSubInstructions(tokens, env);
					break;
				case "mul":
					textAsm += getMulInstructions(tokens, env);
					break;
				case "div":
					textAsm += getDivInstructions(tokens, env);
					break;

				// variable declaration
				case "var":
				case "arr":
					dataAsm += parseGlobalDeclare(tokens, env);
					break;
				case "declare":
					textAsm += parseDeclare(tokens, env);
					break;

				// function
				case "call":
					textAsm += parseCall(tokens, env);
					break;
				case "jump":
					textAsm += getJumpInstruction(tokens);
					break;
				case "return":
					textAsm += parseReturn(tokens, env);
					break;
				case "endfunc":
					exitFunc(env);
					break;

				case "sret":
					textAsm += parseStoreReturn(tokens, env);
					break;
				case "main:":
					textAsm += "";
					break;

				// branching
				case "slt":
					textAsm += getSetLessThan(tokens, env);
					break;
				case "beq":
					textAsm += getBranchEqual(tokens, env);
					break;
				case "bne":
					textAsm += getBranchNotEqual(tokens, env);
					break;

				// for all the others							
				default:
					textAsm += getMIPSInstruction(tokens);
					break;																					
			}
		}

		if (insideFunc == true && lastLabel != null){
			textAsm += lastLabel + ":\n";
			lastLabel = null;
		}

		if (insideFunc) {
			// We parsed everything but ended up in function scope
			exitFunc(env);
		}

		mipsCode = ".data\n" + dataAsm;
		mipsCode += ".space 1\n";
		mipsCode += ".text\n" +  textAsm;

		assert mipsCode != "";

		return mipsCode;
	}

	private void exitFunc(MipsEnv env) 
	{
		if (funcName.equals("func_putint")
			|| funcName.equals("func_getint")) {
			// Do nothing
		} else {
			textAsm += generateEpilogue();
		}
		
		env = env.exit();
		insideFunc = false;
	}

	private String generatePrologue(MipsEnv env)
	{
		String textAsm = "";

		frameSize = env.getLocalVarOffset(); // offset for local vars
		frameSize += 9 * MipsConstants.RegisterSize; // $s0-$s7 and $fp
		
		// Align frame size to multiple words
		int mod = frameSize % 4;
		if (mod != 0) {
			frameSize += 4 - mod;
		}

		textAsm += "\n#prologue start\n";
		// Set up frame
		textAsm += "subu $sp, $sp, " + frameSize + "\n";
		textAsm += "sw $fp, " + (frameSize - MipsConstants.RegisterSize * 9) +"($sp)\n";
		textAsm += "addiu $fp, $sp, " + (frameSize - MipsConstants.RegisterSize) + "\n";

		// Save $s0-$s7
		textAsm += "sw $s0, 0($fp)\n";
		textAsm += "sw $s1, -4($fp)\n";
		textAsm += "sw $s2, -8($fp)\n";
		textAsm += "sw $s3, -12($fp)\n";
		textAsm += "sw $s4, -16($fp)\n";
		textAsm += "sw $s5, -20($fp)\n";
		textAsm += "sw $s6, -24($fp)\n";
		textAsm += "sw $s7, -28($fp)\n";
		textAsm += "#prologue end\n\n";

		return textAsm;
	}

	private String generateEpilogue() 
	{
		String asm = "\n" + funcName + "_end:\n";
		asm += "\n#epilogue start\n";

		// Print for debug
		// asm += "li $v0, 1\n";
		// asm += "lw $a0, -44($fp)\n";
		// asm += "syscall\n";

		// Save $s0-$s7
		asm += "lw $s0, 0($fp)\n";
		asm += "lw $s1, -4($fp)\n";
		asm += "lw $s2, -8($fp)\n";
		asm += "lw $s3, -12($fp)\n";
		asm += "lw $s4, -16($fp)\n";
		asm += "lw $s5, -20($fp)\n";
		asm += "lw $s6, -24($fp)\n";
		asm += "lw $s7, -28($fp)\n";

		// Restore $sp and $fp
		asm += "addiu $sp, $sp, " + frameSize + "\n";
		asm += "lw $fp, -32($fp)\n";

		asm += "jr $ra\n";
		asm += "#epilogue end\n\n";

		return asm;
    }

    private String generatePutstringFunc() {
    	String asm = "";

    	// ASM magic!
    	asm += "#putstring syscall\n";

    	asm += "#get string length\n";
    	asm += "lw $t0, -36($fp)\n";
    	asm += "li $t1, 0\n";
    	asm += "li $t2, 0\n";
    	asm += "\nputstring_len_start:\n";
    	asm += "lb $t3, 0($t0)\n";
    	asm += "beq $t2, $t3, putstring_len_end\n";
    	asm += "addiu $t0, $t0, -1\n";
    	asm += "addiu $t1, $t1, 1\n";
    	asm += "j putstring_len_start\n";
    	asm += "\nputstring_len_end:\n";
    	asm += "addiu $t1, $t1, 1\n";

    	asm += "#make room on the stack for reverse string\n";
    	asm += "move $t4, $sp\n";
    	asm += "move $t5, $t1\n";
    	asm += "addiu $t4, $t4, -1\n";
    	asm += "subu $sp, $sp, $t1\n";

		asm += "#copy reverse string onto stack\n";
    	asm += "\nputstring_copy_start:\n";
    	asm += "beqz $t5, putstring_copy_end\n";
    	asm += "lb $t3, 0($t0)\n";
    	asm += "sb $t3, 0($t4)\n";
    	asm += "addiu $t0, $t0, 1\n";
    	asm += "addiu $t4, $t4, -1\n";
    	asm += "addiu $t5, $t5, -1\n";
    	asm += "j putstring_copy_start\n";
    	asm += "\nputstring_copy_end:\n";

    	asm += "#perform syscall on reversed string\n";
    	asm += "move $a0, $sp\n";
    	asm += "li $v0, 4\n";
		asm += "syscall\n";

    	asm += "#free stack space\n";
    	asm += "addu $sp, $sp, $t1\n";

    	return asm;
    }

    private String generateGetstringFunc() {
    	String asm = "";

    	// ASM magic!
    	asm += "#getstring syscall\n";

    	asm += "#create 1k buffer on the stack\n";
    	asm += "subu $sp, $sp, 1024\n";

    	asm += "#perform syscall and read into buffer\n";
    	asm += "move $a0, $sp\n";
    	asm += "li $a1, 1024\n";
    	asm += "li $v0, 8\n";
		asm += "syscall\n";

    	asm += "#get string length\n";
    	asm += "move $t0, $sp\n";
    	asm += "li $t1, 0\n";
    	asm += "li $t2, 0\n";
    	asm += "\ngetstring_len_start:\n";
    	asm += "lb $t3, 0($t0)\n";
    	asm += "beq $t2, $t3, getstring_len_end\n";
    	asm += "addiu $t0, $t0, 1\n";
    	asm += "addiu $t1, $t1, 1\n";
    	asm += "j getstring_len_start\n";
    	asm += "\ngetstring_len_end:\n";
    	asm += "addiu $t1, $t1, 1\n";

    	// asm += "#make room on the stack for reverse string\n";
    	asm += "move $t0, $sp\n";
    	asm += "lw $t4, -36($fp)\n";
    	asm += "move $t5, $t1\n";
    	// asm += "addiu $t4, $t4, -1\n";
    	// asm += "subu $sp, $sp, $t1\n";

		asm += "#copy reverse string to result array\n";
    	asm += "\ngetstring_copy_start:\n";
    	asm += "beqz $t5, getstring_copy_end\n";
    	asm += "lb $t3, 0($t0)\n";
    	asm += "sb $t3, 0($t4)\n";
    	asm += "addiu $t0, $t0, 1\n";
    	asm += "addiu $t4, $t4, -1\n";
    	asm += "addiu $t5, $t5, -1\n";
    	asm += "j getstring_copy_start\n";
    	asm += "\ngetstring_copy_end:\n";

    	

    	asm += "#free stack space\n";
    	asm += "addiu $sp, $sp, 1024\n";

    	return asm;
    }

	private String parseGlobalDeclare(String[] tokens, MipsEnv env) throws MIPSException 
	{
		String name = lastLabel;
		int elementSize;
		int bytes;

		Type.DataType dt;
		switch(tokens[1]) {
		case "INT":
			dt = Type.DataType.INT;
			elementSize = MipsConstants.IntSize;
			break;
		case "CHAR":
			dt = Type.DataType.CHAR;
			elementSize = MipsConstants.CharSize;
			break;
		default:
			throw new MIPSException("Unknown data type in declaration");
		}

		Type.SymbolType st;
		int arrSize = -1;
		switch(tokens[0]) {
		case "var":
			st = Type.SymbolType.VAR;
			bytes = elementSize;
			break;
		case "arr":
			st = Type.SymbolType.ARRAY;
			arrSize = Integer.valueOf(tokens[2]);
			bytes = elementSize * arrSize;
			break;
		default:
			throw new MIPSException("Unknown symbol type in declaration");
		}

		env.insertGlobal(name, dt, st, arrSize);

		String asm =  ".space " + bytes + "\n" + lastLabel + ":\n";
		// asm += ".space 1\n";
		// asm += ".align 2\n";
		lastLabel = null;

		return asm;
	}

	private String parseDeclare(String[] tokens, MipsEnv env) throws MIPSException 
	{
		String name = tokens[1];
		int elementSize;
		int bytes;

		Type.DataType dt;
		switch(tokens[3]) {
		case "INT":
			dt = Type.DataType.INT;
			elementSize = MipsConstants.IntSize;
			break;
		case "CHAR":
			dt = Type.DataType.CHAR;
			elementSize = MipsConstants.CharSize;
			break;
		default:
			throw new MIPSException("Unknown data type in declaration");
		}

		Type.SymbolType st;
		int arrSize = -1;
		switch(tokens[2]) {
		case "var":
			st = Type.SymbolType.VAR;
			bytes = elementSize;
			break;
		case "arr":
			st = Type.SymbolType.ARRAY;
			arrSize = Integer.valueOf(tokens[4]);

			// If array is a param, we only need a pointer
			if (name.startsWith("param")) {
				bytes = MipsConstants.IntSize;
			} else{
				bytes = elementSize * arrSize;

				// Align to word size
				int mod = bytes % 4;
				if (mod != 0) {
					bytes += 4 - mod;
				}
			}

			break;
		default:
			throw new MIPSException("Unknown symbol type in declaration");
		}

		env.insert(name, dt, st, arrSize);
		env.addLocalVarOffset(bytes);

		return "";
	}

	private String getSetLessThan(String[] tokens, MipsEnv env)
	{
		/*  how slt works:
		    if (comparisonLeft < comparisonRight)
				t0 = 1;
		    else 
		   		t0 = 0;
		*/

		String comparisonLeft = getLoad(tokens[2], 0, env) + "\n";			// loaded in $s0
		String comparisonRight = getLoad(tokens[3], 1, env) + "\n";			// loaded in $s1

		String loadedRegisters = comparisonLeft + comparisonRight;
		String sltCode = loadedRegisters + "slt $t0, $s0, $s1 \n";			// save in temporary
		return sltCode + "sw $t0, " + env.lookupAddress(tokens[1]) + "\n";	// store back the temp value
	}

	private String getBranchEqual(String[] tokens, MipsEnv env)
	{
		String valLeft = getLoad(tokens[1], 0, env) + "\n";		// loaded in $s0
		String valRight = getLoad(tokens[2], 1, env) + "\n";	// loaded in $s1
		String jumpLabel = tokens[3];

		String loadedRegisters = valLeft + valRight;

		return loadedRegisters + "beq $s0, $s1, " + jumpLabel + "\n";
	}

	private String getBranchNotEqual(String[] tokens, MipsEnv env)
	{
		String valLeft = getLoad(tokens[1], 0, env) + "\n"; 	// loaded in $s0
		String valRight = getLoad(tokens[2], 1, env) + "\n";	// loaded in $s1
		String jumpLabel = tokens[3];

		String loadedRegisters = valLeft + valRight;

		return loadedRegisters + "bne $s0, $s1, " + jumpLabel + "\n";
	}

	private String parseCall(String[] tokens, MipsEnv env) 
	{
		String ident = tokens[1].substring(0, tokens[1].indexOf('('));
		String argStr = tokens[1].substring(tokens[1].indexOf('(') + 1, tokens[1].length() - 1);
		String[] argList = argStr.split(",", 0);

		// Inline some of the syscalls
		if (ident.equals("putint"))
			return syscallPutint(argList[0], env);
		else if (ident.equals("getint"))
			return syscallReadint();

		String asm = "";

		// Initial offset
		// = $s0-$s7 + $fp + 4 bytes as $sp still points above future frame
		int spOffset = MipsConstants.RegisterSize * 9 + 4;

		// Place arguments into frame
		for (String arg : argList) {
			// Check if we have no args
			if (arg.equals(""))
				break;

			if (isInteger(arg)) {
				asm += "li $t0, " + arg + "\n";
				asm += "sw $t0, -" + spOffset + "($sp)\n";
				spOffset += MipsConstants.IntSize;
			} else {
				// Handle array identifiers
				String argIdent;
				int arrIndex = -1;

				if (arg.contains("(")) {
					int index = arg.indexOf("(");
					argIdent = arg.substring(0, index);
					arrIndex = Integer.valueOf(arg.substring(index + 1, arg.length() - 1));
				} else {
					argIdent = arg;	
				}

				Type.DataType dataType = env.lookupDataType(argIdent);
				Type.SymbolType symType = env.lookupSymbolType(argIdent);

				if(symType == Type.SymbolType.ARRAY && arrIndex == -1){
					// We're passing an array by reference
					// Place array address on frame
					asm += "la $t0, " + env.lookupAddress(argIdent) + "\n";
					asm += "sw $t0, -" + spOffset + "($sp)\n";
					spOffset += MipsConstants.IntSize;
				} else if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
					// Get array value by index and place on frame
					asm += offsetArray(argIdent, arrIndex, "$t1", "$t2", env);

					if(dataType == Type.DataType.CHAR) {
						asm += "lb $t0, 0($t1)\n";
						asm += "sb $t0, -" + spOffset + "($sp)\n";
						spOffset += MipsConstants.CharSize;
					} else if(dataType == Type.DataType.INT) {
						asm += "lw $t0, 0($t1)\n";
						asm += "sw $t0, -" + spOffset + "($sp)\n";
						spOffset += MipsConstants.IntSize;
					}
				} else {
					if(dataType == Type.DataType.CHAR) {
						asm += "lb $t0, " + env.lookupAddress(argIdent) + "\n";
						asm += "sb $t0, -" + spOffset + "($sp)\n";
						spOffset += MipsConstants.CharSize;
					} else if(dataType == Type.DataType.INT) {
						asm += "lw $t0, " + env.lookupAddress(argIdent) + "\n";
						asm += "sw $t0, -" + spOffset + "($sp)\n";
						spOffset += MipsConstants.IntSize;
					}
				}
			}
		}

		asm += "move $s7, $ra\n";
		asm += "jal func_" + ident + "\n";
		asm += "move $ra, $s7\n";
		return asm;
	}

	private String syscallPutint(String arg, MipsEnv env) 
	{
		String asm = "";

		if (isInteger(arg)) {
			asm += "li $t0, " + arg + "\n";
			asm += "move $t0, $a0\n";
			asm += "li $v0, 1\n";
		} else {
			// Handle array identifiers
			String argIdent;
			int arrIndex = -1;

			if (arg.contains("(")) {
				int index = arg.indexOf("(");
				argIdent = arg.substring(0, index);
				arrIndex = Integer.valueOf(arg.substring(index + 1, arg.length() - 1));
			} else {
				argIdent = arg;	
			}

			Type.DataType dataType = env.lookupDataType(argIdent);
			Type.SymbolType symType = env.lookupSymbolType(argIdent);

			if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
				asm += offsetArray(argIdent, arrIndex, "$t0", "$t1", env);

				if(dataType == Type.DataType.CHAR) {
					asm += "lb $a0, 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					asm += "lw $a0, 0($t0)\n";
				}
			} else if(symType == Type.SymbolType.ARRAY && arrIndex == 0 && env.lookupIsReference(argIdent)) {
				asm += "lw $t0, " + env.lookupAddress(argIdent) + "\n";

				if(dataType == Type.DataType.CHAR) {
					asm += "lb $a0, 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					asm += "lw $a0, 0($t0)\n";
				}
			} else {
				if(dataType == Type.DataType.CHAR) {
					asm += "lb $a0, " + env.lookupAddress(argIdent) + "\n";
				} else if(dataType == Type.DataType.INT) {
					asm += "lw $a0, " + env.lookupAddress(argIdent) + "\n";
				}
			}

			asm += "li $v0, 1\n";
		}

		asm += "syscall\n";
		return asm;
	}

	private String syscallReadint() {
		String asm = "li $v0, 5\n";
		asm += "syscall\n";
		return asm;
	}

	private String syscallReadstring(String[] args, MipsEnv env) 
	{
		String asm = "";

		// First arg
		String argIdent;
		int arrIndex = -1;
		String arg = args[0];

		if (arg.contains("(")) {
			int index = arg.indexOf("(");
			argIdent = arg.substring(0, index);
			arrIndex = Integer.valueOf(arg.substring(index + 1, arg.length() - 1));
		} else {
			argIdent = arg;	
		}

		if (env.lookupIsReference(argIdent)) {
			asm += "lw $a0, " + env.lookupAddress(argIdent) + "\n";
		} else {
			asm += "la $a0, " + env.lookupAddress(argIdent) + "\n";
		}

		// Second arg
		arg = args[1];
		if (arg.contains("(")) {
			int index = arg.indexOf("(");
			argIdent = arg.substring(0, index);
			arrIndex = Integer.valueOf(arg.substring(index + 1, arg.length() - 1));
		} else {
			argIdent = arg;	
		}


		if (isInteger(arg)) {
			asm += "li $a0, " + arg + "\n";
		} else {
			// Handle array identifiers
			if (arg.contains("(")) {
				int index = arg.indexOf("(");
				argIdent = arg.substring(0, index);
				arrIndex = Integer.valueOf(arg.substring(index + 1, arg.length() - 1));
			} else {
				argIdent = arg;	
			}

			Type.DataType dataType = env.lookupDataType(argIdent);
			Type.SymbolType symType = env.lookupSymbolType(argIdent);

			if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
				// Get array value by index
				asm += offsetArray(argIdent, arrIndex, "$t1", "$t2", env);
				asm += "lw $a1, 0($t1)\n";
			} else {
				asm += "lw $a0, " + env.lookupAddress(argIdent) + "\n";
			}
		}

		asm += "li $v0, 8\n";
		asm += "syscall\n";
		return asm;
	}

	// specify all the other instructions
	private String getMIPSInstruction(String[] tokens)
	{
		// handling labels
		if(tokens[0].toLowerCase().contains(":"))
		{
			lastLabel = tokens[0].replace(":", "");
			return "";
		}

		return "";
	}

	// Jump instruction
	private String getJumpInstruction(String[] tokens)
	{
		return "j " + tokens[1] + "\n";	// go to label
	}

	// Arithmetic instructions
	// TODO: check the store instruction
	private String getAddInstructions(String[] tokens, MipsEnv env)
	{
		String loadInstructions = "";
		String addInstructions = "";

		loadInstructions = getLoad(tokens[1], 0, env) + "\n";
		addInstructions = "add " + "$s2, " + getRegister() + ", ";

		loadInstructions += getLoad(tokens[2], 1, env) + "\n";
		addInstructions += getRegister() + "\n";

		// store value
		addInstructions += "sw " + "$s2, " + env.lookupAddress(tokens[3]) + "\n";
		return loadInstructions + addInstructions;
	}

	private String getSubInstructions(String[] tokens, MipsEnv env)
	{
		String loadInstructions = "";
		String subInstructions = "";
		
		loadInstructions = getLoad(tokens[1], 0, env) + "\n";
		subInstructions = "sub " + "$s2, " + getRegister() + ", ";

		loadInstructions += getLoad(tokens[2], 1, env) + "\n";
		subInstructions += getRegister() + "\n";

		// store value
		subInstructions += "sw " + "$s2, " + env.lookupAddress(tokens[3]) + "\n";
		return loadInstructions + subInstructions;
	}

	private String getMulInstructions(String[] tokens, MipsEnv env)
	{
		String loadInstructions = "";		
		String mulInstructions = "";

		loadInstructions = getLoad(tokens[1], 0, env) + "\n";
		mulInstructions = "mul " + "$s2, " +  getRegister() + ", ";

		loadInstructions += getLoad(tokens[2], 1, env) + "\n";
		mulInstructions += getRegister() + "\n";

		// store value
		mulInstructions += "sw " + "$s2, " + env.lookupAddress(tokens[3]) + "\n";
		return loadInstructions + mulInstructions;
	}

	private String getDivInstructions(String[] tokens, MipsEnv env)
	{
		String loadInstructions = "";		
		String divInstructions = "";

		loadInstructions = getLoad(tokens[1], 0, env) + "\n";
		divInstructions = "div " + "$s2, " +  getRegister() + ", ";

		loadInstructions += getLoad(tokens[2], 1, env) + "\n";
		divInstructions += getRegister() + "\n";

		// store value
		divInstructions += "sw " + "$s2, " + env.lookupAddress(tokens[3]) + "\n";
		return loadInstructions + divInstructions;
	}	

	// Load instructions
	private String getLoad(String[] tokens)
	{
		return "";
	}

	// TODO: modify when load is not an integer
	private String getLoad(String token, int temporaryCounter, MipsEnv env)
	{
		String loadCode = "";
		currentRegister = "$s" + temporaryCounter;
		if(isInteger(token) == true)
		{
			loadCode += "li " + currentRegister + ", " + token;
		}
		else 
		{
			String argIdent;
			int arrIndex = -1;

			if (token.contains("(")) {
				int index = token.indexOf("(");
				argIdent = token.substring(0, index);
				arrIndex = Integer.valueOf(token.substring(index + 1, token.length() - 1));
			} else {
				argIdent = token;	
			}

			Type.DataType dataType = env.lookupDataType(argIdent);
			Type.SymbolType symType = env.lookupSymbolType(argIdent);
			
			if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
				loadCode += offsetArray(argIdent, arrIndex, "$t0", "$t1", env);

				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb " + currentRegister + ", 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw " + currentRegister + ", 0($t0)\n";
				}
			} else if(symType == Type.SymbolType.ARRAY && arrIndex == 0 && env.lookupIsReference(argIdent)) {
				loadCode += "lw $t0, " + env.lookupAddress(argIdent) + "\n";

				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb " + currentRegister + ", 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw " + currentRegister + ", 0($t0)\n";
				}
			} else {
				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb " + currentRegister + ", " + env.lookupAddress(argIdent) + "\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw " + currentRegister + ", " + env.lookupAddress(argIdent) + "\n";
				}
			}
		}

		return loadCode;
	}

	// store instructions
	private String getStore(String[] tokens, MipsEnv env)
	{
		String loadCode = "";
		String storeCode = "";

		String getValueFrom;
		String storeValueTo;

		int getValueFromIndex = -1;
		int storeValueToIndex = -1;

		// Handle array identifiers
		if (tokens[1].contains("(")) {
			int index = tokens[1].indexOf("(");
			getValueFrom = tokens[1].substring(0, index);
			getValueFromIndex = Integer.valueOf(tokens[1].substring(index + 1, tokens[1].length() - 1));
		} else {
			getValueFrom = tokens[1];	
		}

		if (tokens[2].contains("(")) {
			int index = tokens[2].indexOf("(");
			storeValueTo = tokens[2].substring(0, index);
			storeValueToIndex = Integer.valueOf(tokens[2].substring(index + 1, tokens[2].length() - 1));
		} else {
			storeValueTo = tokens[2];	
		}

		// Load source value into $s0
		if(isInteger(getValueFrom) == true)
		{
			loadCode += "li $s0" + ", " + getValueFrom + "\n";
		} else {
			Type.SymbolType symType = env.lookupSymbolType(getValueFrom);
			Type.DataType dataType = env.lookupDataType(getValueFrom);
			String ident = getValueFrom;
			int arrIndex = getValueFromIndex;

			if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
				loadCode += offsetArray(ident, arrIndex, "$t0", "$t1", env);

				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb $s0, 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw $s0, 0($t0)\n";
				}
			} else if(symType == Type.SymbolType.ARRAY && arrIndex == 0 && env.lookupIsReference(ident)) {
				loadCode += "lw $t0, " + env.lookupAddress(ident) + "\n";

				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb $s0, 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw $s0, 0($t0)\n";
				}
			} else {
				if(dataType == Type.DataType.CHAR) {
					loadCode += "lb $s0, " + env.lookupAddress(ident) + "\n";
				} else if(dataType == Type.DataType.INT) {
					loadCode += "lw $s0, " + env.lookupAddress(ident) + "\n";
				}
			}
		}

		Type.SymbolType symType = env.lookupSymbolType(storeValueTo);
		Type.DataType dataType = env.lookupDataType(storeValueTo);
		String ident = storeValueTo;
		int arrIndex = storeValueToIndex;

		if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
			storeCode += offsetArray(ident, arrIndex, "$t0", "$t1", env);

			if(dataType == Type.DataType.CHAR) {
				storeCode += "sb $s0, 0($t0)\n";
			} else if(dataType == Type.DataType.INT) {
				storeCode += "sw $s0, 0($t0)\n";
			}
		} else if(symType == Type.SymbolType.ARRAY && arrIndex == 0 && env.lookupIsReference(ident)) {
			storeCode += "sw $t0, " + env.lookupAddress(ident) + "\n";

			if(dataType == Type.DataType.CHAR) {
				storeCode += "sb $s0, 0($t0)\n";
			} else if(dataType == Type.DataType.INT) {
				storeCode += "sw $s0, 0($t0)\n";
			}
		} else {
			if(dataType == Type.DataType.CHAR) {
				storeCode += "sb $s0, " + env.lookupAddress(ident) + "\n";
			} else if(dataType == Type.DataType.INT) {
				storeCode += "sw $s0, " + env.lookupAddress(ident) + "\n";
			}
		}

		return loadCode + storeCode;
	}

	private String parseReturn(String[] tokens, MipsEnv env) 
	{
		String asm = "";

		String expr = tokens[1];

		if (isInteger(expr)) {
			asm += "li $v0, " + expr + "\n";
		} else {
			String ident;
			int arrIndex = -1;

			if (expr.contains("(")) {
				int index = expr.indexOf("(");
				ident = expr.substring(0, index);
				arrIndex = Integer.valueOf(expr.substring(index + 1, expr.length() - 1));
			} else {
				ident = expr;	
			}

			Type.DataType dataType = env.lookupDataType(ident);
			Type.SymbolType symType = env.lookupSymbolType(ident);

			if(symType == Type.SymbolType.ARRAY && arrIndex != 0){
				asm += offsetArray(ident, arrIndex, "$t0", "$t1", env);

				if(dataType == Type.DataType.CHAR) {
					asm += "lb $v0, 0($t0)\n";
				} else if(dataType == Type.DataType.INT) {
					asm += "lw $v0, 0($t0)\n";
				}
			} else {
				if(dataType == Type.DataType.CHAR) {
					asm += "lb $v0, " + env.lookupAddress(ident) + "\n";
				} else if(dataType == Type.DataType.INT) {
					asm += "lw $v0, " + env.lookupAddress(ident) + "\n";
				}
			}
		}

		asm += "j " + funcName + "_end\n";

		return asm;
	}

	private String parseStoreReturn(String[] tokens, MipsEnv env) 
	{
		String asm = "";

		String arrExpr = tokens[1];
		String ident;
		int arrIndex = -1;

		if (arrExpr.contains("(")) {
			int index = arrExpr.indexOf("(");
			ident = arrExpr.substring(0, index);
			arrIndex = Integer.valueOf(arrExpr.substring(index + 1, arrExpr.length() - 1));
		} else {
			ident = arrExpr;	
		}

		Type.DataType dataType = env.lookupDataType(ident);
		Type.SymbolType symType = env.lookupSymbolType(ident);

		if(symType == Type.SymbolType.ARRAY && arrIndex != 0) {
			asm += offsetArray(ident, arrIndex, "$t0", "$t1", env);

			if(dataType == Type.DataType.CHAR) {
				asm += "sb $v0, 0($t0)\n";
			} else if(dataType == Type.DataType.INT) {
				asm += "sw $v0, 0($t0)\n";
			}
		} else {
			if(dataType == Type.DataType.CHAR) {
				asm += "sb $v0, " + env.lookupAddress(ident) + "\n";
			} else if(dataType == Type.DataType.INT) {
				asm += "sw $v0, " + env.lookupAddress(ident) + "\n";
			}
		}

		return asm;
	}

	// getting the current register
	private String getRegister()
	{
		return currentRegister;
	}

	private boolean isInteger(String s) throws NumberFormatException
	{
	    try 
	    { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}

	private String offsetArray(String ident, int index, String target, String tmpReg, MipsEnv env) {
		String asm = "";

		String addr = env.lookupAddress(ident);
		Type.DataType type = env.lookupDataType(ident);
		int offset;

		if (type == Type.DataType.INT)
			offset = MipsConstants.IntSize;
		else if (type == Type.DataType.CHAR)
			offset = MipsConstants.CharSize;
		else
			offset = MipsConstants.CharSize;

		offset *= index;

		boolean isReference = env.lookupIsReference(ident);

		if (isReference)
			asm += "lw " + target + ", " + addr + "\n";
		else
			asm += "la " + target + ", " + addr + "\n";

		asm += "li " + tmpReg + ", " + offset + "\n";
		asm += "subu " + target + ", " + target + ", " + tmpReg + "\n";

		return asm;
	}
}