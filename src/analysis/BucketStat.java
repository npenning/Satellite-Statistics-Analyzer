package analysis;

import java.util.ArrayList;
import java.util.Collections;

public class BucketStat {
	public ArrayList<Double> sliceDensity, clusteringCoefficient, sliceAvgConcurrentConnections, sliceComponentCount;
	public ArrayList<Integer> sliceMaxConcurrentConnections, sliceMinConcurrentConnections;
	//connection density [0 - max, 1 - min, 2 - avg]
	public double[] bucketDensity, bucketConnectionDurations, bucketComponents, bucketClusteringCoefficient;
	public double[] satelliteConnectionCount;
	public double[] bucketConcurrentConnections;

	public BucketStat(){

	}

	public BucketStat(int satCount) {
		sliceDensity = new ArrayList<Double>();
		clusteringCoefficient = new ArrayList<Double>();
		sliceAvgConcurrentConnections = new ArrayList<Double>();
		sliceMaxConcurrentConnections = new ArrayList<Integer>();
		sliceMinConcurrentConnections = new ArrayList<Integer>();
		sliceComponentCount = new ArrayList<Double>();
		bucketDensity = new double[3];
		bucketConnectionDurations = new double[3];
		bucketComponents = new double[3];
		satelliteConnectionCount = new double[3];
		bucketClusteringCoefficient = new double[3];
		bucketConcurrentConnections = new double[3];
		bucketDensity[1] = 1;
		bucketComponents[1] = satCount;
	}

	public void updateDensityMinMax(double density) {
		if(density > bucketDensity[0])
			bucketDensity[0] = density;
		if(density < bucketDensity[1])
			bucketDensity[1] = density;
	}

	public void updateComponentMinMax(double componentCount) {
		if(componentCount > bucketComponents[0])
			bucketComponents[0] = componentCount;
		if(componentCount < bucketComponents[1])
			bucketComponents[1] = componentCount;
	}

	public void finalize(){
		bucketClusteringCoefficient[0] = Collections.max(clusteringCoefficient);
		bucketClusteringCoefficient[1] = Collections.min(clusteringCoefficient);
		double ccSum = 0;
		for(double cc: clusteringCoefficient){
			ccSum += cc;
		}
		bucketClusteringCoefficient[2] = ccSum/clusteringCoefficient.size();

		bucketConcurrentConnections[0] = Collections.max(sliceMaxConcurrentConnections);
		bucketConcurrentConnections[1] = Collections.min(sliceMinConcurrentConnections);
		ccSum = 0;
		for(double cc: sliceAvgConcurrentConnections){
			ccSum += cc;
		}
		bucketConcurrentConnections[2] = ccSum/sliceAvgConcurrentConnections.size();
	}

}
