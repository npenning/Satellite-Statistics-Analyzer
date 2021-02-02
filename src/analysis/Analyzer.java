package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import display.Main;
import javafx.concurrent.Task;

public class Analyzer {

	//names of satellites by id
	static String[] names;
	//tracks connection per satellite pair
	static boolean[][] connectionTracker;
	static int[][] connectionDurationTracker;
	//tracks satellite connection count ([0] = connection count, [1] = max concurrent connections)
	static int[] satTracker;
	static int bucketCount, sliceCount = 4320, slicePerBucket;
	static int totalConnectionDuration;
	static int currentSlice, currentBucket;
	static ArrayList<Double> sliceDensity, clusteringCoefficient;
	static ArrayList<Integer> connectionDurations;
	//connection density per bucket. [bucket #][0 - max, 1 - min, 2 - avg]
	static double[][] bucketDensity, bucketConnectionDurations;
	static int[] bucketMaxConcurrentConnections;
	static int sliceConnectionCount, sliceComponentCount;
	static long possibleConnections, possibleTriangles, sliceTriangleCount;
	static ArrayList<Integer> componentDetector = new ArrayList<Integer>();


	static BucketStat[] buckets;

	public static Task<Void> analyze(File dataFile, File keyFile, int bucketCount) throws FileNotFoundException {
		Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            	int triangleDetector;
        		int[] concurrentConnections;
        		double currentDensity, densitySum = 0, componentSum = 0;
        		ArrayList<Integer> connectionIDList;

        		getNamesFromFile(keyFile);

        		buckets = new BucketStat[bucketCount];
        		for(int i = 0; i < buckets.length; i++)
        			buckets[i] = new BucketStat(names.length);

        		slicePerBucket = sliceCount/bucketCount;
        		sliceDensity = new ArrayList<Double>();
        		clusteringCoefficient = new ArrayList<Double>();
        		connectionDurations = new ArrayList<Integer>();
        		connectionTracker = new boolean[names.length][names.length];
        		connectionDurationTracker = new int[names.length][names.length];
        		bucketDensity = new double[bucketCount][3];
        		bucketMaxConcurrentConnections = new int[bucketCount];
        		for(int i = 0; i < bucketCount; i++)
        			bucketDensity[i][1] = 1;
        		possibleConnections = CombinatoricsUtils.binomialCoefficient(names.length, 2);
        		possibleTriangles = CombinatoricsUtils.binomialCoefficient(names.length, 3);
        		currentSlice = 0;
        		satTracker = new int[names.length];
        		concurrentConnections = new int[names.length];

        		//File inputData = new File("satData.txt");
        		Scanner s = new Scanner(dataFile);

        		//for each time slice
        		while(s.hasNextLine()) {
        			//reset slice specific values
        			sliceConnectionCount = 0;
        			sliceTriangleCount = 0;
        			sliceComponentCount = 0;
        			System.out.println(currentSlice);
        			String[] sliceData = s.nextLine().split("\\s");
        			
        			if(currentSlice < sliceCount*(9.0/12.0)){
        				for(int i = 0; i < sliceData.length; i+=2) {
            				int sat1ID = Integer.parseInt(sliceData[i]);
            				int sat2ID = Integer.parseInt(sliceData[i+1]);

            				//toggle internal connection tracker,
            				connectionTracker[sat1ID][sat2ID] = !connectionTracker[sat1ID][sat2ID];
            				concurrentConnections[sat1ID] += (connectionTracker[sat1ID][sat2ID]?1:-1);
        					concurrentConnections[sat2ID] += (connectionTracker[sat1ID][sat2ID]?1:-1);
        				}
        				updateProgress(currentSlice, sliceCount);
        				currentSlice++;
        				
        				continue;
        			}
        			if(currentBucket == 3)
    				{
    					return null;
    				}

        			//for each satellite pair toggle
        			for(int i = 0; i < sliceData.length; i+=2) {
        				int sat1ID = Integer.parseInt(sliceData[i]);
        				int sat2ID = Integer.parseInt(sliceData[i+1]);
        				if(sat1ID == 2624 && sat2ID == 2625)
        					System.out.println("Flag: " + currentSlice);
        				//toggle internal connection tracker,
        				connectionTracker[sat1ID][sat2ID] = !connectionTracker[sat1ID][sat2ID];

        				//if connection is active, update connection count
        				if(connectionTracker[sat1ID][sat2ID]) {
        					satTracker[sat1ID] += 1;
        					satTracker[sat2ID] += 1;
        					concurrentConnections[sat1ID] += 1;
        					concurrentConnections[sat2ID] += 1;
        				}
        				//if connection is closed, update duration/concurrency info
        				else {
    						connectionDurations.add(connectionDurationTracker[sat1ID][sat2ID]);
    						connectionDurationTracker[sat1ID][sat2ID] = 0;

    						concurrentConnections[sat1ID] -= 1;
    						concurrentConnections[sat2ID] -= 1;
        				}
        			}
        			connectionIDList = new ArrayList<Integer>();
        			//calculate network density & clustering coefficient for slice
        			for(int i = 0; i < connectionTracker.length; i++) {
        				triangleDetector = 0;
        				for(int j = i+1; j < connectionTracker[0].length; j++) {
        					if(connectionTracker[i][j]) {
        						sliceConnectionCount++;
        						connectionDurationTracker[i][j]++;
        						triangleDetector++;
        						connectionIDList.add(j);
        					}
        				}
        				//if a given sat is connected to more than one sat, check to see if any pair of connected sats is also connected (triangle exists)
        				if(triangleDetector > 1) {
        					for(int j = 0; j < connectionIDList.size(); j++) {
        						for(int k = j+1; k < connectionIDList.size(); k++) {
        							if(connectionTracker[connectionIDList.get(j)][connectionIDList.get(k)]) {
        								sliceTriangleCount++;
        							}
        						}
        					}
        				}
        				connectionIDList.clear();
        			}
        			System.out.println("Connections: " + sliceConnectionCount);
        			
        			componentDetector.clear();
        			//calculate number of components with size > 1
        			for(int i = 0; i < connectionTracker.length; i++) {
        				if(!componentDetector.contains(i) && findComponentMembers(i)){
        					sliceComponentCount++;
        				}
        			}
        			buckets[currentBucket].sliceComponentCount.add((double) sliceComponentCount);
        			componentSum += sliceComponentCount;

        			buckets[currentBucket].clusteringCoefficient.add(((double)sliceTriangleCount)/possibleTriangles);

        			//update density (max)/(min)/(sum for avg)
        			currentDensity = ((double)sliceConnectionCount)/possibleConnections;
        			buckets[currentBucket].sliceDensity.add(currentDensity);
        			buckets[currentBucket].updateDensityMinMax(currentDensity);
        			densitySum += currentDensity;

        			int ccMax = 0, ccMin = names.length, ccSum = 0;
        			for(int cc: concurrentConnections) {
        				ccSum += cc;
        				if(cc > ccMax)
        					ccMax = cc;
        				if(cc < ccMin)
        					ccMin = cc;
        			}
        			buckets[currentBucket].sliceMaxConcurrentConnections.add(ccMax);
        			buckets[currentBucket].sliceMinConcurrentConnections.add(ccMin);
        			buckets[currentBucket].sliceAvgConcurrentConnections.add(((double)ccSum)/concurrentConnections.length);
        			
        			currentSlice++;
        			//change bucket if needed, and update values
        			if((((double)currentSlice)%slicePerBucket) == 0) {

        				int[] connectionCountData = {0, names.length, 0};
            			for(int cc: satTracker) {
            				connectionCountData[2] += cc;
            				if(cc > connectionCountData[0])
            					connectionCountData[0] = cc;
            				if(cc < connectionCountData[1])
            					connectionCountData[1] = cc;
            			}
            			buckets[currentBucket].satelliteConnectionCount[0] = connectionCountData[0];
            			buckets[currentBucket].satelliteConnectionCount[1] = connectionCountData[1];
            			buckets[currentBucket].satelliteConnectionCount[2] = connectionCountData[2]/names.length;
            			satTracker = new int[names.length];

        				buckets[currentBucket].bucketDensity[2] = densitySum/slicePerBucket;
        				densitySum = 0;

        				buckets[currentBucket].bucketComponents[0] = Collections.max(buckets[currentBucket].sliceComponentCount);
        				buckets[currentBucket].bucketComponents[1] = Collections.min(buckets[currentBucket].sliceComponentCount);
        				buckets[currentBucket].bucketComponents[2] = componentSum/slicePerBucket;
        				componentSum = 0;

        				for(int i = 0; i < connectionDurationTracker.length; i++) {
        					for(int j = i+1; j < connectionDurationTracker.length; j++) {
        						if(connectionDurationTracker[i][j] != 0) {
        							connectionDurations.add(connectionDurationTracker[i][j]);
        							connectionDurationTracker[i][j] = 0;
        						}
        					}
        				}
        				buckets[currentBucket].bucketConnectionDurations[0] = Collections.max(connectionDurations);
        				buckets[currentBucket].bucketConnectionDurations[1] = Collections.min(connectionDurations);
        				int durSum = 0;
        				for(Integer dur: connectionDurations) {
        					durSum += dur;
        				}
        				buckets[currentBucket].bucketConnectionDurations[2] = ((double)durSum)/connectionDurations.size();
        				connectionDurations.clear();

        				buckets[currentBucket].finalize();

        				System.out.println(currentBucket + " complete");

        				File resultFile = new File("F:\\School\\Thesis\\results" + "\\" + (currentBucket < 10?"0":"") + currentSlice + ".json");
        				ObjectMapper mapper = new ObjectMapper();
        				try {
        					mapper.writeValue(resultFile, buckets[currentBucket]);
        				} catch (JsonGenerationException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				} catch (JsonMappingException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				} catch (IOException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}

        				currentBucket++;
        			}
        			updateProgress(currentSlice, sliceCount);

        		}
        		s.close();
        		Main.setResults(buckets);
        		return null;
            }
		};

		task.setOnSucceeded(e1 -> {
			Main.writeResultsToFile();
			Main.refreshCenterPane();
			Main.refreshLeftPane();
		});
		return task;
	}

	private static boolean findComponentMembers(int satID){
		boolean flag = false;
		for(int i = satID+1; i < connectionTracker[0].length; i++){
			if((connectionTracker[satID][i] || connectionTracker[i][satID]) && !componentDetector.contains(i)){
				componentDetector.add(i);
				findComponentMembers(i);
				flag = true;
			}
		}
		return flag;
	}

	private static void getNamesFromFile(File key) throws FileNotFoundException {

		//File key = new File("key.txt");
		Scanner s = new Scanner(key);
		ArrayList<String> satNames = new ArrayList<String>();

		while(s.hasNextLine()) {
			satNames.add(s.nextLine());
		}
		s.close();
		names = new String[satNames.size()];
		for(int i = 0; i < satNames.size(); i++)
			names[i] = satNames.get(i);
	}

}
