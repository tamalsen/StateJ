package com.srts.phaseI.main;

import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

import com.srts.phaseI.solvers.SymbolicConstraintsGeneral;

public class TestSolver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//a>2 && a<5
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		LinearIntegerConstraint lig=new LinearIntegerConstraint(new SymbolicInteger("a"), Comparator.GT, new IntegerConstant(2));
		LinearIntegerConstraint lig2=new LinearIntegerConstraint(new SymbolicInteger("a"), Comparator.LT, new IntegerConstant(2));
		lig.and=lig2;
		System.out.println("lig is satisfiable: "+ scg.isSatisfiable(lig));
	}

}

