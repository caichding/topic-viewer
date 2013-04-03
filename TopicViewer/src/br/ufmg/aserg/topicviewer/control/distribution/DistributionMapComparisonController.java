package br.ufmg.aserg.topicviewer.control.distribution;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ptstemmer.exceptions.PTStemmerException;
import br.ufmg.aserg.topicviewer.control.measurement.metrics.AbstractSemanticTopicMetric;
import br.ufmg.aserg.topicviewer.control.measurement.metrics.TopicConcentration;
import br.ufmg.aserg.topicviewer.control.measurement.metrics.TopicFocus;
import br.ufmg.aserg.topicviewer.control.measurement.metrics.TopicSpread;
import br.ufmg.aserg.topicviewer.control.semantic.SemanticTopicsCalculator;
import br.ufmg.aserg.topicviewer.gui.distribution.DistributionMap;
import br.ufmg.aserg.topicviewer.gui.distribution.DistributionMapGraphicPanel;
import br.ufmg.aserg.topicviewer.util.DoubleMatrix2D;
import br.ufmg.aserg.topicviewer.util.FileUtilities;
import br.ufmg.aserg.topicviewer.util.Properties;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;

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
	
	private static int getStringIndex(String string, String[] stringArray) {
		for (int i = 0; i < stringArray.length; i++)
			if (string.equals(stringArray[i])) return i;
		return -1;
	}
	
	private static int getClusterIndex(int documentId, int[][] clusters) {
		for (int i = 0; i < clusters.length; i++)
			for (int docId : clusters[i])
				if (docId == documentId) return i;
		return -1;
	}
	
	// Retorna o vetor de termos do documento em um novo espa�o vetorial
	private static DoubleMatrix1D buildDocumentNewVectorSpace(DoubleMatrix1D document, String[] oldSpaceTerms, String[] newSpaceTerms, cern.colt.matrix.DoubleMatrix2D lsiTransformMatrix) {
		
		// a partir do vetor de termos da matriz original, construir o seu equivalente
		// no novo espa�o. termos n�o encontrados s�o dados com valor 0
		DoubleMatrix1D documentNewSpace = new SparseDoubleMatrix1D(newSpaceTerms.length);
		for (int i = 0; i < oldSpaceTerms.length; i++) {
			int newIndex = getStringIndex(oldSpaceTerms[i], newSpaceTerms);
			if (newIndex != -1)
				documentNewSpace.set(newIndex, document.get(i));
		}
		
		// com o vetor mapeado para o novo espa�o, � em seguida transformado para o 
		// espa�o reduzido pelo LSI
		Algebra matrixAlgebra = new Algebra();
		return documentNewSpace = matrixAlgebra.mult(lsiTransformMatrix, documentNewSpace);
	}
	
	private static double calculateCosineSimilarity(DoubleMatrix1D vector1, DoubleMatrix1D vector2) {
		double cosineSimilarity = vector1.zDotProduct(vector2);
		double denominator = Math.sqrt(vector1.zDotProduct(vector1) * vector2.zDotProduct(vector2));
		return denominator == 0 ? 1D : cosineSimilarity / denominator;
	}
	
	// Retorna a nova configura��o de clusters, dado o agrupamento de um projeto semente. Cada classe
	// da vers�o mais nova ser� alocada para o cluster (da vers�o antiga) mais similar
	private static int[][] getNewClustersFromFirstProject(String projectBefore, String projectAfter) throws IOException {
		
		// construindo conjunto de termos para o agrupamento antigo
		DoubleMatrix2D beforeTermDocMatrix = new DoubleMatrix2D(projectBefore.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + "-lsi.matrix");
		String[] termIds = FileUtilities.readTermIds(projectBefore + ".ids");
		int[][] clusteringBefore = FileUtilities.readClustering(projectBefore + ".clusters");
		
		// atribuindo classes da vers�o nova para o cluster mais similar da vers�o antiga, segundo a
		// similaridade do cosseno
		String lsiTransformFileName = projectBefore.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".lsi";
		cern.colt.matrix.DoubleMatrix2D lsiTransform = SemanticTopicsCalculator.getLsiTransformCopy(new DoubleMatrix2D(lsiTransformFileName));
				
		DoubleMatrix2D afterTermDocMatrix = new DoubleMatrix2D(projectAfter.replace(Properties.CORRELATION_MATRIX_OUTPUT, Properties.TERM_DOC_MATRIX_OUTPUT) + ".matrix");
		String[] afterTermIds = FileUtilities.readTermIds(projectAfter + ".ids");
		
		Map<Integer, List<Integer>> newClustering = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < clusteringBefore.length; i++) newClustering.put(i, new LinkedList<Integer>());
		for (int i = 0; i < afterTermDocMatrix.columns(); i++) {
			DoubleMatrix1D docVector = buildDocumentNewVectorSpace(afterTermDocMatrix.viewColumn(i), afterTermIds, termIds, lsiTransform);
			
			// calculando similaridades
			double[] similarities = new double[clusteringBefore.length]; 
			for (int j = 0; j < clusteringBefore.length; j++) {
				double numClasses = clusteringBefore[j].length;
				double similarity = 0D;
				for (int documentId : clusteringBefore[j])
					similarity += calculateCosineSimilarity(docVector, beforeTermDocMatrix.viewColumn(documentId));
				
				similarities[j] = similarity / numClasses;
			}
			
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
				clusters = getNewClustersFromFirstProject(projectBefore, project);
				String[] termIds = FileUtilities.readTermIds(project + ".ids");
				semanticTopics = SemanticTopicsCalculator.generateSemanticTopicsFromClasses(clusters, termIds, documentIds);
			}
			
			for (String documentId : allClassNames) {
				String packageName = documentId.substring(documentId.lastIndexOf(':')+1, documentId.lastIndexOf('.'));
				String className = documentId.substring(documentId.lastIndexOf('.')+1);
				
				int documentIndex = getStringIndex(documentId, documentIds);
				int cluster = (documentIndex != -1) ? getClusterIndex(documentIndex, clusters) : -1;
				distributionMap.put(packageName, packageName + "." + className, cluster);
			}
			
			distributionMap.organize();
        	new DistributionMapGraphicPanel(distributionMap, semanticTopics);
        	
        	// calculating metrics - Topic Concentration on Packages
        	AbstractSemanticTopicMetric concentration = new TopicConcentration(distributionMap, semanticTopics);
        	List<Map<String, Double>> results = new LinkedList<Map<String, Double>>();
			results.add(concentration.calculate());
			FileUtilities.saveMetricResults(new String[]{"Concentration"}, results, project + "-package.results");
			
			// calculating metrics - Topic Spread and Focus
			AbstractSemanticTopicMetric spread = new TopicSpread(distributionMap, semanticTopics);
			AbstractSemanticTopicMetric focus = new TopicFocus(distributionMap, semanticTopics);
			
        	results = new LinkedList<Map<String, Double>>();
			results.add(spread.calculate());
			results.add(focus.calculate());
			FileUtilities.saveMetricResults(new String[]{"Spread", "Focus"}, results, project + "-topic.results");
        	
        	if (projectBefore == null) projectBefore = project;
        	System.out.println("Merged Project: " + project);
		}
	}
}