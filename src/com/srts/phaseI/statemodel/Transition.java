package com.srts.phaseI.statemodel;

import gov.nasa.jpf.jvm.bytecode.Instruction;

import java.util.Set;


public class Transition implements Comparable<Transition>{
	private String id;
	private boolean hasLoop=false;
	
	
	public String getId() {
		return id;
	}
	private TransitionIn tIn;
	private TransitionOut tOut;
	private Set<String> stmtCoverage;
	public Transition(TransitionIn tIn,TransitionOut tOut,Set<String> stmtCoverage,String id){
		this.tIn=tIn;
		this.tOut=tOut;
		this.stmtCoverage=stmtCoverage;
		this.id=id;
	}
	public TransitionOut getTransitionOut(){
		return tOut;
	}
	
	public TransitionIn getTransitionIn(){
		return tIn;
	}
	public Set<String> getStmtCoverage(){
		return stmtCoverage;
	}
	
	public void setHasLoop(boolean l){
		hasLoop=l;
	}
	public boolean containsLoop(){
		return hasLoop;
	}
	public boolean equals(Object t){
		return (t!=null) &&(t instanceof Transition) 
				&& ((t==this) 
						|| (((Transition)t).getTransitionIn().equals(this.tIn) 
							&& ((Transition)t).getTransitionOut().equals(this.tOut))); 
	}
	public int hashCode(){
		return 1;
	}
	@Override
	public int compareTo(Transition arg0) {
		return (Integer.parseInt(this.getId())-Integer.parseInt(arg0.getId()));
	}
	public String toString(){
		return this.getId()+ " from " + tIn.getState().getId() + " to " +tOut.getState().getId() +" event:"+tIn.getStimuli()+  " guard: "+ tIn.getGuard() + " ret: "+ tOut.getReturnVal();
	}
}
