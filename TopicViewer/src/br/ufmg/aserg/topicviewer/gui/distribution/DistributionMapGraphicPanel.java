package br.ufmg.aserg.topicviewer.gui.distribution;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

public class DistributionMapGraphicPanel extends JPanel {

	private static final long serialVersionUID = 6060506974958934930L;
	
	private static final Integer packageStroke = 3;
	private static final Integer packageSpace = 8;
	private static final Integer classStroke = 2;
	private static final Integer classSpace = 3;
	private static final Integer classSize = 20;
	
	private static final Integer maxPackages = 10;
	private static final Integer maxClasses = 5;
	
	private DistributionMap distributionMap;
	private String[][] semanticTopics;
	
	private int xBound = 0;
	private int yBound = 0;
	
	private Rectangle2D.Double externalView;
	private List<DistributionRectangle> packageRectangles;
	private List<DistributionRectangle> pckgLabelRectangles;
	private List<DistributionRectangle> classRectangles;
	private List<DistributionRectangle> labelRectangles;
	
	public DistributionMapGraphicPanel(DistributionMap distributionMap, String[][] semanticTopics) {
		super();
		
		this.distributionMap = distributionMap;
		this.semanticTopics = semanticTopics;
		
		this.packageRectangles = new LinkedList<DistributionRectangle>();
		this.pckgLabelRectangles = new LinkedList<DistributionRectangle>();
		this.classRectangles = new LinkedList<DistributionRectangle>();
		this.labelRectangles = new LinkedList<DistributionRectangle>();
		
		this.buildDistributionMap();
		this.addMouseMotionListener(getMouseMotionListener());
		this.setLayout(new BorderLayout());
		
		this.externalView = new Rectangle2D.Double(5, 5, this.xBound, this.yBound);
		this.setSize(new Dimension(this.xBound, this.yBound));
		this.setPreferredSize(new Dimension(this.xBound, this.yBound));
		
		try {
			this.saveImage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void buildDistributionMap() {
		
		int packageIndex = 0;
		int packageX = packageSpace;
		int packageY = packageSpace;
		int maxPackageHeight = 0;
		
		for (String packageName : this.distributionMap.getPackages()) {
			List<String> classes = this.distributionMap.getClasses(packageName);
			int numClasses = classes.size();
			
//			int numColumns = Math.min(maxClasses, numClasses);
			int numColumns = maxClasses;
			int packageWidth = 2*packageStroke + 2*classSpace 
					+ numColumns*(2*classStroke + classSize) 
					+ (numColumns-1)*classSpace;
			int numLines = (int) Math.ceil((double) numClasses / maxClasses);
			int packageHeight = 2*packageStroke + 2*classSpace 
					+ numLines*(2*classStroke + classSize)
					+ (numLines-1)*classSpace;
			maxPackageHeight = Math.max(packageHeight, maxPackageHeight);
			
			this.packageRectangles.add(new DistributionRectangle(packageX, packageY, packageWidth, packageHeight, packageName));
			this.buildDistributionMap(classes, packageX, packageY);
			
			this.pckgLabelRectangles.add(new DistributionRectangle(packageX, packageY + packageHeight + packageStroke + 2*packageSpace, classSize, classSize, packageName));
			
			packageX += packageWidth + packageSpace;
			this.xBound = Math.max(this.xBound, packageX);
			this.yBound = Math.max(this.yBound, packageY + maxPackageHeight + packageSpace + classSize + packageSpace);
			
			packageIndex++;
			if (packageIndex == maxPackages) {
				packageX = packageSpace;
				packageY += maxPackageHeight + packageSpace + classSize + packageSpace;
				maxPackageHeight = 0;
				packageIndex = 0;
			}
		}
		
		// build labels
		int labelX = packageSpace;
		int labelY = this.yBound + packageSpace;
		for (int i = 0; i < this.semanticTopics.length; i++) {
			this.labelRectangles.add(new DistributionRectangle(labelX, labelY, classSize, classSize, "", i, this.semanticTopics[i]));
			labelY += classSize + classSpace;
		}
		
		this.yBound = labelY;
	}
	
	private void buildDistributionMap(List<String> classes, int packageX, int packageY) {
		
		int classIndex = 0;
		int classX = packageX + packageStroke + classSpace;
		int classY = packageY + packageStroke + classSpace;
		
		for (String className : classes) {
			
			int classWidth = 2*classStroke + classSize;
			int classHeight = classWidth;
			
			int clusterIndex = this.distributionMap.getCluster(className);
			String[] topics = (clusterIndex != -1) ? this.semanticTopics[clusterIndex] : new String[]{};
			this.classRectangles.add(new DistributionRectangle(classX, classY, classWidth, classHeight, className, clusterIndex, topics));
			
			classX += classWidth + classSpace;
			
			classIndex++;
			if (classIndex == maxClasses) {
				classX = packageX + packageStroke + classSpace;
				classY += classHeight + classSpace;
				classIndex = 0;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponents(g);
		Graphics2D graphics = (Graphics2D) g;
		
		// paint border
		graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(255, 255, 255));
		graphics.fill(this.externalView);
		
		// paint packages
		graphics.setColor(new Color(0, 0, 0));
		graphics.setStroke(new BasicStroke(packageStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (DistributionRectangle pckg : this.packageRectangles)
			graphics.draw(pckg);
		
		for (DistributionRectangle pckgLabel : this.pckgLabelRectangles) {
			graphics.setFont(graphics.getFont().deriveFont(12f));
			graphics.drawString(pckgLabel.getEntityName(), (int) pckgLabel.getX() /*+ classSize + packageSpace*/, (int) pckgLabel.getY() /*+ packageSpace + 2*classSpace*/);
		}
		
		// paint classes
		for (DistributionRectangle clazz : this.classRectangles) {
			graphics.setColor(new Color(0, 0, 0));
			graphics.setStroke(new BasicStroke(classStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setColor(clazz.getColor());
			graphics.fill(clazz);
		}
		
		// paint labels
		for (DistributionRectangle label : this.labelRectangles) {
			graphics.setColor(new Color(0, 0, 0));
			graphics.setStroke(new BasicStroke(classStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setColor(label.getColor());
			graphics.fill(label);
			
			graphics.setColor(new Color(0, 0, 0));
			graphics.setFont(graphics.getFont().deriveFont(12f));
			graphics.drawString(label.getClusterTopics(), (int) label.getX() + classSize + packageSpace, (int) label.getY() + packageSpace + 2*classSpace);
		}
	}
	
	public void saveImage() throws IOException {
		BufferedImage image = new BufferedImage(this.xBound, this.yBound, BufferedImage.TYPE_INT_RGB);
		this.paintComponent(image.getGraphics());

	    ImageIO.write(image, "PNG", new File(this.distributionMap.getProjectName() + ".png"));
	}
	
	private MouseMotionListener getMouseMotionListener() {
		return new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				for (DistributionRectangle clazz : classRectangles)
					if (clazz.contains(e.getPoint())) {
						setToolTipText(clazz.toString());
						ToolTipManager.sharedInstance().mouseMoved(e);
						return;
					}
				
				for (DistributionRectangle pckg : packageRectangles)
					if (pckg.contains(e.getPoint())) {
						setToolTipText(pckg.toString());
						ToolTipManager.sharedInstance().mouseMoved(e);
						return;
					}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		};
	}
	
	@Override
	public void update(Graphics g) {
		// TODO Auto-generated method stub
		super.update(g);
	}
}