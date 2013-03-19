package com.srts.phaseI.main.ui;

public class StatementDetails {

	public StatementDetails(String className,String methodName,int lineNo, int index){
		this.className=className;
		this.methodName=methodName;
		this.lineNo=lineNo;
		this.index=index;
	}
	public String className;
	public String methodName;
	public int lineNo=-1;
	public int index=-1;
	
	@Override
	public String toString() {
		return className+ " "+ methodName+ ":" + lineNo+ "  index: "+ index; 
	}
}
