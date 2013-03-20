package com.srts.phaseI.JPFlisteners;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.LocalVarInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL;
import gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.ExpressionType;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.MathRealExpression;
import gov.nasa.jpf.symbc.numeric.ObjectExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.SpecialExpressionFactory;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.srts.phaseI.path.Path;
import com.srts.phaseI.path.VSM;
import com.srts.phaseI.util.NumericUtils;
import com.srts.phaseI.util.Utility;


public class CoverageAnalyzer extends PropertyListenerAdapter{

	private Config conf;
	private JPF jpf;
	private boolean backtracked=false;
	private static LinkedHashSet<Instruction> statements; 
	private static Set<Path>  paths= new HashSet<Path>();
	private MethodInfo currentPublicMethodInfo;
	private static Path path;
	private static VSM vsm=null; 


	public CoverageAnalyzer(Config conf, JPF jpf) {
		this.conf=conf;
		this.jpf=jpf;
	}


	public static void flush(){
		paths=new HashSet<Path>();
	}

	public static Set<Path> getPaths(){
		return paths;
	}

	public static Path getCurrentPath(){
		return path;
	}

	public static LinkedHashSet<Instruction> getCurrentInsnSeq(){
		return statements;
	}
	private Set<String> paramSymNames;

	private Set<String> members;

	public void methodEntered(JVM vm) {  
		Instruction insn=vm.getLastInstruction();
		MethodInfo miThis=insn.getMethodInfo(); //expect the main method
		MethodInfo miLast=vm.getLastMethodInfo(); //expect the method invoked from main
		//System.out.println(" miThis:"+miThis + " miLast:" +miLast);
		
		if((insn instanceof INVOKEVIRTUAL //for methods that are invoked from main method other than <init> 
				|| insn instanceof INVOKESPECIAL) // for <init> invoked from main
				&& miThis.getName().contains("main") 
				&& !miLast.getName().contains("getInt")){// ignore getInt invoked from main
			currentPublicMethodInfo=miLast;
			System.out.println("Method entered to " + miLast.getFullName());
			statements=new LinkedHashSet<Instruction>();
			members=new HashSet<String>();
			paramSymNames=new HashSet<String>();
			path=new Path(currentPublicMethodInfo.getFullName());
			vsm=new VSM();
			/*
			 * Gather parameter names of the method into <code>paramSymNames</code>			 
			 */
			LocalVarInfo[] lv=currentPublicMethodInfo.getLocalVars();
			//			StackFrame sf=vm.getLastThreadInfo().getTopFrame();
			for (int j = 0; lv.length!=0 && j < currentPublicMethodInfo.getArgumentTypeNames().length; j++){ 
				/*System.out.println(Arrays.toString(lv));
				System.out.println("currentPublicMethodInfo.getArgumentTypeNames().length: "+ currentPublicMethodInfo.getArgumentTypeNames().length);
				System.out.println(vm.getLastThreadInfo().getStackTrace());

				System.out.println(lv[j+1].getSlotIndex());*/

				//String name=sf.getLocalAttr(lv[j+1].getSlotIndex()).toString();
				String name=lv[j+1].getName();
				paramSymNames.add(name); //.substring(0, name.indexOf("_SYM"))

			}

			/*
			 * Gather field names into  <code>members</code>.
			 */
			FieldInfo fields[]=currentPublicMethodInfo.getClassInfo().getDeclaredInstanceFields();
			//System.out.println("Field contents: "  + Arrays.toString(fields));
			if(fields!=null && fields.length !=0){
				for(FieldInfo field : fields){
					String name=field.getName();
					members.add(name);
					//System.out.println("Fieldname: "+name);
				}
			}
		}
	}


	public void stateBacktracked(Search search){
		//System.out.println("state backtracked!");
		//System.out.println(search.getVM().getLastThreadInfo().getStackTrace());
		backtracked=true;
	}


	/*	public void choiceGeneratorSet(JVM vm){
		System.out.println("cg set");
	}*/

	public void executeInstruction(JVM vm) {
		Instruction insn=vm.getLastInstruction();
		List<StackFrame> stack=vm.getLastThreadInfo().getStack();

		//System.out.println("+"+insn.getPosition() + ": " + insn.getMnemonic());
		/*condition: at least one public method (except Verify.getInt()) on top of synthetic main and actual main.*/
		boolean condition=(stack.size()>=3
				&& stack.get(1).getMethodInfo().getName().contains("main") 
				&& !stack.get(2).getMethodInfo().getFullName().contains("getInt")); 

		if(condition){ //condition3: at least one public method on top of synthetic main and actual main.
			/*
			 * if any of the members is updated 
			 * then mark a flag in vsm accordingly 
			 */
			if(insn instanceof PUTFIELD){
				FieldInfo fi=((PUTFIELD)insn).getFieldInfo();
				String fieldName=fi.getName();
				String type=fi.getType();
				StackFrame sf=vm.getLastThreadInfo().getTopFrame();
				Expression fieldVal=null;
				if(fi.is2SlotField()){
					fieldVal=(Expression)sf.getLongOperandAttr();
				}
				else{
					fieldVal=(Expression)sf.getOperandAttr();
				}
				if(fi.isReference()&&fieldVal==null){
					fieldVal=new ObjectExpression(null, null,fieldName);
				}
				if(fieldVal==null){
					if(type.equalsIgnoreCase("INT"))
						fieldVal=new IntegerConstant(sf.peek());
					else if(type.equalsIgnoreCase("CHAR"))
						fieldVal=new IntegerConstant(sf.peek());
					else if(type.equalsIgnoreCase("BYTE"))
						fieldVal=new IntegerConstant(sf.peek());
					else if(type.equalsIgnoreCase("BOOLEAN"))
						fieldVal=new IntegerConstant(sf.peek());
					else if(type.equalsIgnoreCase("LONG"))
						fieldVal=new RealConstant(sf.longPeek());
					else if(type.equalsIgnoreCase("FLOAT"))
						fieldVal=new RealConstant(sf.longPeek());
					else if(type.equalsIgnoreCase("DOUBLE"))
						fieldVal=new RealConstant(sf.longPeek());
				}
				System.out.println("===COvAnalyzer===");
				vm.getLastThreadInfo().getTopFrame().printStackContent();
				vsm.updateValue(fieldName, fieldVal);
				System.out.println("During putfield of "+fieldName+ " :"+ fieldVal);
				vsm.setUpdatedBy(fieldName, insn.getPosition(),insn.toString(),insn.getMethodInfo().getFullName());
			}
		}
	}

	public static void updateVSM(String fieldName, Expression fieldVal, int position, String insn, String methName){
		vsm.updateValue(fieldName, fieldVal);
		vsm.setUpdatedBy(fieldName, position,insn,methName);
	}

	public void instructionExecuted(JVM vm) {
		Instruction insn = vm.getLastInstruction();
		//System.out.println("-"+insn.getPosition() + ": " + insn.getMnemonic());
		//System.out.println("* " + insn.getPosition()+": " + insn +" " + insn.getMethodInfo().getName());
		//System.out.println(vm.getLastElementInfo().getClassInfo()+" "+ vm.getLastElementInfo());
		boolean condition1=false,condition2=false,condition3=false;
		if (!vm.getSystemState().isIgnored() 
				&& !insn.getMethodInfo().getName().contains("getInt")) {
			List<StackFrame > stack=vm.getLastThreadInfo().getStack();
			//System.out.println(currentPublicMethodInfo);
			//System.out.println(vm.getLastThreadInfo().getStackTrace());
			try {
				/*only synthetic main method in the stack, just after returning from main method.*/
				condition1=(stack.size()==1); 
				/*actual main method at top and size=2, just after returning from a public method.*/ 
				condition2=(stack.size()==2 && stack.get(1).getMethodInfo().getName().contains("main"));
				/*a public method on top of synthetic main and actual main.*/
				condition3=(stack.size()>=3 && stack.get(1).getMethodInfo().getName().contains("main") && stack.get(2).getMethodInfo()==currentPublicMethodInfo); //size >=2 and current public method at 1
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			//System.out.println("condition1: "+ condition1 + " condition2:"+ condition2+ " condition3:" + condition3);

			if(condition3){
				//vm.getLastThreadInfo().getTopFrame().printStackContent();
				System.out.println(insn.getPosition() + ": "+ insn);//+" visit Count: "+insn.getVisitCount() ); +" visit Count: "+insn.getVisitCount() + " Step: " + vm.getLastThreadInfo().isFirstStepInsn());
				
				
				
				/* At first add the statement to statement sequence of the path.*/
				statements.add(insn);


				if(path.isExceptionPath()){
					System.out.println("----CoverageAnalyzer.java:374 An Exception Path.");
					path.setVSM(vsm);
					vsm=new VSM();
					if(!currentPublicMethodInfo.isInit() && currentPublicMethodInfo.getReturnTypeCode()!=Types.T_VOID){
						path.setReturnVal(null);
					}
					ChoiceGenerator <?>cg = vm.getChoiceGenerator(); 
					if (!(cg instanceof PCChoiceGenerator)){
						ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
						while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
							prev_cg = prev_cg.getPreviousChoiceGenerator();
						}
						cg = prev_cg;
					}
					if ((cg instanceof PCChoiceGenerator) &&
							((PCChoiceGenerator) cg).getCurrentPC() != null){
						PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();

						Constraint now=pc.header,prev=pc.header;
						while(now!=null){
							if(now.getLeft().getType()==ExpressionType.UNKNOWN || now.getRight().getType()==ExpressionType.UNKNOWN){
								System.out.println("CoverageAnalyzer.java: Exception Handling! Need to delete unknown constraint.");
								System.out.println("--Before delete: "+ pc.header.toString().replaceAll("\n", " "));
								if(now==prev){
									pc.header=now.and;
								}
								else{
									prev.and=now.and;
								}
								now=now.and;
								System.out.println("--After delete: "+ pc.header.toString().replaceAll("\n", " "));
								continue;
							}
							NumericUtils.setExpressionType(now.getLeft());
							NumericUtils.setExpressionType(now.getRight());
							now.setConstraintType();
							System.out.println("CoverageAnalyzer.java: 395 " + now.toString().replaceAll("\n", " ") + " type: "+ now.getType());
							prev=now;
							now=now.and;
						}


						path.setPc(pc);
						System.out.println("------Setting PC: " + pc.toString().replace("\n", " "));
					}
					//statements.remove(insn);//remove the return instruction of main method
					LinkedHashSet<String> insnSeq=new LinkedHashSet<String>();
					Iterator<Instruction> it= statements.iterator();
					int count=0;
					while(it.hasNext()){
						Instruction in=it.next();
						if(count++ !=0)//do not insert the 1st instruction since it is invokevirtual from main.
							insnSeq.add(in.getPosition() +": " +in.toString());
					}

					path.setInsnSeq(insnSeq);
					Utility.printSet("------Setting insn seq", statements);

					if(currentPublicMethodInfo.isInit()){
						path.setAsConstructorPath();
					}
					paths.add(path);
					path=new Path(currentPublicMethodInfo.getFullName());
					vm.getSystemState().setIgnored(true);
					return;
				}
			}//if(condition3)


			if(condition2){
				/*
				 * Note: this portion will be executed after return from a method under symbolic execution.
				 */
				if(insn instanceof ReturnInstruction){
					statements.add(insn);
					path.setVSM(vsm);
					//System.out.println("CoverageAnalyzer.java: vsm set in "+path);
					vsm=new VSM(); /*there is a buggy story behind this*/


					/*
					 *  The return value of the method  will be added to the current instance of the path.
					 */
					//System.out.println("meth: "+ currentPublicMethodInfo.getName()+"isInit? "+ currentPublicMethodInfo.isInit());
					if(!currentPublicMethodInfo.isInit()){
						Expression lastReturnVal=(Expression)((ReturnInstruction)insn).getReturnAttr(vm.getLastThreadInfo());
						if(currentPublicMethodInfo.getReturnTypeCode()==Types.T_REFERENCE && lastReturnVal==null){
							lastReturnVal=SpecialExpressionFactory.getNullObjectExpression();
						}
						String returnType=currentPublicMethodInfo.getReturnTypeName();
						if(lastReturnVal==null){
							if(returnType.equalsIgnoreCase("INT"))
								lastReturnVal=new IntegerConstant((Integer)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
							else if(returnType.equalsIgnoreCase("CHAR"))
								lastReturnVal=new IntegerConstant((Integer)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
							else if(returnType.equalsIgnoreCase("BYTE"))
								lastReturnVal=new IntegerConstant((Integer)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
							else if(returnType.equalsIgnoreCase("BOOLEAN"))
								lastReturnVal=new IntegerConstant((Boolean)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo())==true?1:0);
							else if(returnType.equalsIgnoreCase("LONG"))
								lastReturnVal=new RealConstant((Long)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
							else if(returnType.equalsIgnoreCase("FLOAT"))
								lastReturnVal=new RealConstant((Float)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
							else if(returnType.equalsIgnoreCase("DOUBLE"))
								lastReturnVal=new RealConstant((Double)((ReturnInstruction)insn).getReturnValue(vm.getLastThreadInfo()));
						}
						NumericUtils.setExpressionType(lastReturnVal);
						//System.out.println("Setting returnval in the path: "+ lastReturnVal);
						path.setReturnVal(lastReturnVal);
					}

					/*
					 * Finalization of the path.
					 */
					//System.out.println("Finalizing the path: "+ path);
					ChoiceGenerator <?>cg = vm.getChoiceGenerator();
					if (!(cg instanceof PCChoiceGenerator)){
						ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
						while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
							prev_cg = prev_cg.getPreviousChoiceGenerator();
						}
						cg = prev_cg;
					}
					if ((cg instanceof PCChoiceGenerator) &&
							((PCChoiceGenerator) cg).getCurrentPC() != null){
						PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();

						System.out.println("Setting Expression types in PC");
						Constraint now=pc.header,prev=pc.header;
						while(now!=null){
							if(now.getLeft().getType()==ExpressionType.UNKNOWN || now.getRight().getType()==ExpressionType.UNKNOWN){
								System.out.println("CoverageAnalyzer.java: Need to delete unknown constraint.");
								System.out.println("--Before delete: "+ pc.header.toString().replaceAll("\n", " "));
								if(now==prev){
									pc.header=now.and;
								}
								else{
									prev.and=now.and;
								}
								now=now.and;
								System.out.println("--After delete: "+ pc.header.toString().replaceAll("\n", " "));
								continue;
							}
							NumericUtils.setExpressionType(now.getLeft());
							NumericUtils.setExpressionType(now.getRight());
							now.setConstraintType();
							prev=now;
							now=now.and;
						}


						path.setPc(pc);
						System.out.println("Setting PC: " + pc.toString().replace("\n", " "));
					}
					//statements.remove(insn);//remove the return instruction of main method
					LinkedHashSet<String> insnSeq=new LinkedHashSet<String>();
					Iterator<Instruction> it= statements.iterator();
					int count=0;
					while(it.hasNext()){
						Instruction in=it.next();
						if(count++ !=0)//do not insert the 1st instruction since it is invokevirtual from main.
							insnSeq.add(in.getPosition() +": " +in.toString());
					}

					path.setInsnSeq(insnSeq);
					Utility.printSet("Setting insn seq", statements);


					paths.add(path);

					if(currentPublicMethodInfo.isInit())
						path.setAsConstructorPath();

					path=new Path(currentPublicMethodInfo.getFullName());
					//System.out.println("CoverageAnalyzer.java new Path: "+ path);
				}/*if(insn instanceof ReturnInstruction)*/

			}/*if(condition2)*/

			/*
			 * Note: A backtrack is happened to a previous state after executing the final return statement of the control flow. 
			 * The execution flow is backtracked to a conditional jump statement to execute remaining choices in that statement.
			 * When a backtrack occurs we remove the instructions from the statement sequence which occur later 
			 * than the backtracked instruction.
			 */
			if(backtracked){
				if(true){
					ArrayList<Instruction> al= new ArrayList<Instruction>();
					al.addAll(statements);
					System.out.println("backtracked: "+insn+" "+al.size());
					//System.out.println("backtracked!");
					int foundAt=-1;
					int size=al.size();
					for(int i=0;i<size;i++){
						if(foundAt == -1 && insn.equals(al.get(i))){
							foundAt=i+1;
							//System.out.println("Found at: " + foundAt);
						}
						else if(foundAt!=-1){
							Instruction ins=al.remove(foundAt);
							/* During removal we make them unvisited*/
							ins.visit(null);
						}
						else{
							/*
							 * Instructions which occur before the backtracked instruction are
							 * visited by the current path.
							 */
							al.get(i).visit(path);
						}
					}
					statements=new LinkedHashSet<Instruction>();
					statements.addAll(al);
				}/*if(condition1)*/
				backtracked=false;
			}/*if(backtracked)*/
		}/*if (!vm.getSystemState().isIgnored())*/
		else{
			if(!insn.getMethodInfo().equals(currentPublicMethodInfo))
				System.out.println("CoverageAnalyzer.java!!! Instruction from non-supported method: "+insn + " " + insn.getMethodInfo() + " currentPublicMethodInfo:" + currentPublicMethodInfo );
			else
				System.out.println("CoverageAnalyzer.java!!! State Ignored.");
		}
	}

}
