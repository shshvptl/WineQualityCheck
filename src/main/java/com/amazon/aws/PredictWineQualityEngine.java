package com.amazon.aws;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.tuning.CrossValidator;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.StructType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PredictWineQualityEngine {

    public static void main(String[] args) throws Exception {
        Utility utility = new Utility();

        String name = "Predict Wine Quality Application";

        SparkSession spark = null;
        spark = SparkSession.builder()
                .master("spark://ip-172-31-16-165.ec2.internal:7077")
                .appName(name)
                .getOrCreate();
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        jsc.setLogLevel("ERROR");

        StructType schema = (StructType) StructType.fromJson(utility.getSchema());

        //reading training dataset file
        Dataset<Row> wineDf = spark.read()
                .format("csv")
                .schema(schema)
                .option("header", true)
                .option("escape", "\"")
                .option("delimiter", ";")
                .option("mode", "PERMISSIVE")
                .option("path", "/home/ubuntu/CS643_Project2/Data/TrainingDataset.csv")
                .option("ignoreLeadingWhiteSpace", "true")
                .option("ignoreTrailingWhiteSpace", "true")
                .load().cache();

        //creating a temporary view on wineDf
        wineDf.createOrReplaceTempView("wineDf");
        wineDf = spark.sql("select * from wineDf");

        //set quantity based on condition here in wineDF
        String[] featureCols = new String[]{"fixedAcidity", "volatileAcidity", "citricAcid", "residualSugar", "chlorides", "freeSulfurDioxide",
                "totalSulfurDioxide", "density", "pH", "sulphates", "alcohol"};

        VectorAssembler assembler = new VectorAssembler().setInputCols(featureCols).setOutputCol("features");
        Dataset<Row> df2 = assembler.transform(wineDf);
        System.out.println("Printing df2...");
        df2.show(10, false);

        //setting up label index
        StringIndexer labelIndexer = new StringIndexer().setInputCol("quality").setOutputCol("label");
        Dataset<Row> filterWineDf = labelIndexer.fit(df2).transform(df2);
        System.out.println("Printing filterWineDf...");
        filterWineDf.show(10, false);

        //reading testdata
        Dataset<Row> testData = spark.read()
                .format("csv")
                .schema(schema)
                .option("header", true)
                .option("escape", "\"")
                .option("delimiter", ";")
                .option("mode", "PERMISSIVE")
                .option("path", "/home/ubuntu/CS643_Project2/Data/ValidationDataset.csv")
                .option("ignoreLeadingWhiteSpace", "true")
                .option("ignoreTrailingWhiteSpace", "true")
                .load().cache();

        testData.createOrReplaceTempView("testData");
        testData = spark.sql("select * from testData");

        System.out.println("Printing testData...");
        testData.show(10, false);

        //set quantity based on condition here in wineDF
        Dataset<Row> df4 = assembler.transform(testData);
        System.out.println("Printing df4...");
        df4.show(10, false);

        Dataset<Row> filterTestdataDf = labelIndexer.fit(df4).transform(df4);
        System.out.println("Printing filterTestdataDf...");
        filterTestdataDf.show(10, false);

        RandomForestClassifier randomForestClassifier = new RandomForestClassifier().setImpurity("gini").setMaxDepth(3).setNumTrees(500).setFeatureSubsetStrategy("auto").setSeed(5043);
        RandomForestClassificationModel model = randomForestClassifier.fit(filterWineDf);
        MulticlassClassificationEvaluator evalutor = new MulticlassClassificationEvaluator().setLabelCol("label");
        Dataset<Row> predictions = model.transform(filterTestdataDf);
        System.out.println("Printing predictions...");
        predictions.show(10, false);

        System.out.println("Printing predictions label...");
        predictions.select("prediction", "label").show(false);

        double accuracy = evalutor.evaluate(predictions);
        System.out.println("Accuracy before pipeline fitting: " + accuracy);

        Dataset<Row> wrong = predictions.select("prediction", "label").where("prediction != label");

        System.out.println("Printing wrong count: " + wrong.count());

        long accuracyManual = 1 - (wrong.count() / filterTestdataDf.count());
        System.out.println("Printing accuracyManual: " + accuracyManual);

        MulticlassMetrics rm = new MulticlassMetrics(predictions.select("prediction", "label"));
        System.out.println("Weighted F1 score before pipeline fitting:" + rm.weightedFMeasure());

        LocalDateTime start = LocalDateTime.now();
        System.out.println("Start time: " + start.toString());

        ParamMap[] paramGrid = new ParamGridBuilder()
                .addGrid(randomForestClassifier.maxBins(), new int[]{25, 31})
                .addGrid(randomForestClassifier.maxDepth(), new int[]{5, 10})
                .addGrid(randomForestClassifier.numTrees(), new int[]{20, 60})
                .build();

        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{randomForestClassifier});

        CrossValidator cv = new CrossValidator()
                .setEstimator(pipeline)
                .setEvaluator(evalutor)
                .setEstimatorParamMaps(paramGrid)
                .setNumFolds(10);

        CrossValidatorModel pipeLineFittedModel = cv.fit(filterWineDf);
        Dataset<Row> predictions2 = pipeLineFittedModel.transform(filterTestdataDf);

        double accuracy2 = evalutor.evaluate(predictions2);
        System.out.println("Accuracy after pipeline fitting: " + accuracy2);

        MulticlassMetrics rm2 = new MulticlassMetrics(predictions2.select("prediction", "label"));
        System.out.println("Weighted F1 score after pipeline fitting:" + rm2.weightedFMeasure());
        System.out.format("Weighted precision = %f\n", rm2.weightedPrecision());
        System.out.format("Weighted recall = %f\n", rm2.weightedRecall());
        System.out.format("Weighted false positive rate = %f\n", rm2.weightedFalsePositiveRate());
        System.out.format("Weighted true positive rate = %f\n",rm2.weightedTruePositiveRate());
        System.out.format("Weighted accuracy = %f\n",rm2.accuracy());

        LocalDateTime end = LocalDateTime.now();
        System.out.println("End time: " + end.toString());
        System.out.println("Total time taken: " + ChronoUnit.MINUTES.between(start, end));
    }
}
