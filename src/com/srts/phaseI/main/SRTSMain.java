package com.srts.phaseI.main;

import gov.nasa.jpf.jvm.ClassInfo;
import java.io.File;
import com.srts.phaseI.classfile.ClassFile;
import com.srts.phaseI.classfile.ClassFileInstrumentar;
import com.srts.phaseI.classfile.ClassFileException;
import com.srts.phaseI.classfile.ClassFilePrinter;
import com.srts.phaseI.statemodel.StateModel;
import com.srts.phaseI.statemodel.StateModelRegister;


public class SRTSMain {

	public static void printClassFile(String path){
		ClassFilePrinter printer = new ClassFilePrinter();

		ClassInfo ci=null;   
		try { 
			ClassFile cf = new ClassFile(path);
			cf.parse(printer);
			//ci=new ClassInfo(cf, 12458574);

		} catch (ClassFileException cfx){ 
			cfx.printStackTrace(); 
		} 
	}
	
	public static  void main(String[] args){
		/**
		 * @TODO: construct class dependency graph (assumption is: it would be a DAG)
		 * @TODO: Sort topologically.
		 * @TODO: for each class, identify its public methods and instrument it with a main method.
		 */
		System.out.println("===================Welcome to the machines!!!==================");
		
		String[] classPath=new String[]{"F:\\Project\\EclipseWorkspace4\\TestProject\\bin\\"};
		String tempPath="F:\\temp\\";
		ClassFileInstrumentar.initClassFiles(classPath,tempPath);
		for(String clsName: ClassFileInstrumentar.getClassOrder()){
			
			printClassFile(tempPath + clsName.replaceAll("\\.", "\\\\")+ ".class");
			System.out.println("State Model for: " + clsName);
			for(String s: ClassFileInstrumentar.getSymMethods(clsName)){
				System.out.println("----"+ s);
			}
			StateModel sm=StateModelBuilder.extractStateModel(clsName,tempPath, "main([Ljava/lang/String;)V",ClassFileInstrumentar.getSymMethods(clsName),0);
			StateModelRegister.addStateModel(clsName, sm);
			sm.printAsGraphViz(new File("F:\\Project\\Graphviz\\"+clsName+".dot"));
			sm.printToFile(new File("F:\\Project\\Graphviz\\"+clsName+".sm"), clsName);
			drawPDF("F:\\Project\\Graphviz\\"+clsName+".dot");
			System.out.println("Minimized No of Transitions: "+ sm.hm.keySet().size());
		}
		System.exit(0);
	}
	
	private static void drawPDF(String file){
		String[] cmd={"dot.exe","-Tpdf","-O",file};
        
		Runtime rt=Runtime.getRuntime();
        try {
        	Process p=rt.exec(cmd);
        	/*Thread.sleep(100);
        	Process q1=rt.exec("pdfclose --file F:\\Project\\Graphviz\\sm.dot.pdf" );
        	Thread.sleep(100);
        	Process q2=rt.exec("pdfopen --file F:\\Project\\Graphviz\\sm.dot.pdf" );*/
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
}
