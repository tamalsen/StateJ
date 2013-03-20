package com.srts.phaseI.statemodel;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.ExpressionType;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.ObjectExpression;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SpecialExpressionFactory;

public class TransitionOut {
	private State outState;
	private Expression returnVal;
	public TransitionOut(State q,Expression ret){
		this.outState=q;
		this.returnVal=ret;
	}
	public State getState(){
		return outState;
	}
	public Expression getReturnVal(){
		return returnVal;
	}
	public boolean equals(Object t){
		return (t!=null) && (t instanceof TransitionOut) 
				&& ((t==this) 
						|| (((TransitionOut)t).getState().equals(this.getState())) 
						&& ((TransitionOut)t).getReturnVal()!=null && ((TransitionOut)t).getReturnVal().equals(this.getReturnVal())); 
	}

	/**
	 * @return the returnval only if returnval is a constant or a PDL , not in other cases
	 */
	public Expression getAllowedReturnVal(){
		if(returnVal==null){
			return SpecialExpressionFactory.getNullObjectExpression();
		}
		if(returnVal instanceof ObjectExpression){
			ObjectExpression oe= (ObjectExpression)returnVal;
			if(oe.getRefName().indexOf("#")>0){//pdl
				return returnVal;
			}
			else{
				return SpecialExpressionFactory.getUnknownObjectExpression();
			}
		}
		if (returnVal.getType()==-1 || returnVal.getType()==2)
			return returnVal;

		if(returnVal instanceof IntegerExpression){
			return SpecialExpressionFactory.getUnknownSymbolicInteger();
		}
		if(returnVal instanceof RealExpression){
			return SpecialExpressionFactory.getUnknownSymbolicReal();
		}
		return null;
	}
}
