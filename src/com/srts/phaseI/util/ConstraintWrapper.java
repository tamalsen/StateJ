package com.srts.phaseI.util;

import gov.nasa.jpf.symbc.numeric.Constraint;

public class ConstraintWrapper {
	public Constraint c;
	
	public ConstraintWrapper(Constraint c){
		this.c=c;
	}
	public boolean equals(Object cw){
		if(cw instanceof ConstraintWrapper){
			
			Constraint cns=((ConstraintWrapper)cw).c;
			/*if(this.c.getLeft().stringPC().equals(cns.getLeft().stringPC()) 
					&& this.c.getRight().stringPC().equals(cns.getRight().stringPC())){*/
			if(cns.equals(this.c) || cns.isEquivalent(c)){
				return true;
			}
			
			/*if (this.c.isEquivalent(cns)){ 
				return true;
			}
			  cns.and=this.c;
			System.out.println("ConstraintWrapper.java:21 equals: "+cns);
			SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
			if(!solver.isSatisfiable(cns)){
				return true;
			}*/
		}
		return false;
	}
	
	public int hashCode(){
		return 1;

	}
	
	public String toString(){
		return c.toString();
	}
}
