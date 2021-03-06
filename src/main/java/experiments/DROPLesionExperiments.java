package experiments;

import optimizer.PCASkiingOptimizer;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import runner.PCARunner;
import smile.math.distance.EuclideanDistance;
import smile.math.distance.Metric;
import stats.KNN;
import utils.CSVTools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by meep_me on 9/1/16.
 */
public class DROPLesionExperiments extends Experiment {
    public static String baseString = "src/main/java/experiments/LesionExperiments/";
    public static DateFormat day = new SimpleDateFormat("MM-dd");
    public static DateFormat minute = new SimpleDateFormat("HH_mm");

    private static String runtimeOutFile(String dataset, double lbr, double qThresh, PCASkiingOptimizer.PCAAlgo algo, Date date){
        String output = String.format("%s_%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, algo, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/runtime/%s.csv", output);
    }

    private static String fullRuntimeOutFile(String dataset, double lbr, double qThresh, PCASkiingOptimizer.PCAAlgo algo, Date date){
        String output = String.format("%s_%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, algo, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/fullRuntime/%s.csv", output);
    }

    private static String kOutFile(String dataset, double lbr, double qThresh, PCASkiingOptimizer.PCAAlgo algo, Date date){
        String output = String.format("%s_%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, algo, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/k/%s.csv", output);
    }

    //java ${JAVA_OPTS} -cp "assembly/target/*:core/target/classes:frontend/target/classes:contrib/target/classes" macrobase.analysis.stats.experiments.SVDDropExperiments
    public static void main(String[] args) throws Exception {
        Date date = new Date();

        //Input arguments: data, LBR, numtrials, and opt or not
        String dataset = args[0];
        double lbr = Double.parseDouble(args[1]);
        int numTrials = Integer.parseInt(args[2]);
        System.out.println(dataset);
        System.out.println(lbr);
        System.out.println(numTrials);

        //Fixed arguments:
        double qThresh = 1.96;
        PCASkiingOptimizer.PCAAlgo[] algos = {PCASkiingOptimizer.PCAAlgo.TROPP, PCASkiingOptimizer.PCAAlgo.FAST};
        Metric<double[]> distanceMeasure = new EuclideanDistance();
        double propnTrain = 0.75;

        //PlaceHolders
        RealMatrix data;
        String labels[];
        KNN knnClassifier;
        PCARunner drop;

        double[] avgBaseRuntime = new double[numTrials];
        double[] avgBaseFullRuntime = new double[numTrials];
        double[] avgBaseK = new double[numTrials];

        double[] avgSampleRuntime = new double[numTrials];
        double[] avgSampleFullRuntime = new double[numTrials];
        double[] avgSampleK = new double[numTrials];

        double[] avgReuseRuntime = new double[numTrials];
        double[] avgReuseFullRuntime = new double[numTrials];
        double[] avgReuseK = new double[numTrials];

        double[] avgCostRuntime = new double[numTrials];
        double[] avgCostFullRuntime = new double[numTrials];
        double[] avgCostK = new double[numTrials];


        //load data and get labels as list
        CSVTools.DataLabel d = getLabeledData(dataset);
        data = new Array2DRowRealMatrix(d.matrix);
        labels = d.origLabels;

        for (PCASkiingOptimizer.PCAAlgo algo : algos) {
            //compute baseline
            for (int i = 0; i < numTrials; i++) {
                drop = new PCARunner(qThresh, lbr, algo, PCASkiingOptimizer.work.NOREUSE, PCASkiingOptimizer.optimize.NOOPTIMIZE, PCASkiingOptimizer.sampling.NOSAMPLE);
                drop.consume(data);
                avgBaseK[i] = drop.finalK();
                avgBaseRuntime[i] = drop.totalTime();

                d = new CSVTools.DataLabel(drop.getFinalTransform(), labels, data.getRowDimension(), drop.finalK());
                knnClassifier = new KNN(d, distanceMeasure, propnTrain);
                knnClassifier.runKNN(1);
                avgBaseFullRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            }

            //compute with sampling
            for (int i = 0; i < numTrials; i++) {
                drop = new PCARunner(qThresh, lbr, algo, PCASkiingOptimizer.work.NOREUSE, PCASkiingOptimizer.optimize.NOOPTIMIZE, PCASkiingOptimizer.sampling.SAMPLE);
                drop.consume(data);
                avgSampleK[i] = drop.finalK();
                avgSampleRuntime[i] = drop.totalTime();

                d = new CSVTools.DataLabel(drop.getFinalTransform(), labels, data.getRowDimension(), drop.finalK());
                knnClassifier = new KNN(d, distanceMeasure, propnTrain);
                knnClassifier.runKNN(1);
                avgSampleFullRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            }

            //compute with reuse
            for (int i = 0; i < numTrials; i++) {
                drop = new PCARunner(qThresh, lbr, algo, PCASkiingOptimizer.work.REUSE, PCASkiingOptimizer.optimize.NOOPTIMIZE, PCASkiingOptimizer.sampling.SAMPLE);
                drop.consume(data);
                avgReuseK[i] = drop.finalK();
                avgReuseRuntime[i] = drop.totalTime();

                d = new CSVTools.DataLabel(drop.getFinalTransform(), labels, data.getRowDimension(), drop.finalK());
                knnClassifier = new KNN(d, distanceMeasure, propnTrain);
                knnClassifier.runKNN(1);
                avgReuseFullRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            }

            //compute with cost function
            for (int i = 0; i < numTrials; i++) {
                drop = new PCARunner(qThresh, lbr, algo, PCASkiingOptimizer.work.REUSE, PCASkiingOptimizer.optimize.OPTIMIZE, PCASkiingOptimizer.sampling.SAMPLE);
                drop.consume(data);
                avgCostK[i] = drop.finalK();
                avgCostRuntime[i] = drop.totalTime();

                d = new CSVTools.DataLabel(drop.getFinalTransform(), labels, data.getRowDimension(), drop.finalK());
                knnClassifier = new KNN(d, distanceMeasure, propnTrain);
                knnClassifier.runKNN(1);
                avgCostFullRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            }

            double[][] outputRuntimes = new double[][]{avgBaseRuntime, avgSampleRuntime, avgReuseRuntime, avgCostRuntime};
            double[][] outputFullRuntimes = new double[][]{avgBaseFullRuntime, avgSampleFullRuntime, avgReuseFullRuntime, avgCostFullRuntime};
            double[][] outputK = new double[][]{avgBaseK, avgSampleK, avgReuseK, avgCostK};
            double2dListToCSV(outputRuntimes, runtimeOutFile(dataset, lbr, qThresh, algo, date));
            double2dListToCSV(outputFullRuntimes, fullRuntimeOutFile(dataset, lbr, qThresh, algo, date));
            double2dListToCSV(outputK, kOutFile(dataset, lbr, qThresh, algo, date));
        }
    }
}
