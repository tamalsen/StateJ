//
//Copyright (C) 2006 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package com.srts.phaseI.solvers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import yices.YicesLite;

public class ProblemYices extends ProblemGeneral {
  YicesLite yices;
  int ctx;
  static YicesLite oldYices;
  static int oldCtx;

  HashMap<String, String> modelMap = new HashMap<String, String>();

  public ProblemYices() {
    //reset the old context
    if(oldYices!=null)
      oldYices.yicesl_del_context(oldCtx);

    String workingDir = System.getProperty("user.dir");
    String modelDir = workingDir + "/yicesOutput";
    File file = new File(modelDir);
    if (!file.exists()) {
      try {
        file.mkdir();
      } catch (Exception e) {
        throw new RuntimeException("# Error: Cannot create directory" + modelDir);
      }
    }

    yices = new YicesLite();

    ctx = yices.yicesl_mk_context();
    yices.yicesl_set_verbosity((short)0);
    //		workingDir = System.getProperty("user.dir");
  }

  public String getYicesDouble(double d) {
    int power = 1;
    String dstr = Double.toString(d);
    if (dstr.contains("E")) {
      int indexOfE = dstr.indexOf('E');
      power = Integer.decode(dstr.substring(indexOfE));
      dstr = dstr.substring(0, indexOfE);
    }
    if (dstr.contains(".")) {
      int indexOfDot = dstr.indexOf('.');
      int length = dstr.length();
      int nZeroes = length - indexOfDot - 1;
      dstr = dstr.substring(0, indexOfDot) + dstr.substring(indexOfDot + 1, length);
      int div = 1;
      while (nZeroes > 0) {
        div *= 10;
        nZeroes--;
      }
      if (power > 1)
        return "(/ (* " + dstr + " " + power + ") " + div + ")";
      else
        return "(/ " + dstr + " " + div + ")";
    } else
      return "(*" + dstr + " " + power + ")";
  }

  public double getYicesDouble(String value)
  {
    boolean isNeg = false;
    if (value.contains("/")) {
      String[] tokens = value.split("/");

      if (tokens[0].startsWith("-")) {
        isNeg = true;
        tokens[0] = tokens[0].substring(1);
      }

      double left = Double.valueOf(tokens[0]);
      double right = Double.valueOf(tokens[1]);

      if (right == 0)
        throw new RuntimeException("# Error: Divide by zero!");

      return isNeg ? -1.0 * left / right : left / right;
    } else
      return Double.valueOf(value);
  }

  public String makeIntVar(String name, int min, int max) {
    yices.yicesl_read(ctx,"(define "+name+"::int)");
    yices.yicesl_read(ctx,"(assert (> "+ name + " " + min +"))");
    yices.yicesl_read(ctx,"(assert (< "+ name + " " + max +"))");
    return name;
  }

  public String makeRealVar(String name, double min, double max) {
    yices.yicesl_read(ctx,"(define "+name+"::real)");
    yices.yicesl_read(ctx,"(assert (> "+ name + " " + getYicesDouble(min) +"))");
    yices.yicesl_read(ctx,"(assert (< "+ name + " " + getYicesDouble(max) +"))");
    return name;
  }

  public Object eq(int value, Object exp){return "(= " + value + " " + (String)exp + ")";}
  public Object eq(Object exp, int value){return "(= " + (String)exp + " " + value + ")";}
  public Object eq(Object exp1, Object exp2){return "(= " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object eq(double value, Object exp){return "(= " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object eq(Object exp, double value){return "(= " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object neq(int value, Object exp){return "(/= " + value + " " + (String)exp + ")";}
  public Object neq(Object exp, int value){return "(/= " + (String)exp + " " + value + ")";}
  public Object neq(Object exp1, Object exp2){return "(/= " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object neq(double value, Object exp){return "(/= " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object neq(Object exp, double value){return "(/= " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object leq(int value, Object exp){return "(<= " + value + " " + (String)exp + ")";}
  public Object leq(Object exp, int value){return "(<= " + (String)exp + " " + value + ")";}
  public Object leq(Object exp1, Object exp2){return "(<= " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object leq(double value, Object exp){return "(<= " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object leq(Object exp, double value){return "(<= " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object geq(int value, Object exp){return "(>= " + value + " " + (String)exp + ")";}
  public Object geq(Object exp, int value){return "(>= " + (String)exp + " " + value + ")";}
  public Object geq(Object exp1, Object exp2){return "(>= " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object geq(double value, Object exp){return "(>= " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object geq(Object exp, double value){return "(>= " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object lt(int value, Object exp){return "(< " + value + " " + (String)exp + ")";}
  public Object lt(Object exp, int value){return "(< " + (String)exp + " " + value + ")";}
  public Object lt(Object exp1, Object exp2){return "(< " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object lt(double value, Object exp){return "(< " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object lt(Object exp, double value){return "(< " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object gt(int value, Object exp){return "(> " + value + " " + (String)exp + ")";}
  public Object gt(Object exp, int value){return "(> " + (String)exp + " " + value + ")";}
  public Object gt(Object exp1, Object exp2){return "(> " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object gt(double value, Object exp){return "(> " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object gt(Object exp, double value){return "(> " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object plus(int value, Object exp) {return "(+ " + value + " " + (String)exp + ")";}
  public Object plus(Object exp, int value) {return "(+ " + (String)exp + " " + value + ")";}
  public Object plus(Object exp1, Object exp2) {return "(+ " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object plus(double value, Object exp) {return "(+ " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object plus(Object exp, double value) {return "(+ " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object minus(int value, Object exp) {return "(- " + value + " " + (String)exp + ")";}
  public Object minus(Object exp, int value) {return "(- " + (String)exp + " " + value + ")";}
  public Object minus(Object exp1, Object exp2) {return "(- " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object minus(double value, Object exp) {return "(- " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object minus(Object exp, double value) {return "(- " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object mult(int value, Object exp) {return "(* " + value + " " + (String)exp + ")";}
  public Object mult(Object exp, int value) {return "(* " + (String)exp + " " + value + ")";}
  public Object mult(Object exp1, Object exp2) {return "(* " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object mult(double value, Object exp) {return "(* " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object mult(Object exp, double value) {return "(* " + (String)exp + " " + getYicesDouble(value) + ")";}

  public Object div(int value, Object exp) {return "(/ " + value + " " + (String)exp + ")";}
  public Object div(Object exp, int value) {return "(/ " + (String)exp + " " + value + ")";}
  public Object div(Object exp1, Object exp2) {return "(/ " + (String)exp1 + " " + (String)exp2 + ")";}
  public Object div(double value, Object exp) {return "(/ " + getYicesDouble(value) + " " + (String)exp + ")";}
  public Object div(Object exp, double value) {return "(/ " + (String)exp + " " + getYicesDouble(value) + ")";}

  //	Object sin(Object exp) {
  //		return pb.sin((RealExp) exp);
  //	}
  //	Object cos(Object exp) {
  //		return pb.cos((RealExp) exp);
  //	}
  //
  //	Object power(Object exp, double value) {
  //		return pb.power((RealExp) exp, (int)value);
  //	}

  public Object mixed(Object exp1, Object exp2) {return "(= " + (String)exp1 + " " + (String)exp2 + ")";}

  private void readModel(String modelFileName) {
    BufferedReader bufferedReader = null;
    modelMap.clear();

    try {
      bufferedReader =  new BufferedReader(new FileReader(new File(modelFileName)));
      String new_line;
      String delims = "[ ]+";
      while((new_line = bufferedReader.readLine()) != null) {
        String[] tokens = new_line.split(delims);
        if(tokens.length == 3){
          modelMap.put(tokens[1], tokens[2].substring(0, tokens[2].length() - 1));
        }
      }
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (bufferedReader != null) {
          bufferedReader.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  public double getRealValueInf(Object dpVar) {
    //		return ((RealVar) dpVar).getValue().getInf();
    //		throw new RuntimeException("# Error: Yices can not compute realValueInf!");
    //System.out.println("# Warning: Yices can not compute realValueInf! (used 0.0)");
    //return 0.0;
    return getRealValue(dpVar);
  }
  public double getRealValueSup(Object dpVar) {
    //		return ((RealVar) dpVar).getValue().getSup();
    //		throw new RuntimeException("# Error: Yices can not compute realValueSup!");
    System.out.println("# Warning: Yices can not compute realValueSup! (used 0.0)");
    return 0.0;
  }

  public double getRealValue(Object dpVar) {
    String vname = (String) dpVar;

    if (modelMap.containsKey(vname)) {
      return getYicesDouble(modelMap.get(vname));
    }
    return 0.0;
  }

  public int getIntValue(Object dpVar) {
    String vname = (String) dpVar;

    if (modelMap.containsKey(vname)) {
      return Integer.parseInt(modelMap.get(vname));
    }
    return 0;
  }

  public Boolean solve() {
    //        pb.getSolver().setTimeLimit(30000);
    //		yices.yicesl_read(ctx,"(dump-context)");

    int sat = yices.yicesl_inconsistent(ctx);
    if(sat == 0) {
      String workingDir = System.getProperty("user.dir");
      String modelFileName = workingDir + "/yicesOutput/model.txt";
      yices.yicesl_set_output_file(modelFileName);

      yices.yicesl_read(ctx,"(set-evidence! true)");
      yices.yicesl_read(ctx, "(check)");
      yices.yicesl_read(ctx,"(set-evidence! false)");
      readModel(modelFileName);
    }

    //		yices.yicesl_del_context(ctx);
    oldYices = yices;
    oldCtx = ctx;
    return sat == 0 ? true : false;
  }

  public void post(Object constraint) {
    yices.yicesl_read(ctx,"(assert " + (String)constraint + ")");
  }

  public Object and(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise AND");
  }

  public Object and(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise AND");
  }

  public Object and(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise AND");
  }

  @Override
  public
  Object or(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise OR");
  }

  @Override
  public
  Object or(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise OR");
  }

  @Override
  public
  Object or(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise OR");
  }

  @Override
  public
  Object shiftL(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftL");
  }

  @Override
  public
  Object shiftL(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftL");
  }

  @Override
  public
  Object shiftR(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftR");
  }

  @Override
  public
  Object shiftR(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftR");
  }

  @Override
  public
  Object xor(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise XOR");
  }

  @Override
  public
  Object xor(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise XOR");
  }

  @Override
  public
  Object xor(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise XOR");
  }

  @Override
  public Object shiftL(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise shifL");
  }

  @Override
  public Object shiftR(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise shifR");
  }

  @Override
  public Object shiftUR(int value, Object exp) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftUR");
  }

  @Override
  public Object shiftUR(Object exp, int value) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftUR");
  }

  @Override
  public Object shiftUR(Object exp1, Object exp2) {
    throw new RuntimeException("## Error Yices does not support bitwise shiftUR");
  }

  @Override
  public void postLogicalOR(Object[] constraints) {
    assert(constraints != null && constraints.length >=1);
    String orResult = "";
    for (int i = 0; i<constraints.length; i++) {
      orResult += (String)constraints[i] + " ";
    }
    orResult = "(or " + orResult+ ")";
    post(orResult);
  }

}
