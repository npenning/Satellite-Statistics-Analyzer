package display;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collection;

import analysis.Analyzer;
import analysis.BucketStat;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main extends Application implements EventHandler<ActionEvent>{

	static BucketStat[] results;
	File inputData, inputKey, resultData;
	Alert popup;
	static ObjectMapper mapper = new ObjectMapper();

	//top pane elements
	HBox topPane;
	Button btnLoadData, btnLoadKey, btnAnalyze, btnLoadResults, btnResultsFileSelect;
	FileChooser fileSelect;
	DirectoryChooser dSelect;
	Label lblDataFilePath, lblKeyFilePath, lblBucketCount, lblResultsFilePath;
	ChoiceBox<Integer> bucketCountSelector;

	//bottom pane elements
	HBox bottomPane;
	Label lblProgress;
	static ProgressBar progress;

	//left pane elements
	static ListView<String> boxSelector;

	//center pane elements
	static ScrollPane dataScroller;
	static GridPane[] bucketSheets;
	private static DecimalFormat threeDec = new DecimalFormat(".###");

	public static void main(String[] args) {
		launch(args);

	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("SatNet Analyzer");

		setupTopPane(primaryStage);
		setupBottomPane(primaryStage);
		boxSelector = new ListView<String>();
		boxSelector.setBackground(new Background(new BackgroundFill(Color.web("#d9d9d9"), CornerRadii.EMPTY, Insets.EMPTY)));
		boxSelector.getSelectionModel().selectedItemProperty().addListener(e -> {
			int selectedIndex = boxSelector.getSelectionModel().getSelectedIndex();
			if(selectedIndex < results.length){
				dataScroller.setContent(bucketSheets[selectedIndex]);
			}
			else{
				dataScroller.setContent(bucketSheets[bucketSheets.length-1]);
			}
		});
		/*
		boxSelector.getItems().clear();
		int bucketLength = 24/6;
		for(int i = 0; i < 6; i++){
			int hour = i * bucketLength;
			boxSelector.getItems().add("B" + i + ": " + (hour<10?"0":"") + hour + ":00 - " + (hour + (bucketLength-1)<10?"0":"") + (hour + (bucketLength-1)) + ":59");
		}
		refreshCenterPane();
		*/
		dataScroller = new ScrollPane();

		BorderPane rootPane = new BorderPane();
		rootPane.setTop(topPane);
		rootPane.setBottom(bottomPane);
		rootPane.setLeft(boxSelector);
		rootPane.setCenter(dataScroller);


		Scene scene = new Scene(rootPane, 1300, 800);
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public void warning(String warningText){
		popup = new Alert(AlertType.WARNING);
		popup.setContentText(warningText);
		popup.show();
	}

	public void error(String errorText){
		popup = new Alert(AlertType.ERROR);
		popup.setContentText(errorText);
		popup.show();
	}

	private void setupTopPane(Stage primaryStage){
		//Top pane Setup
		fileSelect = new FileChooser();
		fileSelect.setInitialDirectory(new File("F:\\School\\Thesis"));
		dSelect = new DirectoryChooser();
		dSelect.setInitialDirectory(new File("F:\\School\\Thesis"));

		btnLoadData = new Button("Select Data File");
		btnLoadData.setOnAction(e -> {
            inputData = fileSelect.showOpenDialog(primaryStage);
            lblDataFilePath.setText(inputData.getName());
        });

		lblDataFilePath = new Label("...");
		lblDataFilePath.setFont(new Font(lblDataFilePath.getFont().getName(), 18));
		lblDataFilePath.setAlignment(Pos.CENTER);

		btnLoadKey = new Button("Select Key File");
		btnLoadKey.setOnAction(e -> {
            inputKey = fileSelect.showOpenDialog(primaryStage);
            lblKeyFilePath.setText(inputKey.getName());
        });

		lblKeyFilePath = new Label("...");
		lblKeyFilePath.setFont(new Font(lblKeyFilePath.getFont().getName(), 18));
		lblKeyFilePath.setAlignment(Pos.CENTER);

		lblBucketCount = new Label("Number of Buckets:");
		lblBucketCount.setFont(new Font(lblBucketCount.getFont().getName(), 18));
		lblBucketCount.setAlignment(Pos.CENTER);

		bucketCountSelector = new ChoiceBox<Integer>();
		bucketCountSelector.getItems().add(1);
		bucketCountSelector.getItems().add(2);
		bucketCountSelector.getItems().add(4);
		bucketCountSelector.getItems().add(6);
		bucketCountSelector.getItems().add(12);
		bucketCountSelector.getItems().add(24);
		bucketCountSelector.setValue(12);

		btnAnalyze = new Button("Analyze");
		btnAnalyze.setOnAction(e -> {
            if(inputData == null){
            	warning("Please select a data file.");
            }
            else if(inputKey == null){
            	warning("Please select a key file.");
            }
            else{
            		Task<Void> analysis;
					try {
						analysis = Analyzer.analyze(inputData, inputKey, bucketCountSelector.getValue());
						progress.progressProperty().bind(analysis.progressProperty());
						new Thread(analysis).start();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

            }
        });

		lblResultsFilePath = new Label("...");
		lblResultsFilePath.setFont(new Font(lblResultsFilePath.getFont().getName(), 18));
		lblResultsFilePath.setAlignment(Pos.CENTER);

		btnResultsFileSelect = new Button("Select Results Folder");
		btnResultsFileSelect.setOnAction(e -> {
            resultData = dSelect.showDialog(primaryStage);
            lblResultsFilePath.setText(resultData.getName());
        });


		btnLoadResults = new Button("Load Results from File");
		btnLoadResults.setOnAction(e -> {
            if(resultData == null){
            	warning("Please select a results file.");
            }
            else{
            	loadResultsFromFile(resultData);
            }
        });

		topPane = new HBox();
		topPane.setPadding(new Insets(15, 12, 15, 12));
		topPane.setSpacing(10);
		topPane.setAlignment(Pos.CENTER);
		topPane.setBackground(new Background(new BackgroundFill(Color.web("#c7c7c7"), CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.getChildren().addAll(btnLoadData, lblDataFilePath, btnLoadKey, lblKeyFilePath, lblBucketCount, bucketCountSelector, btnAnalyze, btnResultsFileSelect, lblResultsFilePath, btnLoadResults);
	}

	private void setupBottomPane(Stage primaryStage) {

		bottomPane = new HBox();
		bottomPane.setPadding(new Insets(15, 12, 15, 12));
		bottomPane.setSpacing(10);
		bottomPane.setAlignment(Pos.CENTER);
		bottomPane.setBackground(new Background(new BackgroundFill(Color.web("#c7c7c7"), CornerRadii.EMPTY, Insets.EMPTY)));

		lblProgress = new Label("Analysis :");
		lblProgress.setFont(new Font(lblKeyFilePath.getFont().getName(), 18));

		progress = new ProgressBar(0);
		progress.setPrefWidth(1100);


		bottomPane.getChildren().addAll(lblProgress, progress);
	}

	public static void refreshLeftPane() {
		boxSelector.getItems().clear();
		int bucketLength = 24/results.length;
		for(int i = 0; i < results.length; i++){
			int hour = i * bucketLength;
			boxSelector.getItems().add("B" + i + ": " + (hour<10?"0":"") + hour + ":00 - " + (hour + (bucketLength-1)<10?"0":"") + (hour + (bucketLength-1)) + ":59");
		}
		boxSelector.getItems().add("Overall");
		boxSelector.getSelectionModel().select(0);
		boxSelector.getFocusModel().focus(0);

	}

	public static void refreshCenterPane() {
		bucketSheets = new GridPane[results.length+1];
		GridPane tPane;
		BarChart<String, Number> tBar;
		LineChart<Number, Number> tLine;
		XYChart.Series<String, Number> barSeries;
		XYChart.Series<Number, Number> lineSeries;
		NumberAxis sliceAxis = new NumberAxis();
		sliceAxis.setLabel("Slice (t)");
		NumberAxis yAxis = new NumberAxis();

		for(int i = 0; i < results.length; i++){
			tPane = new GridPane();

			//connection sum
			yAxis = new NumberAxis();
			yAxis.setLabel("# of Connections for indv. Sats");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Individual Sat Connection Count");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].satelliteConnectionCount[0]) + ")", results[i].satelliteConnectionCount[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].satelliteConnectionCount[2]) + ")", results[i].satelliteConnectionCount[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].satelliteConnectionCount[1]) + ")", results[i].satelliteConnectionCount[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 0, 0);

			//connection duration
			yAxis = new NumberAxis();
			yAxis.setLabel("Connection Duration (t)");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Connection Duration");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].bucketConnectionDurations[0]) + ")", results[i].bucketConnectionDurations[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].bucketConnectionDurations[2]) + ")", results[i].bucketConnectionDurations[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].bucketConnectionDurations[1]) + ")", results[i].bucketConnectionDurations[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 1, 0);

			//density
			yAxis = new NumberAxis();
			yAxis.setLabel("Graph Density");
			sliceAxis = new NumberAxis();
			sliceAxis.setLabel("Slice (t)");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Density per Time Slice");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].sliceDensity.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].sliceDensity.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 0, 1);
			//tPane.add(tLine, 0, 0);
			
			yAxis = new NumberAxis();
			yAxis.setLabel("Density");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Density Max/Min/Avg");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].bucketDensity[0]) + ")", results[i].bucketDensity[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].bucketDensity[2]) + ")", results[i].bucketDensity[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].bucketDensity[1]) + ")", results[i].bucketDensity[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 1, 1);

			//clustering coefficient
			yAxis = new NumberAxis();
			yAxis.setLabel("Clustering Coefficient");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Clustering Coefficient per Time Slice");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].clusteringCoefficient.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].clusteringCoefficient.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 0, 2);
			//tPane.add(tLine, 1, 0);

			yAxis = new NumberAxis();
			yAxis.setLabel("Clustering Coefficient");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Clustering Coefficient Max/Min/Avg");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].bucketDensity[0]) + ")", results[i].bucketClusteringCoefficient[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].bucketDensity[2]) + ")", results[i].bucketClusteringCoefficient[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].bucketDensity[1]) + ")", results[i].bucketClusteringCoefficient[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 1, 2);

			//density
			yAxis = new NumberAxis();
			yAxis.setLabel("# of Components with Order > 1");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Component count per Time Slice \n (Order > 1)");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].sliceComponentCount.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].sliceComponentCount.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 0, 3);
			//tPane.add(tLine, 0, 1);

			yAxis = new NumberAxis();
			yAxis.setLabel("# of Components with Order > 1");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Component Count Max/Min/Avg");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].bucketComponents[0]) + ")", results[i].bucketComponents[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].bucketComponents[2]) + ")", results[i].bucketComponents[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].bucketComponents[1]) + ")", results[i].bucketComponents[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 1, 3);

			//Max Concurrent Connections
			yAxis = new NumberAxis();
			yAxis.setLabel("Max Concurrent Connections (Slice)");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Max Concurrent Connections per Time Slice");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].sliceMaxConcurrentConnections.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].sliceMaxConcurrentConnections.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 0, 4);
			//tPane.add(tLine, 1, 1);

			//Min Concurrent Connections
			yAxis = new NumberAxis();
			yAxis.setLabel("Min Concurrent Connections (Slice)");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Min Concurrent Connections per Time Slice");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].sliceMinConcurrentConnections.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].sliceMinConcurrentConnections.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 1, 4);
			//tPane.add(tLine, 0, 2);

			//Avg Concurrent Connections
			yAxis = new NumberAxis();
			yAxis.setLabel("Avg Concurrent Connections (Slice)");
			tLine = new LineChart<Number, Number>(sliceAxis, yAxis);

			tLine.setTitle("Avg Concurrent Connections per Time Slice");
			tLine.setLegendVisible(false);
			tLine.setCreateSymbols(false);

			lineSeries = new XYChart.Series<>();
			for(int j = 0; j < results[i].sliceAvgConcurrentConnections.size(); j++){
				lineSeries.getData().add(new XYChart.Data<Number, Number>( j, results[i].sliceAvgConcurrentConnections.get(j)));
			}
			tLine.getData().add(lineSeries);

			tPane.add(tLine, 0, 5);
			//tPane.add(tLine, 1, 2);

			//Concurrent Connections M/M/A
			yAxis = new NumberAxis();
			yAxis.setLabel("Concurrent Connections");
			tBar = new BarChart<>(new CategoryAxis(), yAxis);

			tBar.setTitle("Concurrent Connections Max/Min/Avg");
			tBar.setLegendVisible(false);

			barSeries = new XYChart.Series<>();

			barSeries.getData().add(new XYChart.Data<>("Max (" + threeDec.format(results[i].bucketConcurrentConnections[0]) + ")", results[i].bucketConcurrentConnections[0]));
			barSeries.getData().add(new XYChart.Data<>("Avg (" + threeDec.format(results[i].bucketConcurrentConnections[2]) + ")", results[i].bucketConcurrentConnections[2]));
			barSeries.getData().add(new XYChart.Data<>("Min (" + threeDec.format(results[i].bucketConcurrentConnections[1]) + ")", results[i].bucketConcurrentConnections[1]));

			tBar.getData().add(barSeries);

			tPane.add(tBar, 1, 5);


			bucketSheets[i] = tPane;
		}
		dataScroller.setContent(bucketSheets[0]);

	}

	public static void setResults(BucketStat[] aResults){
		results = aResults;
	}

	public static void writeResultsToFile(){
		/*
		File resultFolder = new File("F:\\School\\Thesis\\results");
		resultFolder.mkdir();
		File resultFile;
		String path;

		for(int i = 0; i < results.length; i++){
			path = resultFolder.getAbsolutePath() + "\\" + (i < 10?"0":"") + i + ".json";
			resultFile = new File(path);

			try {
				mapper.writeValue(resultFile, results[i]);
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
		}
		*/

	}

	private void loadResultsFromFile(File resultsFile) {
		results = new BucketStat[resultsFile.listFiles().length];
		int i = 0;
		for (final File f : resultsFile.listFiles()) {
			try {
				results[i++] = mapper.readValue(f, analysis.BucketStat.class);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		refreshLeftPane();
		refreshCenterPane();

	}

	@Override
	public void handle(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}


}
















































