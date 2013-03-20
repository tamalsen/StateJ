package com.srts.phaseI.JPFlisteners;

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ASTORE;
import gov.nasa.jpf.jvm.bytecode.DSTORE;
import gov.nasa.jpf.jvm.bytecode.FSTORE;
import gov.nasa.jpf.jvm.bytecode.IINC;
import gov.nasa.jpf.jvm.bytecode.ISTORE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.LSTORE;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.SpecialExpressionFactory;

import java.util.List;
import java.util.Set;

public class LoopHandlingListener extends PropertyListenerAdapter{
	@Override
	public void methodEntered(JVM vm) {  
		Instruction insn=vm.getLastInstruction();
		ThreadInfo th=vm.getLastThreadInfo();
		List<StackFrame> stack=vm.getLastThreadInfo().getStack();
		boolean condition=(stack.size()>=3
				&& stack.get(1).getMethodInfo().getName().contains("main") 
				&& !stack.get(2).getMethodInfo().getFullName().contains("getInt"));
		/*
		 * During any method invocation clear visit status of the instructions in the method
		 */
		if(condition){
			MethodInfo mi=vm.getLastMethodInfo();
			Instruction[] insns=mi.getInstructions();
			for(Instruction in:insns){
				in.visit(null);
			}
		}
	}

	@Override
	public void executeInstruction(JVM vm) {
		Instruction insn=vm.getLastInstruction();
		ThreadInfo th=vm.getLastThreadInfo();
		List<StackFrame> stack=vm.getLastThreadInfo().getStack();

		/*condition: at least one public method (except Verify.getInt()) on top of synthetic main and actual main.*/
		boolean condition=(stack.size()>=3
				&& stack.get(1).getMethodInfo().getName().contains("main") 
				&& !stack.get(2).getMethodInfo().getFullName().contains("getInt")); 

		if(condition){
			//System.out.println("~"+ insn.getPosition() + ": "+ insn + " visit Count: " + insn.getVisitCount());
		}
		String str=(String)vm.getConfig().get("loop_depth");
		int loopDepth=0;
		if(str!=null){
			loopDepth=Integer.parseInt(str);
		}
		
		if(condition && !th.isFirstStepInsn() && insn.getVisitCount()>loopDepth){
			System.out.println("LoopHandlingListener.java::17 Loop detected " + insn + "visit count: " + insn.getVisitCount());
			CoverageAnalyzer.getCurrentPath().setAsLoopPath();
			Set<Instruction> insnSeq=CoverageAnalyzer.getCurrentInsnSeq();
			boolean posFound=false;
			Instruction lastInsnInLoop=insn;

			/****PC REMOVAL******/
			/*boolean pcFound=false;
			PathCondition pcBeforeLoop=null;*/

			System.out.println("----Loop Body starts==>");
			for(Instruction in:insnSeq){
				if(!posFound && !in.equals(insn)){
					continue;
				}
				else{
					posFound=true;
					System.out.println("------"+ in);
					if(in.getPosition()>lastInsnInLoop.getPosition()){
						lastInsnInLoop=in;
					}

					/****PC REMOVAL******/
					/*if(!pcFound && insnPCMap.get(in)!=null){
						pcBeforeLoop=insnPCMap.get(in).prev;
						pcFound =true;
						System.out.println("LoopHandlingListener.java::64 restoring pc to pre-loop pc: " + pcBeforeLoop);
					}*/


					if(in instanceof PUTFIELD){
						FieldInfo fi=((PUTFIELD)in).getFieldInfo();
						String type=fi.getType();
						ElementInfo ei=th.getElementInfo(th.getThis());
						if(type.equalsIgnoreCase("INT") 
								|| type.equalsIgnoreCase("CHAR")
								|| type.equalsIgnoreCase("BYTE")
								|| type.equalsIgnoreCase("BOOLEAN")
								|| type.equalsIgnoreCase("LONG")){
							Expression e=SpecialExpressionFactory.getUnknownSymbolicInteger();
							ei.setFieldAttr(fi,e);
							CoverageAnalyzer.updateVSM(fi.getName(), e, in.getPosition(), in.getMnemonic(), in.getMethodInfo().getFullName());
						}
						else if(type.equalsIgnoreCase("FLOAT")
								|| type.equalsIgnoreCase("DOUBLE")){
							Expression e=SpecialExpressionFactory.getUnknownSymbolicReal();
							ei.setFieldAttr(fi,e);
							CoverageAnalyzer.updateVSM(fi.getName(), e, in.getPosition(), in.getMnemonic(), in.getMethodInfo().getFullName());
						}
						else if(fi.isReference()){
							Expression e=SpecialExpressionFactory.getUnknownObjectExpression();
							ei.setFieldAttr(fi,e);
							CoverageAnalyzer.updateVSM(fi.getName(), e, in.getPosition(), in.getMnemonic(), in.getMethodInfo().getFullName());
						}
					}
					else if(in instanceof ISTORE){
						int lvi=((ISTORE)in).getLocalVariableIndex();
						th.setLocalAttr(lvi, SpecialExpressionFactory.getUnknownSymbolicInteger());
					}
					else if(in instanceof LSTORE){
						int lvi=((LSTORE)in).getLocalVariableIndex();
						th.setLocalAttr(lvi, SpecialExpressionFactory.getUnknownSymbolicInteger());
					}
					else if(in instanceof FSTORE){
						int lvi=((FSTORE)in).getLocalVariableIndex();
						th.setLocalAttr(lvi, SpecialExpressionFactory.getUnknownSymbolicReal());
					}
					else if(in instanceof DSTORE){
						int lvi=((DSTORE)in).getLocalVariableIndex();
						th.setLocalAttr(lvi, SpecialExpressionFactory.getUnknownSymbolicReal());
					}
					else if(in instanceof ASTORE){
						int lvi=((ASTORE)in).getLocalVariableIndex();
						th.setLocalAttr(lvi, SpecialExpressionFactory.getUnknownObjectExpression());
					}
					else if(in instanceof IINC){
						int lvi=((IINC)in).getIndex();
						th.setLocalAttr(lvi,SpecialExpressionFactory.getUnknownSymbolicInteger());
					}
					else if(insn.getAttr() instanceof Object[]){
						System.out.println("----Definitely a inter-object-method-invocation!");
						//make them unknown too 
					}
				}
			}
			System.out.println("----Loop Body ends==>");


			/****PC REMOVAL******/
			/*ChoiceGenerator<?> cg=vm.getSystemState().getChoiceGenerator();
			if (!(cg instanceof PCChoiceGenerator)){
				ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
				while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
					prev_cg = prev_cg.getPreviousChoiceGenerator();
				}
				cg = prev_cg;
			}
			if(cg instanceof PCChoiceGenerator){
				((PCChoiceGenerator)cg).setCurrentPC(pcBeforeLoop);
			}
			else{
				try {
					throw new Exception("PCCG Expected!!");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/

			System.out.println("----Jumping to "+ ": "+ lastInsnInLoop.getNext());  /* + lastInsnInLoop.getNext().getPosition()*/ 
			th.skipInstruction(insn);
			th.clearOperandStack();
			th.setNextPC(lastInsnInLoop.getNext());
		}
	}


	/****PC REMOVAL******/
	/*private class PCPC{
		PathCondition prev;
		PathCondition now;
		PCPC(PathCondition p,PathCondition n){
			prev=p;
			now=n;
		}
		@Override
		public String toString() {
			return "prev: " + prev+ " now:" + now;
		}
	}
	private HashMap<String,PCPC> insnPCMap = new HashMap<String, PCPC>(); 
	private PathCondition prevPc=null;


	private String getInsnStr(Instruction isn){
		return isn.getPosition()+": " + isn.getMnemonic();
	}

	private String getPcStr(PathCondition pc){
		if(pc==null){
			return "null";
		}
		return pc.toString().replace('\n', ' ');
	}*/





	@Override
	public void instructionExecuted(JVM vm) {
		Instruction insn=vm.getLastInstruction();
		ThreadInfo th=vm.getLastThreadInfo();
		List<StackFrame> stack=vm.getLastThreadInfo().getStack();

		/*condition: at least one public method (except Verify.getInt()) on top of synthetic main and actual main.*/
		boolean condition=(stack.size()>=3
				&& stack.get(1).getMethodInfo().getName().contains("main") 
				&& !stack.get(2).getMethodInfo().getFullName().contains("getInt")); 

		if(condition&& !th.isFirstStepInsn()){
			insn.visit(CoverageAnalyzer.getCurrentPath());
			//System.out.println("LoopHandlingListener.java:127 insn visited: "+ insn + " " + insn.getVisitCount());
		}


		/****PC REMOVAL******/
		/*if(condition){
			ChoiceGenerator<?> cg=vm.getChoiceGenerator();
			if (cg != null && !(cg instanceof PCChoiceGenerator)) {
				cg = cg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
			}

			if(cg instanceof PCChoiceGenerator){
				PathCondition nowPc=((PCChoiceGenerator) cg).getCurrentPC();
				nowPc=(nowPc==null)?null:nowPc.make_copy();

				PCPC alreadyThere= null;
				if(insnPCMap.get(getInsnStr(insn))!=null){
					alreadyThere=insnPCMap.get(getInsnStr(insn));
					if((nowPc!=null &&!nowPc.equals(alreadyThere.now))){
						System.out.println("LoopHandlingLisstener.java::206 Modifying "+ getPcStr(alreadyThere.now) +" to " + getPcStr(nowPc));
						alreadyThere.now=nowPc;
						prevPc=(nowPc==null)?null:nowPc.make_copy();
					}
				}
				else{
					if((nowPc!=null && !nowPc.equals(prevPc))){
						PCPC pcpc=new PCPC(prevPc,nowPc);
						System.out.println("LoopHandlingLisstener.java::210 Adding ->" + getPcStr(nowPc) + "<- to " + getInsnStr(insn) + " prev: "+ getPcStr(prevPc) );
						insnPCMap.put(getInsnStr(insn), pcpc);
						prevPc = (nowPc==null)?null:nowPc.make_copy();
					}	
				}
			}
		}*/

	}
}
