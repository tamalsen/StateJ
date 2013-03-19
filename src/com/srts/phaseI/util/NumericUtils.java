package com.srts.phaseI.util;

import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.ExpressionType;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.srts.phaseI.solvers.SymbolicConstraintsGeneral;

public class NumericUtils {
	public static Constraint getConsIfSatisfy(final Constraint cons1,final Map<Expression,Expression> exprMap){
		if (cons1 == null){ // if default state return a synthetic Constraint
			Constraint c= new LinearIntegerConstraint(new IntegerConstant(1),Comparator.EQ,new IntegerConstant(1));
			c.setConstraintType();
			return c;
		}
		Constraint tempC=null,cons=null;
		tempC=cons1.makeCopy();
		while(tempC!=null){
			Constraint tempC2=null;
			Expression l=replaceVarValInExpression(exprMap,tempC.getLeft());
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
			System.out.println("NumericUtils.java:63 !!! Null is not expected here.");
		}
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		if(scg.isSatisfiable(cons)){
			System.out.println("NumericUtils.java:70 "+cons.stringPC().replace('\n', ' ') + " is satisfiable!");
			return cons;
		}
		else {// not satisfiable
			return null;
		}
	}
	
	/*
	 * Replaces occurrences of a symbolic variable with the 
	 * corresponding expression specified in exprMap. Returns the new expression.
	 */
	private static Expression replaceVarValInExpression(final Map<Expression,Expression> varValMap,final Expression e){
		
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
	
	private static int calculateExpressionType(Expression left,Expression right){
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

	public static Set<String> doFindVariableNames(final Expression e){

		if(e instanceof BinaryLinearIntegerExpression) {
			Set<String> left=null,right=null;
			BinaryLinearIntegerExpression tempBle=((BinaryLinearIntegerExpression)e);
			left=doFindVariableNames(tempBle.getLeft());
			right=doFindVariableNames(tempBle.getRight());
			left.addAll(right);
			return left;
		}
		else if(e instanceof BinaryRealExpression){
			Set<String> left=null,right=null;
			BinaryRealExpression tempBre=((BinaryRealExpression)e);
			left=doFindVariableNames(tempBre.getLeft());
			right=doFindVariableNames(tempBre.getRight());
			left.addAll(right);
			return left;
		}
		else if( e instanceof BinaryNonLinearIntegerExpression){
			Set<String> left=null,right=null;
			BinaryNonLinearIntegerExpression tempBnle=((BinaryNonLinearIntegerExpression)e);
			left=doFindVariableNames(tempBnle.left);
			right=doFindVariableNames(tempBnle.right);
			left.addAll(right);
			return left;
		}

		else if(e instanceof MathRealExpression){
			Set<String> left=null,right=null;
			MathRealExpression tempMre=((MathRealExpression)e);
			left=doFindVariableNames(tempMre.getArg1());
			right=doFindVariableNames(tempMre.getArg2());
			left.addAll(right);
			return left;
		}


		else if(e instanceof IntegerConstant 
				|| e instanceof RealConstant){  
			//System.out.println("CoverageAnalyzer.java:189 Constant:"+ e + " type:"+e.getType());
			Set<String> s=new HashSet<String>();
			s.add(e.toString());
			return s;
		}
		else if(e instanceof SymbolicInteger){ 
			//System.out.println("CoverageAnalyzer.java:189  Integer:"+e + " type:"+e.getType());
			Expression tce=((SymbolicInteger)e).getTypeCastedExpression();
			if(tce!=e){
				return doFindVariableNames(tce);
			}
			else{
				Set<String> s=new HashSet<String>();
				s.add( e.toString());
				return s;
			}
		}
		if(e instanceof SymbolicReal){ 
			//System.out.println("CoverageAnalyzer.java:189  Integer:"+e + " type:"+e.getType());
			Expression tce=((SymbolicReal)e).getTypeCastedExpression();
			if(tce!=e){
				return doFindVariableNames(tce);
			}
			else{
				Set<String> s=new HashSet<String>();
				s.add( e.toString());
				return s;
			}
		}
		else if (e instanceof ObjectExpression){
			Map<String,Object> map= new HashMap<String, Object>();
			e.getVarsVals(map);
			Set<String> s=new HashSet<String>();
			Iterator<String> it=map.keySet().iterator();
			while(it.hasNext()){
				s.add(it.next());
			}
			return s;
		}
		else{
			System.out.println( "Error!!!Unknown expression type: " + e + " Type: " + e.getClass());
			return null;
		}
	}


	public static void setExpressionType(Expression ex){ //MDL, PDL or MXL
		if(ex==null)
			return;
		System.out.println("CoverageAnalyzer.java:245 Expression : "+ex+" type: "+ ex.getType());
		Set<String> varSet=doFindVariableNames(ex);
		Utility.printSet("vars", varSet);
		boolean mdl=false,pdl=false;
		Iterator<String> it=varSet.iterator();
		while(it.hasNext()){
			String var=it.next();
			if(var.contains("_SYM")){
				pdl=true;
			}
			else if(!var.contains("CONST_")){
				mdl=true;
			}
		}
		if(pdl && mdl){
			ex.setType(ExpressionType.MXL);
		}
		else if(pdl){
			ex.setType(ExpressionType.PDL);
		}
		else if(mdl){
			ex.setType(ExpressionType.MDL);
		}
		else{
			ex.setType(ExpressionType.CONCRETE);
		}
		System.out.println("CoverageAnalyzer.java:271 Expression : "+ex+" type: "+ ex.getType());
	}

	public static boolean isSatisfy(final Constraint cons){
		if(cons==null)
			return true;
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		if(scg.isSatisfiable(cons)){
			//System.out.println(c.stringPC().replace('\n', ' ') + " is satisfiable!");
			//System.out.println("Constraint: "+cons.stringPC().replace('\n', ' ') +" is matched with " + stateCond.stringPC().replace('\n', ' '));
			return true;
		}
		//System.out.println("Constraint: "+cons.stringPC().replace('\n', ' ') +"  does not match!");
		return false;
	}










}


