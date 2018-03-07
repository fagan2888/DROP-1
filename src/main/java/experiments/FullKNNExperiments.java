package experiments;

import optimizer.PCASkiingOptimizer;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import runner.FFTRunner;
import runner.PAARunner;
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
public class FullKNNExperiments extends Experiment {
    public static String baseString = "src/main/java/experiments/e2eKNNExperiments/";
    public static DateFormat day = new SimpleDateFormat("MM-dd");
    public static DateFormat minute = new SimpleDateFormat("HH_mm");


    private static String allPAAOutFile(String dataset, double lbr, double qThresh, Date date){
        String output = String.format("%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/PAA/%s.csv", output);
    }

    private static String allFFTOutFile(String dataset, double lbr, double qThresh, Date date){
        String output = String.format("%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/FFT/%s.csv", output);
    }

    private static String allDROPOutFile(String dataset, double lbr, double qThresh, Date date){
        String output = String.format("%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/DROP/%s.csv", output);
    }

    private static String allSVDOutFile(String dataset, double lbr, double qThresh, Date date){
        String output = String.format("%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/SVD/%s.csv", output);
    }

    private static String noDROutFile(String dataset, Date date){
        String output = String.format("%s_%s",minute.format(date),dataset);
        return String.format(baseString + day.format(date) + "/NoDR/%s.csv", output);
    }

    private static String countOut(String dataset, double lbr, double qThresh, Date date){
        String output = String.format("%s_%s_lbr%.2f_q%.2f",minute.format(date),dataset, lbr, qThresh);
        return String.format(baseString + day.format(date) + "/dataPropn/%s.csv", output);
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
        PCASkiingOptimizer.PCAAlgo algo = PCASkiingOptimizer.PCAAlgo.TROPP;//, PCASkiingOptimizer.PCAAlgo.FAST};
        PCASkiingOptimizer.optimize opt = PCASkiingOptimizer.optimize.OPTIMIZE;
        PCASkiingOptimizer.work reuse = PCASkiingOptimizer.work.REUSE;

        Metric<double[]> distanceMeasure = new EuclideanDistance();
        double propnTrain = 0.5;

        //PlaceHolders
        CSVTools.DataLabel dd;
        RealMatrix data;
        String labels[];
        double[] avgDROPRuntime = new double[numTrials];
        double[] avgDROPK = new double[numTrials];
        double[] avgDROPAccuracy = new double[numTrials];
        double[] avgDROPKNNRuntime = new double[numTrials];
        double[] avgPAARuntime = new double[numTrials];
        double[] avgPAAK = new double[numTrials];
        double[] avgPAAAccuracy = new double[numTrials];
        double[] avgPAAKNNRuntime = new double[numTrials];
        double[] avgFFTRuntime = new double[numTrials];
        double[] avgFFTK = new double[numTrials];
        double[] avgFFTAccuracy = new double[numTrials];
        double[] avgFFTKNNRuntime = new double[numTrials];
        double[] avgSVDRuntime = new double[numTrials];
        double[] avgSVDK = new double[numTrials];
        double[] avgSVDAccuracy = new double[numTrials];
        double[] avgSVDKNNRuntime = new double[numTrials];
        double[] avgRuntime = new double[numTrials];
        double[] avgKNNRuntime = new double[numTrials];
        double[] avgK = new double[numTrials];
        double[] avgAccuracy = new double[numTrials];
        int[] DROPDataLookedAt = new int[numTrials];

        KNN knnClassifier;

        //load data and get labels as list
        CSVTools.DataLabel d = getLabeledData(dataset);
        data = new Array2DRowRealMatrix(d.matrix);
        labels = d.origLabels;

        for (int i = 0; i < numTrials; i++) {
            //no DR
            knnClassifier = new KNN(d, distanceMeasure, propnTrain);
            knnClassifier.runKNN(1);
            avgKNNRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            avgAccuracy[i] = knnClassifier.knnAccuracy();
            avgK[i] = d.N;
            avgRuntime[i] = 0;


            PCARunner drop = new PCARunner(qThresh, lbr, algo, reuse, opt);
            PCARunner svd = new PCARunner(qThresh, lbr, PCASkiingOptimizer.PCAAlgo.SVD, PCASkiingOptimizer.work.NOREUSE, PCASkiingOptimizer.optimize.NOOPTIMIZE, PCASkiingOptimizer.sampling.NOSAMPLE);
            PAARunner paa = new PAARunner(qThresh, lbr);
            FFTRunner fft = new FFTRunner(qThresh, lbr);

            svd.consume(data);
            drop.consume(data);
            paa.consume(data);
            fft.consume(data);

            //update SVD stuff
            avgSVDK[i] = svd.finalK();
            avgSVDRuntime[i] = svd.totalTime();

            dd = new CSVTools.DataLabel(svd.getFinalTransform(), labels, data.getRowDimension(), svd.finalK());
            knnClassifier = new KNN(dd, distanceMeasure, propnTrain);
            knnClassifier.runKNN(1);
            avgSVDKNNRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            avgSVDAccuracy[i] = knnClassifier.knnAccuracy();


            //update DROP stuff
            avgDROPK[i] = drop.finalK();
            avgDROPRuntime[i] = drop.totalTime();
            DROPDataLookedAt[i] = drop.getNt();

            dd = new CSVTools.DataLabel(drop.getFinalTransform(), labels, data.getRowDimension(), drop.finalK());
            knnClassifier = new KNN(dd, distanceMeasure, propnTrain);
            knnClassifier.runKNN(1);
            avgDROPKNNRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            avgDROPAccuracy[i] = knnClassifier.knnAccuracy();


            //update PAA stuff
            avgPAAK[i] = paa.finalK();
            avgPAARuntime[i] = paa.totalTime();

            dd = new CSVTools.DataLabel(paa.getFinalTransform(), labels, data.getRowDimension(), paa.finalK());
            knnClassifier = new KNN(dd, distanceMeasure, propnTrain);
            knnClassifier.runKNN(1);
            avgPAAKNNRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            avgPAAAccuracy[i] = knnClassifier.knnAccuracy();


            //update FFT stuff
            avgFFTK[i] = fft.finalK();
            avgFFTRuntime[i] = fft.totalTime();

            dd = new CSVTools.DataLabel(fft.getFinalTransform(), labels, data.getRowDimension(), fft.finalK());
            knnClassifier = new KNN(dd, distanceMeasure, propnTrain);
            knnClassifier.runKNN(1);
            avgFFTKNNRuntime[i] = knnClassifier.getKNNTime() + knnClassifier.getTrainTime();
            avgFFTAccuracy[i] = knnClassifier.knnAccuracy();

        }
        double[][] outputValues = new double[][]{avgRuntime, avgK, avgAccuracy, avgKNNRuntime};
        double[][] outputFFTValues = new double[][]{avgFFTRuntime, avgFFTK, avgFFTAccuracy, avgFFTKNNRuntime};
        double[][] outputPAAValues = new double[][]{avgPAARuntime, avgPAAK, avgPAAAccuracy, avgPAAKNNRuntime};
        double[][] outputDROPValues = new double[][]{avgDROPRuntime, avgDROPK, avgDROPAccuracy, avgDROPKNNRuntime};
        double[][] outputSVDValues = new double[][]{avgSVDRuntime, avgSVDK, avgSVDAccuracy, avgSVDKNNRuntime};
        double2dListToCSV(outputValues, noDROutFile(dataset, date));
        double2dListToCSV(outputSVDValues, allSVDOutFile(dataset, lbr, qThresh, date));
        double2dListToCSV(outputPAAValues, allPAAOutFile(dataset, lbr, qThresh, date));
        double2dListToCSV(outputFFTValues, allFFTOutFile(dataset, lbr, qThresh, date));
        double2dListToCSV(outputDROPValues, allDROPOutFile(dataset, lbr, qThresh, date));
        intListToCSV(DROPDataLookedAt,countOut(dataset,lbr,qThresh,date));
    }
}
