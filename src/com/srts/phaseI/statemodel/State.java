package com.srts.phaseI.statemodel;

import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.MathRealExpression;
import gov.nasa.jpf.symbc.numeric.MixedConstraint;
import gov.nasa.jpf.symbc.numeric.ObjectExpression;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import choco.kernel.model.constraints.ConstraintType;

import com.srts.phaseI.solvers.SymbolicConstraintsGeneral;
import com.srts.phaseI.util.Utility;

public class State {
	private Constraint stateCond;
	private String id;
	private boolean startState=false;
	private boolean exceptionState=false;
	public State(Constraint c, String id){
		stateCond= ((c==null)?null:c.makeCopy());
		this.id=id;
	}
	
	private State(){
	}
	
	
	public void setAsStartState(){
		startState=true;
	}
	public boolean isStartState(){
		return startState;
	}
	
	public void setAsExceptionState(){
		exceptionState=true;
	}
	public boolean isExceptionState(){
		return exceptionState;
	}
	
	public String toString(){
		return this.id;
	}
	public State(Set<Constraint> c, String id){
		Iterator<Constraint> it=c.iterator();
		while(it.hasNext()){
			if(stateCond==null)
				stateCond=it.next().makeCopy();
			else
				stateCond.last().and=it.next().makeCopy();
		}
		this.id=id;
	}
	

	public Constraint getStateCondition(){
		return stateCond;
	}
	
	public String getId(){
		return id;
	}
	
	public boolean equals(Object q){
		
		if(q!=null && q instanceof State){
			State s=(State)q;
			if(this.getStateCondition()!=null && this.getStateCondition().equals(s.getStateCondition())){
				//System.out.println("State.java:61 "+this.getStateCondition() +" and "+ s.getStateCondition() + " :true ");
				return true;
			}
			else{
				//System.out.println("State.java:61 "+this.getStateCondition() +" and "+ s.getStateCondition() + " :false ");
			}
		}
		
		return false;
	}
	/**
	 * 
	 * @param exprMap a map between variable names to their value expression
	 * @return a pdl if the exprMap satisfy the stateCond 
	 */
	public Constraint getConsIfSatisfy(final Constraint pc,final Map<Expression,Expression> exprMap){
		if(this.isStartState()){
			return null; 
		}
		if (stateCond == null){ // if default state return a synthetic Constraint
			Constraint c= new LinearIntegerConstraint(new IntegerConstant(1),Comparator.EQ,new IntegerConstant(1));
			c.setConstraintType();
			return c;
		}
		//System.out.println(exprMap);
		Constraint tempC=null,cons=null;
		tempC=stateCond.makeCopy();
		//System.out.println("before:"+c);
		while(tempC!=null){
			Constraint tempC2=null;
			//System.out.println("State.java: "+ tempC.getLeft() + " class:"+tempC.getLeft().getClass());
			Expression l=replaceVarValInExpression(exprMap,tempC.getLeft());
			//System.out.println("State.java: "+ tempC.getRight() + " class:"+tempC.getRight().getClass() + exprMap);
			Expression r=replaceVarValInExpression(exprMap,tempC.getRight());
			if(l instanceof IntegerExpression && r instanceof IntegerExpression)
				tempC2=new LinearIntegerConstraint((IntegerExpression)l,tempC.getComparator(), (IntegerExpression)r);
			else if(l instanceof IntegerExpression && r instanceof RealExpression)
				tempC2=new MixedConstraint((RealExpression)r,tempC.getComparator().not(), (IntegerExpression)l);
			else if(r instanceof IntegerExpression && l instanceof RealExpression)
				tempC2=new MixedConstraint((RealExpression)l,tempC.getComparator().not(), (IntegerExpression)r);
			else if(l instanceof RealExpression && r instanceof RealExpression)
				tempC2=new RealConstraint((RealExpression)l,tempC.getComparator().not(), (RealExpression)l);
			if(cons==null){
				cons=tempC2;
			}else{
				cons.last().and=tempC2;
			}
			tempC=tempC.and;
		}
		if(cons==null){
			System.out.println("State.java:110 !!! Null is not expected here.");
			throw new RuntimeException("Null is not expected here.");
		}
		Constraint save=cons.makeCopy();
		if(pc!=null){
			cons.last().and=pc.makeCopy();
			//System.out.println("State.java::143 cons: "+ cons.stringPC().replace('\n', ' ') + " pre:" + pc.stringPC().replace('\n', ' ')) ;
		}

		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		if(scg.isSatisfiable(cons)){
			System.out.println("State.java:143 "+cons.stringPC().replace('\n', ' ') + " is satisfiable!");
			//System.out.println("after:"+c);
			return save;
		}
		else {// not satisfiable
			//System.out.println("after:"+c);
			System.out.println("State.java:149 "+cons.stringPC().replace('\n', ' ') + " is NOT satisfiable!");
			return null;
		}
	}
	
	public boolean isSatisfy(final Constraint cons){
		if(cons==null)
			return true;
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		Constraint c=stateCond.makeCopy();
		c.last().and=cons.makeCopy();
		if(scg.isSatisfiable(c)){
			//System.out.println(c.stringPC().replace('\n', ' ') + " is satisfiable!");
			//System.out.println("Constraint: "+cons.stringPC().replace('\n', ' ') +" is matched with " + stateCond.stringPC().replace('\n', ' '));
			return true;
		}
		//System.out.println("Constraint: "+cons.stringPC().replace('\n', ' ') +"  does not match!");
		return false;
	}
	
	/*
	 * Replaces occurrences of a symbolic variable with the 
	 * corresponding expression specified in exprMap. Returns the new expression.
	 */
	private Expression replaceVarValInExpression(final Map<Expression,Expression> varValMap,final Expression e){
		
		if(e instanceof BinaryLinearIntegerExpression) {
			Expression left=null,right=null;
			BinaryLinearIntegerExpression tempBle=((BinaryLinearIntegerExpression)e);
			left=replaceVarValInExpression(varValMap, tempBle.getLeft());
			right=replaceVarValInExpression(varValMap, tempBle.getRight());
			if(left instanceof IntegerConstant && right instanceof IntegerConstant){
				BinaryLinearIntegerExpression bEx=  new BinaryLinearIntegerExpression((IntegerExpression)left,tempBle.getOp(),(IntegerExpression)right,-1);
				return new IntegerConstant(bEx.solution());
			}
			//System.out.println("This should be returned: "+left+"---"+right) ;
			if(left instanceof IntegerExpression && right instanceof IntegerExpression){
				if(left instanceof BinaryNonLinearIntegerExpression || right instanceof BinaryNonLinearIntegerExpression){
					return new BinaryNonLinearIntegerExpression((IntegerExpression)left,tempBle.getOp(),(IntegerExpression)right,calculateExpressionType(left,right));
				}
				else{
					return new BinaryLinearIntegerExpression((IntegerExpression)left,tempBle.getOp(),(IntegerExpression)right,calculateExpressionType(left,right));
				}
			}
		}
		else if(e instanceof BinaryRealExpression){
			Expression left=null,right=null;
			BinaryRealExpression tempBre=((BinaryRealExpression)e);
			left=replaceVarValInExpression(varValMap, tempBre.getLeft());
			right=replaceVarValInExpression(varValMap, tempBre.getRight());
			//System.out.println("This should be returned: "+left+"---"+right) ;
			if(left instanceof RealConstant && right instanceof RealConstant){
				BinaryRealExpression brEx= new BinaryRealExpression((RealExpression)left,tempBre.getOp(),(RealExpression)right,-1);
				return new RealConstant(brEx.solution());
			}
			if(left instanceof RealExpression && right instanceof RealExpression){
					return new BinaryRealExpression((RealExpression)left,tempBre.getOp(),(RealExpression)right,calculateExpressionType(left,right));
			}
			
		}
		else if( e instanceof BinaryNonLinearIntegerExpression){
			Expression left=null,right=null;
			BinaryNonLinearIntegerExpression tempBnle=((BinaryNonLinearIntegerExpression)e);
			left=replaceVarValInExpression(varValMap,tempBnle.left);
			right=replaceVarValInExpression(varValMap, tempBnle.right);
			//System.out.println("This should be returned: "+left+"---"+right) ;
			return new BinaryNonLinearIntegerExpression((IntegerExpression)left,tempBnle.op,(IntegerExpression)right,calculateExpressionType(left,right));
		}
				
		else if(e instanceof MathRealExpression){
			Expression left=null,right=null;
			MathRealExpression tempMre=((MathRealExpression)e);
			left=replaceVarValInExpression(varValMap, tempMre.getArg1());
			right=replaceVarValInExpression(varValMap, tempMre.getArg2());
			return new MathRealExpression(tempMre.op,(RealExpression)left,(RealExpression)right,calculateExpressionType(left,right));
		}
		if(e instanceof IntegerConstant 
				|| e instanceof RealConstant){
			//System.out.println("Constant:"+ e);
			return e;
		}
		if(e instanceof SymbolicInteger 
				|| e instanceof SymbolicReal){
			//System.out.println("Variable:"+e);
			Set<Expression> ks=varValMap.keySet();
			Iterator<Expression> it=ks.iterator();
			while(it.hasNext()){
			final Expression e1=it.next();
			final Expression e2=varValMap.get(e1);
			//System.out.println(e+"--"+e1.stringPC()+"--"+e2.stringPC());
			
				if(e1.toString().matches(".*\\(.*,.*\\)")){
					if(e.toString().matches(".*\\(.*,.*\\)")){
						String var1="", var2="";
						var1=e.toString().replaceAll("(.*)\\(.*\\)", "$1");
						var2=e1.toString().replaceAll("(.*)\\(.*\\)", "$1");
						//System.out.println("State.java var1/var2: "+ var1+ "/" + var2);
						if(var1.equals(var2)){
							if(e.toString().equals(e1.stringPC())){
								return new IntegerConstant(1);
							}
							else{
								return new IntegerConstant(0);
							}
						}
					}
				}
				/* 
				 * if e is equal to e1, return e2.
				 * e2==null indicates e1 need not to be replaced. 
				 */
				if(e2!=null && !e1.stringPC().equals(e2.stringPC()) && e.toString().equals(e1.stringPC()) ){
					//System.out.println(e + " should be replaced with "+ e2.stringPC());
					return e2;
				}
			}
			return e;
		}
		return e;
	}
	
	private int calculateExpressionType(Expression left,Expression right){
		int l=left.getType();
		int r= right.getType();
		
		if(l==r)
			return left.getType(); 
		
		if(l<=0 && r <=0)
			return 0;
		
		if((l*r)==-2 )
			return 2;
		return 1;
	}
}
