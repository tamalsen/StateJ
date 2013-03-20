package com.srts.phaseI.path;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

import java.util.LinkedHashSet;


public class Path {
	private PathCondition pc;
	private LinkedHashSet<String> insnSeq;
	private VSM vsm;
	private Expression returnVal;
	private String methodName;
	private boolean valid=true;
	private boolean constructorPath=false;
	private boolean loopPath=false;
	UncaughtException ue=null;
	
	private class UncaughtException{
		public String exClass="java.lang.Exception";
		public int position=0;
	}
	public void setUncaughtException(String exClass,int position){
		ue=new UncaughtException();
		ue.exClass=exClass;
		ue.position=position;
	}
	
	public String getUncaughtExceptionClass(){
		return (ue==null)?null:ue.exClass;
	}
	public int getUncaughtExceptionPosition(){
		return (ue==null)?-1:ue.position;
	}
	public boolean isExceptionPath(){
		return ue!=null;
	}
	
	public Path(){
		
	}
	
	public Path(String methodName){
		this.methodName=methodName;
		
	}
	public String getMethodName(){
		return methodName;
	}
	public Path(PathCondition pc,LinkedHashSet<String> insnSeq,VSM vsm,Expression returnVal){
		this.pc = pc;
		this.insnSeq = insnSeq;
		this.returnVal = returnVal;
		this.vsm = vsm;
	}
	
	public boolean isValid(){
		return valid;
	}
	public void invalidate(){
		valid=false;
	}
	public void setAsConstructorPath(){
		constructorPath=true;
	}
	public boolean isConstructorPath(){
		return constructorPath;
	}
	public void setAsLoopPath(){
		loopPath=true;
	}
	public boolean isLoopPath(){
		return loopPath;
	}
	public void setPc(PathCondition pc) {
		this.pc = pc;
	}
	public void setInsnSeq(LinkedHashSet<String> insnSeq) {
		this.insnSeq = insnSeq;
	}
	public void setVSM(VSM vsm) {
		this.vsm = vsm;
	}
	public void setReturnVal(Expression returnVal) {
		this.returnVal = returnVal;
	}
	
	public PathCondition getPc() {
		return pc;
	}
	public LinkedHashSet<String> getInsnSeq() {
		return insnSeq;
	}
	public VSM  getVSM() {
		return vsm;
	}
	public Expression getReturnVal() {
		return returnVal;
	}

	public boolean equals(Object o){
		Path p=(o instanceof Path)?(Path)o:null;
		return p!=null 
				&& (pc==p.getPc() || (pc!=null && pc.equals(p.getPc())))
				&& (insnSeq==p.getInsnSeq() ||(insnSeq !=null && insnSeq.equals(p.getInsnSeq())))
				&& (vsm==p.getVSM() ||(vsm !=null && vsm.equals(p.getVSM())))
				&& (returnVal==p.getReturnVal() ||(returnVal !=null && returnVal.equals(p.getReturnVal())))
				&& (methodName!=null && methodName.equals(p.getMethodName()))
				&& valid==p.isValid()
				&& constructorPath==p.isConstructorPath();
	}
}

