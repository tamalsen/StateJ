package com.srts.phaseI.JPFlisteners;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.bytecode.IFNONNULL;
import gov.nasa.jpf.jvm.bytecode.IFNULL;
import gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL;
import gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.NEW;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils.InstructionOrSuper;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils.VarType;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.ExpressionType;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.ObjectExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SpecialExpressionFactory;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.srts.phaseI.classfile.TypeHierarchy;
import com.srts.phaseI.statemodel.StateModel;
import com.srts.phaseI.statemodel.StateModelRegister;
import com.srts.phaseI.statemodel.Transition;
import com.srts.phaseI.statemodel.TransitionIn;
import com.srts.phaseI.statemodel.TransitionOut;
import com.srts.phaseI.util.NumericUtils;
import com.srts.phaseI.util.StringUtils;

public class InterObjectMethodInvocationListener extends PropertyListenerAdapter{

	/**
	 * Naming convention of ref variables:
	 * Local:
	 * Parameter:
	 * Member:
	 * 
	 */

	/**
	 * Other Classes that helps in Object Manipulation:
	 * 
	 * gov.nasa.jpf.symbc.bytecode.BytecodeUtils: InstructionOrSuper execute() 
	 * 			During invoking a symbolic method, if a parameter is ref, a new object is created of
	 * 			a type specified in parameter. A default object expression is created of the form   
	 * 			<param name>$#<local var index> and added as attribute to the parameter.
	 *   
	 *   		Same thing is done for member objects using 
	 *   		gov.nasa.jpf.symbc.bytecode.Helper.initializeInstanceField()

	 * 			For non-concrete method invocation, if an object is passed as actual param, the actual argument
	 * 			is manipulated to convert it into formal arguments. The reason is <local var index> differs.
	 * 
	 * gov.nasa.jpf.symbc.bytecode.INVOKEVIRTUAL
	 * 			Registers choice generators
	 * 
	 * gov.nasa.jpf.symbc.bytecode.INVOKESPECIAL
	 * 			Registers choice generators
	 */

	private void setReturnValueUnknown(ThreadInfo th,int returnType){
		switch(returnType){
		case Types.T_ARRAY:
			System.out.println("--InterObjectMethodInvocationListener.java:: Error! Return type: ARRAY");
			break;
		case Types.T_REFERENCE:
			System.out.println("--InterObjectMethodInvocationListener.java::Return type: REFERENCE");
			th.push(0);
			th.setOperandAttr(SpecialExpressionFactory.getUnknownObjectExpression());
			break;
		case Types.T_SHORT:
		case Types.T_INT:
		case Types.T_CHAR:
		case Types.T_BYTE:
		case Types.T_BOOLEAN:
			th.push(0);
			th.setOperandAttr(SpecialExpressionFactory.getUnknownSymbolicInteger());
			break;
		case Types.T_LONG:
		case Types.T_DOUBLE:
		case Types.T_FLOAT:
			th.longPush(Types.doubleToLong(0.0));
			th.setOperandAttr(SpecialExpressionFactory.getUnknownSymbolicReal());
			break;
		case Types.T_VOID:
			break;
		default:
			System.out.println("--InterObjectMethodInvocationListener.java !!error, unknown return type: "+ returnType);
		}
	}

	private Expression getUnknownExpression(int returnType){
		switch(returnType){
		case Types.T_ARRAY:
			System.out.println("--InterObjectMethodInvocationListener.java:: Error! Return type: ARRAY");
			break;
		case Types.T_REFERENCE:
			System.out.println("--InterObjectMethodInvocationListener.java::Return type: REFERENCE");
			return (SpecialExpressionFactory.getUnknownObjectExpression());
		case Types.T_SHORT:
		case Types.T_INT:
		case Types.T_CHAR:
		case Types.T_BYTE:
		case Types.T_BOOLEAN:
			return (SpecialExpressionFactory.getUnknownSymbolicInteger());
		case Types.T_LONG:
		case Types.T_DOUBLE:
		case Types.T_FLOAT:
			return (SpecialExpressionFactory.getUnknownSymbolicReal());
		case Types.T_VOID:
			break;
		default:
			System.out.println("--InterObjectMethodInvocationListener.java !!error, unknown type: "+ returnType);
			return null;
		}
		return null;
	}

	/**
	 * @param guard
	 * @param paramNames
	 * @param paramValues
	 * @return null if not satisfiable, a constraint if conditionally satisfiable or a "CONST_1==CONST_1" if unConditionally satisfiable 
	 */
	private Constraint getConsIfSatisfy(Constraint guard, String[] paramNames, Expression[] paramValues){
		Map<Expression, Expression> pValMap = new HashMap<Expression, Expression>();
		for(int i=0;i<paramNames.length;i++){
			String name=paramNames[i];
			Expression value=paramValues[i];
			boolean unknown=false;
			if(value.getType()==ExpressionType.UNKNOWN){
				unknown=true;
			}
			Expression e=null;
			if(value instanceof RealExpression){
				e=new SymbolicReal(name);
				pValMap.put(e, unknown?null:value);
			} 
			else if(value instanceof IntegerExpression){
				e=new SymbolicInteger(name);
				pValMap.put(e, unknown?null:value);
			}
			else if(value instanceof ObjectExpression){
				Expression e1=unknown?SpecialExpressionFactory.getUnknownObjectExpression():  new SymbolicInteger(name+"("+(((ObjectExpression)value).getInstanceOf())+","+(((ObjectExpression)value).getState()));
				Expression e2=(new IntegerConstant(1));
				pValMap.put(e1, e2);
			}
			else {
				System.out.println("InterObjectMethodInvocationListener.java:133 Error: other than Integer, Real or Object expression!!" + e);
			}
		}
		System.out.println("INTEROBJ...Java:145 "+ pValMap);
		return NumericUtils.getConsIfSatisfy(guard, pValMap);
	}

	public static boolean isSymbolic(JVM vm){
		List<StackFrame> stack=vm.getLastThreadInfo().getStack();

		/*condition: at least one public method (except Verify.getInt()) on top of synthetic main and actual main.*/
		boolean condition=(stack.size()>=3
				&& stack.get(1).getMethodInfo().getName().contains("main") 
				&& !stack.get(2).getMethodInfo().getFullName().contains("getInt")); 
		return condition;
	}
	public static String varName(String name, byte type,int counter) {
		String suffix = "";
		switch (type) {
		case Types.T_SHORT:
		case Types.T_INT:
		case Types.T_CHAR:
		case Types.T_BYTE:
		case Types.T_BOOLEAN:
			suffix = "_SYMINT";
			break;
		case Types.T_DOUBLE:
		case Types.T_FLOAT:
		case Types.T_LONG:
			suffix = "_SYMREAL";
			break;
		case Types.T_REFERENCE:
			suffix = "_SYMREF";
			break;
		default:
			throw new RuntimeException("Unhandled SymVarType: " + type);
		}
		return name + "_" + counter + suffix;
	}
	
	public void executeInstruction(JVM vm) {
		Instruction insn=vm.getLastInstruction();
		ThreadInfo th=vm.getLastThreadInfo();

		/*condition: at least one public method (except Verify.getInt()) on top of synthetic main and actual main.*/
		boolean condition=isSymbolic(vm);

		/*System.out.println("#."+ insn.getPosition() + ": "+insn );
		th.getTopFrame().printStackContent();*/


		if(condition && insn instanceof InvokeInstruction){
			if(!((InvokeInstruction)insn).getInvokedMethod().getClassName().equals(insn.getMethodInfo().getClassInfo().getName())){

				System.out.println("---------Handling inter object method invocation---------");

				//th.getTopFrame().printStackContent();

				/*getting the reference value from the obj reference in the stack*/
				System.out.println("----Obtaining reference value from the obj reference from the stack.");
				int idx=((InvokeInstruction)insn).getInvokedMethod().getArgumentsSize()-1;
				String refName= th.getOperandAttr(idx)==null?null:th.getOperandAttr(idx).toString();
				ElementInfo thisEi=th.peek(idx)==-1?null:th.getElementInfo(th.peek(idx));
				String refInstanceOf=thisEi==null?null:thisEi.getType().substring(1).replaceAll("/",".").replace(";", "");
				String refState=thisEi.getStringField("$state");
				if(refState.equals("null")){
					refState=null;
				}
				System.out.println("refName: " + refName + " refInstanceOf:" + refInstanceOf + " refState: " + refState);

				final MethodInfo caller=insn.getMethodInfo();
				/*The method that is going to get invoked*/
				final MethodInfo calee=((InvokeInstruction)insn).getInvokedMethod();

				/*The class encloses the called method*/
				final ClassInfo  caleeClass=calee.getClassInfo();

				System.out.println("InterObjectMethodInvocationListener.java:: "+caller.getFullName() + " is invoking " + calee.getFullName() + " return type: "+ ((InvokeInstruction)insn).getReturnType());

				/*
				 * Assuming that you never instantiate your super class, 
				 * so that the super-class.<init> can only appear as the first statement of derived-class.init  
				 */
				if(calee.isInit() && caller.getClassInfo().getSuperClass(caleeClass.getName())!=null){
					System.out.println("--It is an superclass <init>");
					refInstanceOf=caleeClass.getName();
					refState="0";
				}

				/*
				 * If a method is invoked on an object for which the state/type is unknown:
				 * the method is not executed at all, return value is set to unknown.
				 */
				if((refState!=null && refState.equals(StringUtils.VALUE_UNKNOWN) ) 
						|| (refInstanceOf!=null && refInstanceOf.equals(StringUtils.VALUE_UNKNOWN))){
					System.out.println("The method is invoked on an unknown object, treating accordingly..");
					th.clearOperandStack();
					int returnType=((InvokeInstruction)insn).getReturnType();
					setReturnValueUnknown(th,returnType);

					/*A hack to tell invoke instruction not to execute concretely*/
					insn.setAttr(new Object[]{});

					return;
				}


				boolean isInit=false;
				if(((InvokeInstruction)insn).getInvokedMethod().isInit()){
					isInit=true;
					System.out.println("InterObjectMethodInvocationListener.java:: It is an <init>...");
					refState="0";
				}

				if(!th.isFirstStepInsn()){
					/*Creating a hierarchy of classes that have been already loaded.*/
					TypeHierarchy.buildTypeHierarchy(th);

					/*
					 * Getting the subclasses of the class of the receiver object
					 */
					String[] subClasses=TypeHierarchy.getTypeElements(refInstanceOf).toArray(new String[0]);

					/*
					 * But if it is a constructor invocation, there is only one option.
					 */
					if(isInit || refState!=null){ 
						subClasses=new String[]{refInstanceOf};
						/*if((insn.getPrev().getPrev() instanceof  NEW)){
							subClasses=new String[]{refInstanceOf};
						}                                         
						else{ //super() invocation
							subClasses=new String[]{nci.getName()};
						}*/
					}

					System.out.println("InterObjectMetodInvocationListener.java:86 subclasses:" + Arrays.toString(subClasses));


					/*
					 * We save all choices in the instruction attribute, so that we can retrieve them later.
					 */
					Object[][] tmpChoices=new Object[subClasses.length][]; 
					int rowCount=0,choiceCount=0;
					for(String cls:subClasses){
						System.out.println("For subclass: "+ cls);	

						///////Even if refState is non-null, can cause multiple transitions//////
						/*
						 * If object is concrete, no need to  create choice on object type, since there is only one option.
						 */
						/*if(refState!=null && !refInstanceOf.equals(cls)){
							System.out.println("-- is concrete.");
							continue;
						}*/
						////////////////////////////////////////////////////////////////////////


						if(cls.equals("null")){
							tmpChoices[rowCount++]=new Transition[]{null};
							choiceCount ++;
							continue;
						}

						/*Search for the state model of the class. If not found, assume default state model.*/
						boolean defaultSm=false;
						StateModel sm=StateModelRegister.getStateModel(cls);
						if(sm==null){
							sm=StateModel.getDefaultStateModel();
							defaultSm=true;
							System.out.println("--state model is not available. Assuming default state model.");
						}

						/*--Getting transitions related to the invoked method--*/
						System.out.println("--getting transitions that can be triggered by the invoked method ");
						Set<Transition> trans= sm.getTransitions(refState,calee.getName()+ calee.getSignature(), null);
						for(Transition t: trans){
							System.out.println("-----Found: "+ t); 
						}
						tmpChoices[rowCount++] =trans.toArray(new Transition[0]);

						/*
						 * If default state model is assumed, there is no information about which method is causing 
						 * which transition. In that case, a dummy transition is created.
						 */
						if(defaultSm){ /*then there must be only one row*/
							Transition t= (Transition)tmpChoices[rowCount-1][0];
							if(t.getTransitionIn().getStimuli()==null){
								TransitionIn tempTin=new TransitionIn(t.getTransitionIn().getState(), calee.getFullName().replace(caleeClass.getName(), cls), t.getTransitionIn().getGuard());
								Expression returnVal=getUnknownExpression(((InvokeInstruction)insn).getReturnType());
								TransitionOut tempTout=new TransitionOut(t.getTransitionOut().getState(), returnVal);
								t=new Transition(tempTin, tempTout, t.getStmtCoverage(), t.getId());
							}
							tmpChoices[rowCount-1][0]=t;
						}
						choiceCount += trans.size();
					}

					System.out.println("\nNo of subClasses: "+ (rowCount));
					System.out.println("Total no of choices: "+ choiceCount+"\n");

					Object[][] choices=Arrays.copyOfRange(tmpChoices, 0, rowCount);
					System.out.println("--choices: "+Arrays.toString(choices));
					/*setting instruction attribute, used in INVOKEVIRTUAL/INVOKESPECIAL.java*/
					((InvokeInstruction)insn).setAttr(choices);
				}

				if(th.isFirstStepInsn()){

					System.out.println("\n\nSecond time onwards.."); 
					/*having a look on the current choice generator*/
					ChoiceGenerator<?> cg=null;
					int conditionValue=-1;
					SystemState ss=vm.getSystemState();
					cg=ss.getChoiceGenerator();
					//System.out.println("GOT a choice generator: "+ cg);
					if(!(cg instanceof PCChoiceGenerator)){
						System.out.println("InterObjectMethodInvocationListener.java:: Error! Expected PCChoiceenerator.");
					}
					conditionValue = (Integer)cg.getNextChoice();
					System.out.println("----Current choice condValue: "+  conditionValue);

					/* Retrieve choices saved in the instruction attribute */
					Object[][] attr=(Object[][])insn.getAttr();

					/*Mapping choice value to row-column pair*/
					int r=0,c=0;
					while(true){
						if(conditionValue >= attr[r].length){
							conditionValue=conditionValue-attr[r].length;
							r++;
						}
						else{
							c=conditionValue;
							break;
						}
					}
					System.out.println("----Choice value to r,c: "+ r +"," + c);
					Transition thisTran=(Transition)attr[r][c];

					/*Getting everything from the transition*/
					System.out.println("----Current transition: "+ thisTran);

					String srcState=null,tarState=null;
					Constraint guard=null;
					Expression returnVal=null;
					String tranClassName=null;

					if(thisTran != null){
						srcState=thisTran.getTransitionIn().getState().getId();
						tarState=thisTran.getTransitionOut().getState().getId();
						guard=thisTran.getTransitionIn().getGuard();
						returnVal=thisTran.getTransitionOut().getAllowedReturnVal();
						tranClassName=thisTran.getTransitionIn().getStimuli();
						tranClassName=tranClassName.substring(0,tranClassName.lastIndexOf("."));
						/*
						 * If the transition source state does not match current state of the object, 
						 * nothing to do with that transition, just backtrack. 
						 */
						System.out.println("------Source State: " + srcState);
						System.out.println("------Target State: " + tarState);
						System.out.println("------Guard: " + guard);

						/*
						 * Tf the transition is an exception transition, 
						 */
						/*if(!tarState.isEmpty() && Integer.parseInt(tarState)<0){
							CoverageAnalyzer.getCurrentPath().setUncaughtException("java.lang.NullPointerException", insn.getPosition());
						}*/

						if(refState != null  
								&& !refState.equals(srcState)) {
							System.out.println("------Source state does not match. Backtracking..");
							vm.getSystemState().setIgnored(true);
						}

					}
					else{// This is a null pointer exception
						tranClassName="null";
						CoverageAnalyzer.getCurrentPath().setUncaughtException("java.lang.NullPointerException", insn.getPosition());
					}



					
					
					/*--Getting symbolic values of the arguments as well as removing arguments from stack--*/
					
					System.out.println("----Getting and removing arguments from the stack: " + th.getTopFrame().hashCode());
					th.getTopFrame().printStackContent();

					Constraint modifiedGuard=null;
					StackFrame sf = th.getTopFrame();
					byte argTypes[]=calee.getArgumentTypes();
					int argCount=argTypes.length;
					if(argCount>0){
						argCount = argCount + 1;
						System.out.println("argTypes: " + Arrays.toString(argTypes));
						Object [] args =sf.getArgumentAttrs(calee);
						Expression[] argValueArray=new Expression[argCount-1];
						int topPos=sf.getTopPos();
						int conArgPointer=topPos;
						for(int i=1;i<argCount;i++){
							/* Note: sf.getArgumentAttrs and sf.peek iterates from different ends of the stack frame.*/
							int argType=argTypes[argCount-i-1]; /*argCount-2,argCount-3,argCount-4 ... 0*/
							Expression si=(args==null)?null:(Expression)args[argCount-i];/*argCount-1,argCount-2,argCount-3 ... 1*/
							switch(argType){
							case Types.T_ARRAY:
								System.out.println("InterObjectMethodInvocationListener.java:: error!! argument type is array!");
								break;
							case Types.T_REFERENCE:
								System.out.println("InterObjectMethodInvocationListener.java:: error!! argument type is reference!");
								break;
							case Types.T_LONG:
								si=(si==null)?(si =new RealConstant(sf.longPeek(topPos-conArgPointer))):si;
								conArgPointer-=2;
								th.longPop();
								break;
							case Types.T_SHORT:
							case Types.T_INT:
							case Types.T_CHAR:
							case Types.T_BYTE:
							case Types.T_BOOLEAN:
								si=(si==null)?(si =new IntegerConstant(sf.peek(topPos-conArgPointer))):si;
								conArgPointer-=1;
								th.pop();
								break;
							case Types.T_DOUBLE:
							case Types.T_FLOAT:
								si=(si==null)?(si =new RealConstant(Types.longToDouble(sf.longPeek(topPos-conArgPointer)))):si;
								conArgPointer-=2;
								th.longPop();
								break;
							default:
								System.out.println("InterObjectMethodInvocationListener.java:: error, unknown argument type!");
							}
							argValueArray[argCount-1-i]=si;
						}
						System.out.println("------Arguments Values:" +Arrays.toString(argValueArray));



						/*
						 * check whether argument values satisfy the guard condition or not. 
						 */
						String argNames[]= new String [argCount-1];
						String[] localVarNames=calee.getLocalVariableNames();
						int ct=0; /*taking 'this' into account*/
						for(int i=1;i<localVarNames.length;i++){
							System.out.println("--"+ localVarNames[i]);
							argNames[i-1]=varName(localVarNames[i],argTypes[i-1],i);//.substring(0,localVarNames[i].indexOf('$'));
						}
						if(argNames.length!=0){
							modifiedGuard=getConsIfSatisfy(guard, argNames, argValueArray);
							System.out.println("-----modified guard: " + modifiedGuard);
							if(modifiedGuard==null){ //not satisfiable
								System.out.println("------Guard Unsat. Backtracking..");
								vm.getSystemState().setIgnored(true);
							}
						}

						System.out.println("------Argument Names:" +Arrays.toString(argNames));
					}else{
						System.out.println("----NO Argument is there.");
					}

					/*pop the obj reference*/
					th.pop();

					/**
					 * Space for concrete execution.
					 */

					System.out.println("----Obtaining and manipulating return value:" + th.getTopFrame().hashCode());
					th.getTopFrame().printStackContent();
					/*--Manipulating stack to push return type--*/
					int returnType=((InvokeInstruction)insn).getReturnType();
					if(thisTran==null){
						setReturnValueUnknown(th,returnType);
					}
					else{
						System.out.println("------returnVal : " +returnVal);
						switch(returnType){
						case Types.T_ARRAY:
							System.out.println("InterObjectMethodInvocationListener.java:: Error! Return type: ARRAY");
							break;
						case Types.T_REFERENCE:
							System.out.println("InterObjectMethodInvocationListener.java::Return type: REFERENCE");
							th.push(0);
							th.setOperandAttr(returnVal);
							break;
						case Types.T_SHORT:
						case Types.T_INT:
						case Types.T_CHAR:
						case Types.T_BYTE:
						case Types.T_BOOLEAN:
							int constInt=0;
							if(returnVal instanceof IntegerConstant){
								constInt=((IntegerConstant)returnVal).value;
							}
							th.push(constInt);
							th.setOperandAttr(returnVal);
							break;
						case Types.T_LONG:
						case Types.T_DOUBLE:
						case Types.T_FLOAT:
							double constD=0.0;
							if(returnVal instanceof RealConstant){
								constD=((RealConstant)returnVal).value;
							}
							th.longPush(Types.doubleToLong(constD));
							th.setOperandAttr(returnVal);
							break;
						case Types.T_VOID:
							System.out.println("------Return type void");
							break;
						default:
							System.out.println("InterObjectMethodInvocationListener.java !!error, unknown return type: "+ returnType);
						}
					}
					System.out.println("After pushing returned expression: "+ th.getTopFrame().hashCode());
					th.getTopFrame().printStackContent();


					System.out.println("----Choice Generator Manipulation");
					ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
					while (!((prev_cg == null|| (prev_cg instanceof PCChoiceGenerator)))) {
						prev_cg = prev_cg.getPreviousChoiceGenerator();
					}
					PathCondition pc=new PathCondition();
					if (prev_cg != null)
						pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();
					
					
					/*
					 * Choice Generator Manipulation to encode it in path condition if the refValue 
					 * is uninitialized (parameter / member objects)
					 */
					if(refState==null){  
						
						if(refName==null){
							System.out.println("-----!!refName null is not expected!");
						}
						
						
						/*Determine the expression type*/ 
						int type=ExpressionType.MDL;

						/*Set Path Condition*/
						Expression ex1=new SymbolicInteger(refName+"("+(tranClassName)+","+(srcState)+")");
						ex1.setType(type);
						pc._addDet(Comparator.EQ,ex1,new IntegerConstant(1));
						pc.header.setConstraintType();
					}

					if(modifiedGuard!=null && modifiedGuard instanceof Constraint){
						pc._addDet(modifiedGuard.getComparator(),modifiedGuard.getLeft(),modifiedGuard.getRight());
					}
					else if(!(modifiedGuard instanceof Constraint)){
						System.out.println("WARNING!!! GOUARD is not Constraint!! :" + modifiedGuard);
					}
					
					System.out.println("------Final PC:" + pc);
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					
					
					

					/*
					 * Update the ref state to target state of the transition
					 */
					if(thisTran!=null){
						System.out.println("----settiong final state: " + tarState);
						int di=vm.getHeap().newString(tarState, th);
						thisEi.setReferenceField("$state", di);
					}
				}
				System.out.println("---------IOMI Handling Completed---------");
			}
		}
	}
}
