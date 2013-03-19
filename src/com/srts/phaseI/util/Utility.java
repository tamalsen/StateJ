package com.srts.phaseI.util;

import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.MathRealExpression;
import gov.nasa.jpf.symbc.numeric.MixedConstraint;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealConstraint;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.srts.phaseI.solvers.SymbolicConstraintsGeneral;

public class Utility {
	public static void printSet(String head ,Set s){
		System.out.println("===="+head+"====");
		Iterator it=s.iterator();
		while(it.hasNext()){
			System.out.println("--"+it.next());
		}
		System.out.println("=================");
	}
	public static boolean isEquivalent(Constraint a, Constraint b){
		if(a.getLeft().toString().equals(b.getLeft().toString())
				&& a.getRight().toString().equals(b.getRight().toString())
				&& (a.getComparator()==b.getComparator() 
				|| a.getComparator()==b.getComparator().not())) {
			return true;
		}
		return false;
	}

	public static Expression getProperExpressionObject(int type, Number value){
		Expression si=null;
		switch(type){
		case Types.T_ARRAY:
			break;
		case Types.T_REFERENCE:
			break;
		case Types.T_LONG:
		case Types.T_SHORT:
		case Types.T_INT:
		case Types.T_CHAR:
		case Types.T_BYTE:
		case Types.T_BOOLEAN:
			si =new IntegerConstant(value.intValue());
			break;
		case Types.T_DOUBLE:
		case Types.T_FLOAT:
			si =new RealConstant(value.doubleValue());
			break;
		default:
			System.out.println("error, unknown argument type");
		}
		return si;
	}
	private static Vector<Set<Constraint>> states=new Vector<Set<Constraint>>();
	public static Vector<Set<Constraint>> getPermutedConstraints(Set<Constraint> mdls){
		states=new Vector<Set<Constraint>>();
		Vector<Constraint> v=new Vector<Constraint>();
		v.addAll(mdls);
		permute(v,new HashSet<Constraint>(),0);
		return states;
	}
	private static void permute(Vector<Constraint> mdls, Set<Constraint> c,int pos){
		if(mdls.size()==pos){
			Set<Constraint> c1=new HashSet<Constraint>();
			c1.addAll(c);
			states.add(c1);
			return;
		}
		Constraint cons1=mdls.get(pos).makeCopy();
		c.add(cons1);
		permute(mdls,c,pos+1);
		c.remove(cons1);
		Constraint cons2=mdls.get(pos).not();
		c.add(cons2);
		permute(mdls,c,pos+1);
		c.remove(cons2);
	}
	
	public static boolean isSatisfy(final Constraint cons){
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		return scg.isSatisfiable(cons);
	}
	public static  boolean isSatisfy(final Set<Constraint> sc){
		Constraint c=null;
		Iterator<Constraint> it=sc.iterator();
		while(it.hasNext()){
			if(c==null)
				c=it.next().makeCopy();
			else
				c.last().and=it.next().makeCopy();
		}
		boolean b=isSatisfy(c);
		System.out.println( c + " is satisfiable: "+ b);
		return b;
	}
	
	
	public Constraint getPDLIfSatisfy(final Constraint constr,final Map<Expression,Expression> exprMap){
		if (constr == null){
			return null;
		}
		//System.out.println(exprMap);
		Constraint tempC=null,cons=null;
		tempC=constr.makeCopy();
		//System.out.println("before:"+c);
		while(tempC!=null){
			Constraint tempC2=null;
			Expression l=doRecursion(exprMap,tempC.getLeft());
			Expression r=doRecursion(exprMap,tempC.getRight());
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
		}
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		if(scg.isSatisfiable(cons)){
			System.out.println(cons.stringPC().replace('\n', ' ') + " is satisfiable!");
			//System.out.println("after:"+c);
			return cons;
		}
		else {
			//System.out.println("after:"+c);
			return null;
		}
	}
	
	/*
	 * Replaces occurrences of a symbolic variable with the 
	 * corresponding expression specified in exprMap. Returns the new expression.
	 */
	private Expression doRecursion(final Map<Expression,Expression> exprMap,final Expression e){
		
		if(e instanceof BinaryLinearIntegerExpression) {
			Expression left=null,right=null;
			BinaryLinearIntegerExpression tempBle=((BinaryLinearIntegerExpression)e);
			left=doRecursion(exprMap, tempBle.getLeft());
			right=doRecursion(exprMap, tempBle.getRight());
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
			left=doRecursion(exprMap, tempBre.getLeft());
			right=doRecursion(exprMap, tempBre.getRight());
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
			left=doRecursion(exprMap,tempBnle.left);
			right=doRecursion(exprMap, tempBnle.right);
			//System.out.println("This should be returned: "+left+"---"+right) ;
			return new BinaryNonLinearIntegerExpression((IntegerExpression)left,tempBnle.op,(IntegerExpression)right,calculateExpressionType(left,right));
		}
				
		else if(e instanceof MathRealExpression){
			Expression left=null,right=null;
			MathRealExpression tempMre=((MathRealExpression)e);
			left=doRecursion(exprMap, tempMre.getArg1());
			right=doRecursion(exprMap, tempMre.getArg2());
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
			Set<Expression> ks=exprMap.keySet();
			Iterator<Expression> it=ks.iterator();
			while(it.hasNext()){
			final Expression e1=it.next();
			final Expression e2=exprMap.get(e1);
			//System.out.println(e+"--"+e1.stringPC()+"--"+e2.stringPC());
			
				/* 
				 * if e is equal to e1 return e2 so that occurrences of e1 are replaced by e2.
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












