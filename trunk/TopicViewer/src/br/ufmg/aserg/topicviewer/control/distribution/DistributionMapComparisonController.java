package br.ufmg.aserg.topicviewer.control.distribution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ptstemmer.exceptions.PTStemmerException;

import br.ufmg.aserg.topicviewer.control.semantic.SemanticTopicsCalculator;
import br.ufmg.aserg.topicviewer.gui.distribution.DistributionMap;
import br.ufmg.aserg.topicviewer.gui.distribution.DistributionMapGraphicPanel;
import br.ufmg.aserg.topicviewer.util.DoubleMatrix2D;
import br.ufmg.aserg.topicviewer.util.FileUtilities;
import br.ufmg.aserg.topicviewer.util.Properties;
import cern.colt.matrix.DoubleMatrix1D;

public class DistributionMapComparisonController {

	private static List<String> getAllDocumentIds(String[] projects) throws IOException {
		List<String> allDocumentIds = new LinkedList<String>();
		
		for (String projectName : projects) {
			String[] documentIds = FileUtilities.readDocumentIds(projectName + ".ids");
			for (String documentId : documentIds)
				if (!allDocumentIds.contains(documentId))
					allDocumentIds.add(documentId);
		}
		
		return allDocumentIds;
	}
	
	private static int getDocumentIndex(String documentId, String[] documentIds) {
		for (int i = 0; i < documentIds.length; i++)
			if (documentId.equals(documentIds[i])) return i;
		return -1;
	}
	
	private static int getClusterIndex(int documentId, int[][] clusters) {
		for (int i = 0; i < clusters.length; i++)
			for (int docId : clusters[i])
				if (docId == documentId) return i;
		return -1;
	}
	
	private static Set<String> buildTermsSet(int[] cluster, String[] termIds, DoubleMatrix2D termDocMatrix) {
		Set<String> terms = new HashSet<String>();
		for (int documentId : cluster)
			terms.addAll(buildTermsSet(termDocMatrix.viewColumn(documentId), termIds));
		return terms;
	}
	
	private static Set<String> buildTermsSet(DoubleMatrix1D document, String[] termIds) {
		Set<String> terms = new HashSet<String>();
		for (int i = 0; i < document.size(); i++)
			if (document.get(i) > 0D) terms.add(termIds[i]);
		return terms;
	}
	
	private static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
		double intersect = 0D; double union = set1.size();
		
		for (String term : set1)
			if (set2.contains(term)) intersect++;
		for (String term : set2)
			if (!set1.contains(term)) union++;
		
		return intersect / union;
	}
	
	// Retorna a nova configura��o de clusters, dado o agrupamento de um projeto semente. Cada classe
	// da vers�o mais nova ser� alocada para o cluster (da vers�o antiga) mais similar
	private static int[][] getNewClustersFromFirstProject(String projectBefore, String projectAfter) throws IOException {
		
		// construindo conjunto de termos para o agrupamento antigo
		DoubleMatrix2D termDocMatrix = new DoubleMatrix2D(projectBefore.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".matrix");
		String[] termIds = FileUtilities.readTermIds(projectBefore + ".ids");
		int[][] clusteringBefore = FileUtilities.readClustering(projectBefore + ".clusters");
		
		List<Set<String>> clustersBefore = new ArrayList<Set<String>>();
		for (int i = 0; i < clusteringBefore.length; i++)
			clustersBefore.add(buildTermsSet(clusteringBefore[i], termIds, termDocMatrix));
		
		// atribuindo classes da vers�o nova para o cluster mais similar da vers�o antiga, segundo a
		// similaridade de Jaccard. N�o foi usada a similaridade do cosseno pois se tratam de dois es
		// pa�os vetoriais diferentes
		termDocMatrix = new DoubleMatrix2D(projectAfter.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".matrix");
		termIds = FileUtilities.readTermIds(projectAfter + ".ids");
		
		Map<Integer, List<Integer>> newClustering = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < clusteringBefore.length; i++) newClustering.put(i, new LinkedList<Integer>());
		for (int i = 0; i < termDocMatrix.columns(); i++) {
			Set<String> terms = buildTermsSet(termDocMatrix.viewColumn(i), termIds);
			
			// calculando similaridades
			double[] similarities = new double[clusteringBefore.length]; 
			for (int j = 0; j < clusteringBefore.length; j++)
				similarities[j] = calculateJaccardSimilarity(terms, clustersBefore.get(j));
			
			// verificando qual cluster � mais similar
			int newClusterIndex = -1;
			double bestSimilarity = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < similarities.length; j++)
				if (similarities[j] > bestSimilarity) {
					bestSimilarity = similarities[j];
					newClusterIndex = j;
				}
			
			newClustering.get(newClusterIndex).add(i);
		}
		
		// transformando em array
		int[][] newClusters = new int[clusteringBefore.length][0];
		for (int i = 0; i < clusteringBefore.length; i++) {
			newClusters[i] = new int[newClustering.get(i).size()];
			int j = 0;
			for (Integer classId : newClustering.get(i)) {
				newClusters[i][j] = classId; 
				j++;
			}
		}
		
		return newClusters;
	}
	
	// Retorna a ordena��o dos novos clusters, em rela��o ao clustering da vers�o anterior do projeto
	// O mapeamento serve para reordenar tanto a matriz de clustering (cluster x classes) quanto a ma
	// triz que contem os t�picos sem�nticos
	private static List<Integer> getNewClusters(String projectBefore, String projectAfter) throws IOException {
		
		DoubleMatrix2D termDocMatrix = new DoubleMatrix2D(projectAfter.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".matrix");
		String[] termIds = FileUtilities.readTermIds(projectAfter + ".ids");
		int[][] clusteringAfter = FileUtilities.readClustering(projectAfter + ".clusters");

		// construindo conjunto de termos para o novo clustering
		List<Set<String>> clustersAfter = new ArrayList<Set<String>>();
		for (int i = 0; i < clusteringAfter.length; i++)
			clustersAfter.add(buildTermsSet(clusteringAfter[i], termIds, termDocMatrix));
		
		termDocMatrix = new DoubleMatrix2D(projectBefore.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".matrix");
		termIds = FileUtilities.readTermIds(projectBefore + ".ids");
		int[][] clusteringBefore = FileUtilities.readClustering(projectBefore + ".clusters");
		
		// para cada cluster antigo, calcular qual cluster novo � mais similar
		List<Integer> newClusters = new LinkedList<Integer>();
		for (int i = 0; i < clusteringBefore.length; i++) {
			Set<String> terms = buildTermsSet(clusteringBefore[i], termIds, termDocMatrix);
			
			// calculando similaridades
			double[] similarities = new double[clusteringAfter.length]; 
			for (int j = 0; j < clustersAfter.size(); j++)
				similarities[j] = calculateJaccardSimilarity(terms, clustersAfter.get(j));
			
			// verificando qual cluster � mais similar
			int newClusterIndex = -1;
			double bestSimilarity = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < similarities.length; j++)
				if (!newClusters.contains(j) && similarities[j] > bestSimilarity) {
					bestSimilarity = similarities[j];
					newClusterIndex = j;
				}
			
			newClusters.add(newClusterIndex);
		}
		
		return newClusters;
	}
	
	private static int[][] getNewClusters(int[][] oldClusters, List<Integer> newOrdering) {
		int[][] newClusters = new int[oldClusters.length][0];
		for (int i = 0; i < newClusters.length; i++)
			newClusters[i] = oldClusters[newOrdering.get(i)];
		return newClusters;
	}
	
	private static String[][] getNewTopics(String[][] oldTopics, List<Integer> newOrdering) {
		String[][] newTopics = new String[oldTopics.length][0];
		for (int i = 0; i < newTopics.length; i++)
			newTopics[i] = oldTopics[newOrdering.get(i)];
		return newTopics;
	}
	
	public static void main(String[] args) throws IOException, PTStemmerException {
		
		Properties.load();
		String[] projects = new String[args.length];
		for (int i = 0; i < args.length; i++)
			projects[i] = args[i].substring(0, args[i].lastIndexOf('.'));
		
		List<String> allClassNames = getAllDocumentIds(projects);
		System.out.println("Merged all document ids");
		
		String projectBefore = null;
		for (String project : projects) {
			DistributionMap distributionMap = new DistributionMap(project + "-merge");
			String[] documentIds = FileUtilities.readDocumentIds(project + ".ids");
			int[][] clusters = FileUtilities.readClustering(project + ".clusters");
			String[][] semanticTopics = FileUtilities.readSemanticTopics(project + ".topics");
			
			if (projectBefore != null) {
//				List<Integer> newOrdering = getNewClusters(projectBefore, project);
//				clusters = getNewClusters(clusters, newOrdering);
//				semanticTopics = getNewTopics(semanticTopics, newOrdering);
				
				clusters = getNewClustersFromFirstProject(projectBefore, project);
				String[] termIds = FileUtilities.readTermIds(project + ".ids");
				semanticTopics = SemanticTopicsCalculator.generateSemanticTopicsFromClasses(clusters, termIds, documentIds);
			}
			
			for (String documentId : allClassNames) {
				String packageName = documentId.substring(documentId.lastIndexOf(':')+1, documentId.lastIndexOf('.'));
				String className = documentId.substring(documentId.lastIndexOf('.')+1);
				
				int documentIndex = getDocumentIndex(documentId, documentIds);
				int cluster = (documentIndex != -1) ? getClusterIndex(documentIndex, clusters) : -1;
				distributionMap.put(packageName, packageName + "." + className, cluster);
			}
			
			distributionMap.organize();
			
        	new DistributionMapGraphicPanel(distributionMap, semanticTopics);
        	
        	if (projectBefore == null) projectBefore = project;
        	System.out.println("Merged Project: " + project);
		}
	}
}