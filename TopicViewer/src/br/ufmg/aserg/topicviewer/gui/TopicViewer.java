package br.ufmg.aserg.topicviewer.gui;

import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import br.ufmg.aserg.topicviewer.gui.extraction.ExtractVocabularyView;
import br.ufmg.aserg.topicviewer.util.Properties;

public class TopicViewer extends javax.swing.JFrame {

	private static final long serialVersionUID = -7144305228539220119L;
	
    private static final String EXTRACT_VOCABULARY_PANEL = "extract";
	
    private javax.swing.JDesktopPane desktop;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    
    private javax.swing.JMenu activitiesMenu;
    private javax.swing.JMenu aboutMenu;
    private javax.swing.JMenu helpMenu;
    
    private javax.swing.JMenuItem configureWorkspaceMenuItem;
    private javax.swing.JMenuItem extractVocabularyMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    
    private Map<String, JInternalFrame> internalFrames;
	
	public TopicViewer() {
        initComponents();
        initListeners();	
        verifyProperties();
        
        this.internalFrames = new HashMap<String, JInternalFrame>();
    }

    private void initComponents() {

    	setTitle("TopicViewer");
        desktop = new javax.swing.JDesktopPane();
        
        mainMenuBar = new javax.swing.JMenuBar();
        activitiesMenu = new javax.swing.JMenu();
        activitiesMenu.setMnemonic('A');
        activitiesMenu.setText("Activities");
        
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jSeparator1.setBackground(new java.awt.Color(0, 0, 0));
        
        configureWorkspaceMenuItem = new javax.swing.JMenuItem();
        extractVocabularyMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();

        configureWorkspaceMenuItem.setText("Configure Workspace");
        extractVocabularyMenuItem.setText("Extract Vocabulary");
        exitMenuItem.setText("Exit");
        
        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        
        aboutMenu = new javax.swing.JMenu();
        aboutMenu.setMnemonic('b');
        aboutMenu.setText("About");
        
        helpMenu = new javax.swing.JMenu();
        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");
        
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setFont(new java.awt.Font("DejaVu Sans", 0, 12));
        setLocationByPlatform(true);
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));
        getContentPane().add(desktop);

        activitiesMenu.add(configureWorkspaceMenuItem);
        activitiesMenu.add(extractVocabularyMenuItem);
        activitiesMenu.add(jSeparator1);
        activitiesMenu.add(exitMenuItem);

        mainMenuBar.add(activitiesMenu);
        mainMenuBar.add(aboutMenu);
        mainMenuBar.add(helpMenu);

        setJMenuBar(mainMenuBar);
    }
    
    private void initListeners() {
    	configureWorkspaceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuConfigureWorkspaceActionPerformed(evt);
            }
        });
    	
    	extractVocabularyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExtractVocabularyActionPerformed(evt);
            }
        });
    	
    	exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSairActionPerformed(evt);
            }
        });
    }

    private void menuConfigureWorkspaceActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showDialog(this, "Open Workspace") != JFileChooser.CANCEL_OPTION) {
        	Properties.setProperty(Properties.WORKSPACE, chooser.getSelectedFile().getAbsolutePath());
        	enableButtons(true);
        	JOptionPane.showMessageDialog(this, "Workspace has just been configures.", "", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void menuExtractVocabularyActionPerformed(java.awt.event.ActionEvent evt) {
    	if (!invokeView(EXTRACT_VOCABULARY_PANEL)) {
    		ExtractVocabularyView extractVocabulary = new ExtractVocabularyView();
    		this.internalFrames.put(EXTRACT_VOCABULARY_PANEL, extractVocabulary);
    		this.desktop.add(extractVocabulary);
    	}
        System.gc();
    }

    private void menuSairActionPerformed(java.awt.event.ActionEvent evt) {
        System.gc();
        System.exit(0);
    }
    
    private void verifyProperties() {
    	Properties.load();
    	if (Properties.getProperty(Properties.WORKSPACE) == null)
    		enableButtons(false);
    }
    
    private void enableButtons(boolean enable) {
//    	extractVocabularyMenuItem.setEnabled(enable);
    	// TODO
    }
    
    private boolean invokeView(String viewId) {
    	if (this.internalFrames.containsKey(viewId)) {
    		this.internalFrames.get(viewId).setVisible(true);
    		return true;
    	}
    	else return false;
    }

    public static void main(String args[]) {
    	
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TopicViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TopicViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TopicViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TopicViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
            	TopicViewer frame = new TopicViewer();
            	
    			BufferedImage image = null;
    			try {
    				image = ImageIO.read(frame.getClass().getResource("../img/icon.png"));
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    			
    			frame.setIconImage(image);
                frame.setVisible(true);
            }
        });
    }
}