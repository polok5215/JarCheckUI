package com.onappsolution.jarcheck;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;

import javax.swing.JMenuItem;
import java.awt.FlowLayout;

public class JarCheckUI {

	private JFrame frame;
	private JTextPane textArea;
	private JSlider slider;
	private JLabel lblMaxVer;
	private StringBuilder strVer;
	private StringBuilder strTable;
	private JButton btnCopyAllResults;
	
	private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	private JPopupMenu popupMenu;
	private JMenuItem mntmCopyCtrlc;

	private final class FileDropHandler extends TransferHandler{
	    @Override
	    public boolean canImport(TransferHandler.TransferSupport support) {
	        for (DataFlavor flavor : support.getDataFlavors()) {
	            if (flavor.isFlavorJavaFileListType()) {
	                return true;
	            }
	        }
	        btnCopyAllResults.setEnabled(false);
	        return false;
	    }

	    @Override
	    @SuppressWarnings("unchecked")
	    public boolean importData(TransferHandler.TransferSupport support) {
	        if (!this.canImport(support)){
	        	btnCopyAllResults.setEnabled(false);
	            return false;
	        }

	        List<File> files;
	        try {
	            files = (List<File>) support.getTransferable()
	                    .getTransferData(DataFlavor.javaFileListFlavor);
	        } catch (UnsupportedFlavorException | IOException ex) {
	            // should never happen (or JDK is buggy)
	        	btnCopyAllResults.setEnabled(false);
	            return false;
	        }
	        
	        strVer=new StringBuilder("");
	        strTable=new StringBuilder("");
	        textArea.setText("");//reset..
	        
	        for (File file: files) {
	        	try{
	        		strVer.append("<h2>Checking \""+file.getName()+"\"</h2>");
	        		strVer.append(JarCheck.checkJar(file.getPath(), JarCheck.convertHumanToMachine.get("1.1"), JarCheck.convertHumanToMachine.get("1."+slider.getValue())));
	        		strVer.append("<br/>");
	        	}catch(Exception e){
	        		//
	        	}
	        	
	        }
	        
	        strTable.append("<h2>MD5 Check</h2>");
	        strTable.append("<table border='1' cellspacing='0'>");
	        strTable.append("<tr><th>No.</th><th>Jar Name</th><th>MD5</th></tr>");
	        int i=1;
	        for (File file: files) {
	        	try{
	        		strTable.append("<tr><td>"+i+"</td><td>"+file.getName()+"</td><td>"+JarCheck.getMD5Checksum(file.getPath()).toUpperCase()+"</td></tr>");
	        	}catch(Exception e){
	        		//
	        	}
	        	i++;
	        }
	        strTable.append("</table>");
	        strTable.append("<br/><br/><br/>");
	        
	        textArea.setText(strVer.toString()+strTable.toString());
	        
	        btnCopyAllResults.setEnabled(true);
	        return true;
	    }
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JarCheckUI window = new JarCheckUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public JarCheckUI() {
		initialize();
	}

	private void copySelectedText(){
		int length = textArea.getSelectionEnd() - textArea.getSelectionStart();
		if(length>0){
			clipboard.setContents(new StringSelection(textArea.getSelectedText()), null);
		}
	}
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("JarCheckUI 1.0");
		
		textArea = new JTextPane();
		textArea.setContentType("text/html");
		textArea.setAutoscrolls(true);
		textArea.setEditable(false);
		textArea.setTransferHandler(new FileDropHandler());
		textArea.setText("<b>Drag in jar(s)</b> for checking versions and md5!");
		
		popupMenu = new JPopupMenu();
		mntmCopyCtrlc = new JMenuItem("Copy              Ctrl+C");
		mntmCopyCtrlc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copySelectedText();
			}
		});
		popupMenu.add(mntmCopyCtrlc);
		
		addPopup(textArea, popupMenu);
		
		textArea.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton()==3){//right button
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
				
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		textArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl C"), "copyTextSelected");
		textArea.getActionMap().put("copyTextSelected", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				copySelectedText();
			}
		});
		
		
		JScrollPane scrollPane = new JScrollPane ( textArea );
		scrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		frame.getContentPane().add(panel, BorderLayout.NORTH);
		
		btnCopyAllResults = new JButton("Generate HTML");
		panel.add(btnCopyAllResults);
		btnCopyAllResults.setEnabled(false);
		
		slider = new JSlider(0,1,8,6);
		slider.setPaintLabels(true);
		panel.add(slider);
		
		slider.setMinorTickSpacing(1);
		slider.setMajorTickSpacing(1);
		lblMaxVer = new JLabel("Max Ver:");
		panel.add(lblMaxVer);
		
		lblMaxVer.setText("Max Ver: 1."+slider.getValue());
		
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
		        if (!source.getValueIsAdjusting()) {
		        	lblMaxVer.setText("Max Ver: 1."+source.getValue());
		        }
			}
		});
		btnCopyAllResults.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(){
					@Override
					public void approveSelection(){
				        File f = getSelectedFile();
				        if(f.exists() && getDialogType() == SAVE_DIALOG){
				            int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_OPTION);
				            switch(result){
				                case JOptionPane.YES_OPTION:
				                    super.approveSelection();
				                    return;
				                case JOptionPane.NO_OPTION:
				                    return;
				                case JOptionPane.CLOSED_OPTION:
				                    return;
				            }
				        }
				        super.approveSelection();
				    }  
				};
				
				jfc.setFileFilter(new FileNameExtensionFilter("HTML Files", "html", "htm"));
				
				int returnVal = jfc.showSaveDialog(frame);
				
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = jfc.getSelectedFile();
	                
	                try {
	                	BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						
						writer.write(strVer.toString()+strTable.toString());
						
						writer.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
	                
	            }
				
				
			}
		});
	}

	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
}
