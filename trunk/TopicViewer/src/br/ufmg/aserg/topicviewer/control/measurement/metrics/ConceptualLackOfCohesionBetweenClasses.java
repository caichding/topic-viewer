package br.ufmg.aserg.topicviewer.control.measurement.metrics;

import java.util.List;

import cern.colt.matrix.DoubleMatrix2D;

public class ConceptualLackOfCohesionBetweenClasses extends AbstractConceptualMetric {
	
	static class SimilarityGraph {
		
		boolean[][] graph;
		boolean[] vertices;
		
		public SimilarityGraph(int numVertices) {
			this.graph = new boolean[numVertices][numVertices];
			this.vertices = new boolean[numVertices];
		}
		
		public void insertEdge(int vertex1, int vertex2) {
			this.graph[vertex1][vertex2] = this.graph[vertex2][vertex1] = true;
		}
		
		public int getNoCC() {
			int nocc = 0;
			for (int i = 0; i < vertices.length; i++)
				if (!vertices[i]) {
					dfs(i);
					nocc++;
				}
			return nocc;
		}
		
		private void dfs(int vertex) {
			vertices[vertex] = true;
			for (int i = 0; i < vertices.length; i++)
				if (graph[i][vertex] && !vertices[i])
					dfs(i);
		}
	}
	
//    public CC(Graph G) {
//        marked = new boolean[G.V()];
//        id = new int[G.V()];
//        size = new int[G.V()];
//        for (int v = 0; v < G.V(); v++) {
//            if (!marked[v]) {
//                dfs(G, v);
//                count++;
//            }
//        }
//    }
//
//    // depth first search
//    private void dfs(Graph G, int v) {
//        marked[v] = true;
//        id[v] = count;
//        size[count]++;
//        for (int w : G.adj(v)) {
//            if (!marked[w]) {
//                dfs(G, w);
//            }
//        }
//    }

	public ConceptualLackOfCohesionBetweenClasses(DoubleMatrix2D termDocMatrix, String[] documentIds) {
		super("CLCOC5", termDocMatrix, documentIds);
	}

	@Override
	protected double calculate(String packageName) {
		List<Integer> classes = this.packageMapping.get(packageName);
		SimilarityGraph graph = new SimilarityGraph(classes.size());
		
		for (int i = 0; i < classes.size(); i++)
			for (int j = i+1; j < classes.size(); j++)
				if (calculateSimilarity(classes.get(i), classes.get(j)) >= similarityThreshold)
					graph.insertEdge(i, j);
		
		return graph.getNoCC();
	}
}