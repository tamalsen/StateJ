package com.srts.phaseI.main.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ErrorDialog extends Dialog {

	protected Object result;
	protected Shell shlCompleted;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ErrorDialog(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shlCompleted.open();
		shlCompleted.layout();
		Display display = getParent().getDisplay();
		while (!shlCompleted.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shlCompleted = new Shell(getParent(), getStyle());
		shlCompleted.setSize(299, 157);
		shlCompleted.setText("Error!");
		
		Button btnNewButton = new Button(shlCompleted, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shlCompleted.close();
			}
		});
		btnNewButton.setBounds(63, 65, 163, 35);
		btnNewButton.setText("OK");
		
		Label lblAnErrorOccured = new Label(shlCompleted, SWT.NONE);
		lblAnErrorOccured.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		lblAnErrorOccured.setAlignment(SWT.CENTER);
		lblAnErrorOccured.setBounds(25, 10, 243, 35);
		lblAnErrorOccured.setText("An error occured. Please try again.");

	}
}
