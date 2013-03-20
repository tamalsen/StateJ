package com.srts.phaseI.main.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.custom.CBanner;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ComponentDeveloperUI {

	protected Shell shlSrts;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ComponentDeveloperUI window = new ComponentDeveloperUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		shlSrts = new Shell();
		shlSrts.setBackground(SWTResourceManager.getColor(153, 204, 204));
		shlSrts.setSize(532, 335);
		shlSrts.setText("S-RTS - Component Developer");
		
		Button btnNewButton = new Button(shlSrts, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConstructDepGraUI window=new ConstructDepGraUI(shlSrts);
				window.open();
			}
		});
		btnNewButton.setBounds(40, 70, 183, 34);
		btnNewButton.setText("Construct Dependence Graph");
		
		Label lblNewLabel = new Label(shlSrts, SWT.NONE);
		lblNewLabel.setBackground(SWTResourceManager.getColor(153, 204, 204));
		lblNewLabel.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblNewLabel.setBounds(40, 22, 394, 82);
		lblNewLabel.setText("Click the button below to construct a dependence graph \r\nfrom an existing java project. ");
		Button btnComputeChangeInformation = new Button(shlSrts, SWT.NONE);
		btnComputeChangeInformation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ChangeImpactAnalysisUI window = new ChangeImpactAnalysisUI(shlSrts);
				window.open();
			}
		});
		btnComputeChangeInformation.setText("Compute Change Information");
		btnComputeChangeInformation.setBounds(40, 224, 183, 34);
		
		Label lblClickTheButton = new Label(shlSrts, SWT.NONE);
		lblClickTheButton.setBackground(SWTResourceManager.getColor(153, 204, 204));
		lblClickTheButton.setBounds(40, 148, 405, 63);
		lblClickTheButton.setText("Click the button below to compute change information \r\nby analyzing dependence graphs and Transition Statement\r\nCorrespondence.");
		lblClickTheButton.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));

	}
}
