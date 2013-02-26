package br.ufmg.aserg.topicviewer.control.correlation.clustering;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.ufmg.aserg.topicviewer.control.correlation.CorrelationMatrix;
import br.ufmg.aserg.topicviewer.util.DoubleMatrix2D;

public class HierarchicalClustering {

	private int numDocuments;
	private int maxClusters;
	private DisjointTree clustersTree;
//	private AgglomerativeLinkage linkage;
	
	private DoubleMatrix2D clusteredMatrix;
	private DoubleMatrix2D clusteredWithLinksMatrix;
	private Map<Integer, Integer> indexMapping;
	// TODO temporary to investigate clustering in exceptional cases
	private String[] documentIds;
	private StringBuffer unionBuffer;
	private static final String lineseparator = System.getProperty("line.separator");
	
	private int[][] clusters;
	
	public HierarchicalClustering(String projectName, CorrelationMatrix correlationMatrix, String[] documentIds, int numClusters) throws IOException {
		this.numDocuments = correlationMatrix.getNumEntities();
		this.maxClusters = numClusters;
		
		this.documentIds = documentIds;
		this.unionBuffer = new StringBuffer();
		
		this.clustersTree = new DisjointTree();
//		this.linkage = createLinkage();
		DoubleMatrix2D correlationMatrix2D = correlationMatrix.getCorrelationMatrix();
		
		int[] documents = new int[numDocuments];
		for (int i = 0; i < documents.length; i++) {
			documents[i] = i;
			clustersTree.makeSet(new Vertex(i));
		}
		
		initClustering(correlationMatrix2D.copy());
		this.indexMapping = ClusteredMatrixCalculator.generateIndexMapping(this.clusters);
		this.clusteredMatrix = ClusteredMatrixCalculator.generateClusteredMatrix(correlationMatrix2D, this.clusters, this.indexMapping);
		this.clusteredWithLinksMatrix = ClusteredMatrixCalculator.generateClusteredWithLinksMatrix(correlationMatrix2D, this.clusteredMatrix, this.indexMapping);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(projectName + "-clustering.txt"));
			writer.write(this.unionBuffer.toString());
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public DoubleMatrix2D getClusteredMatrix() {
		return this.clusteredMatrix;
	}
	
	public DoubleMatrix2D getClusteredWithLinksMatrix() {
		return this.clusteredWithLinksMatrix;
	}
	
	public Map<Integer, Integer> getIndexMapping() {
		return this.indexMapping;
	}
	
	public int[][] getClusters() {
		return this.clusters;
	}
	
	private void initClustering(DoubleMatrix2D correlationMatrix) {
		int numClusters = this.numDocuments;
		
		while (this.maxClusters < numClusters) {
			int[] leastDissimilarPair = getLeastDissimilarPair(correlationMatrix);
			Vertex set1 = this.clustersTree.findSet(new Vertex(leastDissimilarPair[0]));
			Vertex set2 = this.clustersTree.findSet(new Vertex(leastDissimilarPair[1]));
			
			if (set1 != set2) {
				String set1String = ""; String set2String = ""; 
				for (int i = 0; i < correlationMatrix.rows(); i++) {
					int set = this.clustersTree.findSet(new Vertex(i)).index;
					if (set == set1.index)
						set1String += (this.documentIds[i].lastIndexOf('.') != -1 ? this.documentIds[i].substring(this.documentIds[i].lastIndexOf('.')+1) : this.documentIds[i]) + " ";
					if (set == set2.index)
						set2String += (this.documentIds[i].lastIndexOf('.') != -1 ? this.documentIds[i].substring(this.documentIds[i].lastIndexOf('.')+1) : this.documentIds[i]) + " ";
				}
				this.unionBuffer.append("Union: [" + set1String + "] U [" + set2String + "]" + lineseparator);
				
				this.clustersTree.union(set1, set2);
				updateCorrelationMatrix(set1.index, correlationMatrix);
				numClusters--;
				System.out.println(numClusters);
			}
		}
		
		this.generateClusters();
	}
	
	private void generateClusters() {
		Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
		
		for (int i = 0; i < this.numDocuments; i++) {
			Vertex set = this.clustersTree.findSet(new Vertex(i));
			List<Integer> treeList = (mapping.containsKey(set.index) ? mapping.get(set.index) : new LinkedList<Integer>());
			treeList.add(i); mapping.put(set.index, treeList);
		}
		
		this.clusters = new int[mapping.keySet().size()][0];
		int i = 0;
		for (Integer set : mapping.keySet()) {
			List<Integer> tree = mapping.get(set);
			this.clusters[i] = new int[tree.size()];
			
			int j = 0;
			for (Integer v : tree) {
				this.clusters[i][j] = v; j++;
			}
			i++;
		}
	}
	
	private int[] getLeastDissimilarPair(DoubleMatrix2D correlationMatrix) {
		double bestSimilarity = Double.NEGATIVE_INFINITY;
		int[] leastDissimilarPair = {0,0};
		
		for (int i = 0; i < this.numDocuments; i++)
			for (int j = 0; j < this.numDocuments; j++) {
				double similarity = correlationMatrix.get(i, j);
				if (i < j && similarity != Double.NEGATIVE_INFINITY && similarity > bestSimilarity) {
					bestSimilarity = similarity;
					leastDissimilarPair = new int[] {i,j};
				}
			}
		
		return leastDissimilarPair;
	}
	
	private void updateCorrelationMatrix(int unionSet, DoubleMatrix2D correlationMatrix) {
		int union = this.clustersTree.findSet(new Vertex(unionSet)).index;
		
		Set<Pair<Integer, Integer>> calculatedClusters = new HashSet<Pair<Integer, Integer>>();
		for (int i = 0; i < correlationMatrix.rows()-1; i++)
			for (int j = i+1; j < correlationMatrix.rows(); j++) {
				int set1Index = this.clustersTree.findSet(new Vertex(i)).index;
				int set2Index = this.clustersTree.findSet(new Vertex(j)).index;
				
				Pair<Integer, Integer> newPair = (set1Index < set2Index) ? 
						new Pair<Integer, Integer>(set1Index, set2Index) : 
						new Pair<Integer, Integer>(set2Index, set1Index);
						
				if ((set1Index == union || set2Index == union) && !calculatedClusters.contains(newPair)) {
					double newValue;
					Set<Integer> set1 = getClusterSet(set1Index);
					Set<Integer> set2 = getClusterSet(set2Index);
					
					if (set1Index == set2Index) newValue = Double.NEGATIVE_INFINITY;
					else {
						double distance = 0D;
						for (int doc1 : set1)
							for (int doc2 : set2)
								distance += correlationMatrix.get(doc1, doc2);
						
						newValue = distance / (set1.size() * set2.size());
					}
					
					for (int doc1 : set1)
						for (int doc2 : set2) {
							correlationMatrix.set(doc1, doc2, newValue);
							correlationMatrix.set(doc2, doc1, newValue);
						}
					
					calculatedClusters.add(newPair);
				}
			}
	}
	
	private Set<Integer> getClusterSet(int rootIndex) {
		Set<Integer> clusterSet = new HashSet<Integer>();
		for (int i = 0; i < this.numDocuments; i++)
			if (this.clustersTree.findSet(new Vertex(i)).index == rootIndex)
				clusterSet.add(i);
		return clusterSet;
	}
	
//	protected AgglomerativeLinkage createLinkage() {
//		return new AgglomerativeLinkage() {
//			@Override
//			public double getNewDistance(int set1, int set2, double distance1, double distance2) {
//				double numerator = (set1 * distance1) + (set2 * distance2); 
//				double denominator = set1 + set2;
//				return (numerator / denominator);
//			}
//		};
//	}
//	
//	interface AgglomerativeLinkage {
//		public double getNewDistance(int setSize1, int setSize2, double distance1, double distance2);
//	}

	static class Vertex {
		int index;
		int rank;
		Vertex parent;
		
		public Vertex(int index) {
			this.index = index;
			this.rank = 0;
			this.parent = this;
		}
		
		@Override
		public boolean equals(Object obj) {
			return this.index == ((Vertex) obj).index;
		}
		
		@Override
		public int hashCode() {
			return this.index;
		}
	}
	
	static class DisjointTree {
		Map<Vertex, Vertex> vertexMapping = new HashMap<Vertex, Vertex>();
		
		public void makeSet(Vertex v) {
			vertexMapping.put(v, v);
		}
		
		public Vertex findSet(Vertex v) {
			Vertex mapped = vertexMapping.get(v);
			if (mapped == null) return null;
			if (v != mapped.parent) mapped.parent = findSet(mapped.parent);
			return mapped.parent;
		}
		
		public void union(Vertex v1, Vertex v2) {
			Vertex set1 = findSet(v1); Vertex set2 = findSet(v2);
			if (set1 == null || set2 == null || set1 == set2) return;
			Vertex mapped1 = vertexMapping.get(set1);
			Vertex mapped2 = vertexMapping.get(set2);
			
			if (mapped1.rank > mapped2.rank) {
				mapped2.parent = v1;
			} else {
				mapped1.parent = v2;
				if (mapped1.rank == mapped2.rank) mapped2.rank++;
			}
		}
	}
	
	public class Pair<A, B> {
	    private A first;
	    private B second;

	    public Pair(A first, B second) {
	    	super();
	    	this.first = first;
	    	this.second = second;
	    }

	    public int hashCode() {
	    	int hashFirst = first != null ? first.hashCode() : 0;
	    	int hashSecond = second != null ? second.hashCode() : 0;
	    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
	    }

	    @SuppressWarnings("rawtypes")
		public boolean equals(Object other) {
	    	if (other instanceof Pair) {
	    		Pair otherPair = (Pair) other;
	    		return 
	    		((  this.first == otherPair.first ||
	    			( this.first != null && otherPair.first != null &&
	    			  this.first.equals(otherPair.first))) &&
	    		 (	this.second == otherPair.second ||
	    			( this.second != null && otherPair.second != null &&
	    			  this.second.equals(otherPair.second))) );
	    	}

	    	return false;
	    }
	}
}