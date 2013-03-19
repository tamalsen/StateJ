package com.srts.phaseI.main.ui;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.icu.util.StringTokenizer;


public class ChangeImpactAnalysisUI {

	protected Shell parent;
	protected Shell shlSrts;
	private Text text;
	private Text text_1;
	private Text text_2;
	private Button btnRun;
	private Label lblSrtsForComponent;
	private Text text_3;
	private Button btnOpenCurrentProject;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ChangeImpactAnalysisUI window = new ChangeImpactAnalysisUI(null);
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ChangeImpactAnalysisUI(Shell parent){
		this.parent=parent;
	}
	
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlSrts.open();
		shlSrts.layout();
		while (!shlSrts.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlSrts = new Shell(parent);
		shlSrts.setBackground(SWTResourceManager.getColor(153, 204, 204));
		shlSrts.setSize(750, 446);
		shlSrts.setText("S-RTS - Change Impact Analyzer");
		
		lblSrtsForComponent = new Label(shlSrts, SWT.NONE);
		lblSrtsForComponent.setAlignment(SWT.CENTER);
		lblSrtsForComponent.setFont(SWTResourceManager.getFont("Consolas", 16, SWT.NORMAL));
		lblSrtsForComponent.setBounds(203, 40, 337, 32);
		lblSrtsForComponent.setText("Change Impact Analyzer");
		
		text = new Text(shlSrts, SWT.BORDER);
		text.setBounds(37, 125, 388, 25);
		
		Button btnOpenFile = new Button(shlSrts, SWT.NONE);
		btnOpenFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlSrts, SWT.OPEN);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text.setText(file.getAbsolutePath());
				}
				
			}
		});
		btnOpenFile.setBounds(443, 125, 124, 25);
		btnOpenFile.setText("Open TSC File");
		
		text_1 = new Text(shlSrts, SWT.BORDER);
		text_1.setBounds(37, 170, 388, 25);
		
		Button btnOpenDirectory = new Button(shlSrts, SWT.NONE);
		btnOpenDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlSrts, SWT.OPEN);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_1.setText(file.getAbsolutePath());
				}
			}
		});
		btnOpenDirectory.setBounds(443, 170, 231, 25);
		btnOpenDirectory.setText("Open Old Dependence Graph Matrix");
		
		text_3 = new Text(shlSrts, SWT.BORDER);
		text_3.setBounds(37, 215, 388, 25);
		
		btnOpenCurrentProject = new Button(shlSrts, SWT.NONE);
		btnOpenCurrentProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlSrts, SWT.OPEN);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_3.setText(file.getAbsolutePath());
				}
			}
		});
		btnOpenCurrentProject.setText("Open Current Dependence Graph Matrix");
		btnOpenCurrentProject.setBounds(443, 215, 231, 25);
		
		text_2 = new Text(shlSrts, SWT.BORDER);
		text_2.setBounds(37, 261, 388, 25);
		
		Button btnSaveFile = new Button(shlSrts, SWT.NONE);
		btnSaveFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlSrts, SWT.SAVE);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_2.setText(file.getAbsolutePath());
				}
			}
		});
		btnSaveFile.setBounds(443, 261, 124, 25);
		btnSaveFile.setText("Specify Output File");
		
		btnRun = new Button(shlSrts, SWT.NONE);
		btnRun.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String jsdgOldFile=text_1.getText();
				String jsdgNewFile=text_3.getText();
				String TSCFile=text.getText();
				String outFile=text_2.getText();
				
				ArrayList<StatementDetails> sdList=new ArrayList<StatementDetails>();
				int jsdgOldMatrix[][]=readJsdgMatrixFile(jsdgOldFile, sdList);
				System.out.println("Reading Old JSDG matrix file:");
				for(int i=0;i<jsdgOldMatrix[0].length;i++){
					System.out.println(Arrays.toString(jsdgOldMatrix[i]));
				}
				System.out.println(sdList);
				
				System.out.println("Reading New JSDG matrix file:");
				ArrayList<StatementDetails> sdList2=new ArrayList<StatementDetails>();
				int jsdgNewMatrix[][]=readJsdgMatrixFile(jsdgNewFile, sdList2);
				for(int i=0;i<jsdgOldMatrix[0].length;i++){
					System.out.println(Arrays.toString(jsdgNewMatrix[i]));
				}
				System.out.println(sdList2);
		
			}
		});
		btnRun.setFont(SWTResourceManager.getFont("Segoe UI", 14, SWT.NORMAL));
		btnRun.setBounds(198, 333, 337, 48);
		btnRun.setText("Compute Change Information");

	}
	
	private int[][] readJsdgMatrixFile(String fileName, ArrayList<StatementDetails> sdList){
		int jsdgMatrix[][]=null;
		try {
			BufferedReader fin= new BufferedReader(new FileReader(fileName));
			String thisLine=fin.readLine();
			StringTokenizer st=new StringTokenizer(thisLine," ");
			jsdgMatrix=new int[st.countTokens()][st.countTokens()];
			int j=0;
			while(thisLine != null){
				if(thisLine.contains("Statement Node Details:")){
					break;
				}
				st=new StringTokenizer(thisLine," ");
				for(int i=0;i<st.countTokens();i++){
					jsdgMatrix[j][i]=Integer.parseInt(st.nextToken());
				}
				j++;
				thisLine=fin.readLine();
			}
			
			thisLine=fin.readLine();
			thisLine=fin.readLine();
			while((thisLine=fin.readLine()) != null){
				st=new StringTokenizer(thisLine,"\t");
				if(st.countTokens()!=4)
					break;
				StatementDetails sd=new StatementDetails(st.nextToken(),st.nextToken(),Integer.parseInt(st.nextToken()),Integer.parseInt(st.nextToken()));
				sdList.add(sd);
			}
			
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		return jsdgMatrix;
	}
	
}


