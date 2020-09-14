import com.sun.tools.javac.Main;
import org.jkarma.mining.joiners.PeriodSet;
import org.jkarma.mining.providers.PeriodSetProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.WindowingStrategy;
import org.jkarma.mining.windows.Windows;
import org.jkarma.pbcd.detectors.Detectors;
import org.jkarma.pbcd.detectors.PBCD;
import org.jkarma.pbcd.events.*;
import org.jkarma.pbcd.patterns.Patterns;
import org.jkarma.pbcd.similarities.UnweightedJaccard;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;

public class PFPM {

    @Option(required=true, name="-blockSize", usage="block size")
    public int blockSize = 10;

    @Option(required=true, name="-minPer", usage="minimun periodicity")
    public int minPer = 2;

    @Option(required=true, name="-maxPer", usage="maximun periodicity")
    public int maxPer = 20;

    @Option(required=true, name="-minAvgPer", usage="minimun avarage periodicity")
    public float minAvgPer = 2f;

    @Option(required=true, name="-maxAvgPer", usage="maximun avarage periodicity")
    public float maxAvgPer = 20f;

    @Option(required=true, name="-minChange", usage="minimum change threshold")
    public float minChange = 0.5f;

    @Option(required=true, name="-i", usage="input filename")
    // public File inputFle = new File("dataset/synthetic/dataset 1.csv");
    public File inputFile = null;

    public PBCD<Transaction, Feature, PeriodSet, Boolean> getPBCD(){

        WindowingStrategy<PeriodSet> wmodel = Windows.cumulativeSliding(); // Mixed
        PeriodSetProvider<Feature> accessor = new PeriodSetProvider<Feature>(wmodel);

        MiningStrategy<Feature,PeriodSet> strategy = Strategies.uponItemsets(new HashSet<Feature>()).limitDepth(3)
                .pfpm(minPer, maxPer, minAvgPer, maxAvgPer).dfs(accessor);

        return Detectors.upon(strategy)
                .unweighted((p,s) -> Patterns.isPeriodic(p, minPer, maxPer, minAvgPer, maxAvgPer, s), new UnweightedJaccard())
                .describe(new PartialPeriodicEPEvaluator<>(minPer, maxPer, minAvgPer, maxAvgPer, 2))
                .build(minChange, blockSize);
    }

    public Stream<Transaction> getDataset() throws IOException {
        return Utils.parseStream(this.inputFile, Transaction.class);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running jKarma for PFPM...");

        final long startTime = System.currentTimeMillis();

        File changes = new File("PFPM changes.csv");
        File pattern = new File("PFPM pattern.csv");

        if(changes.exists()){
            changes.delete();
        }else{
            new FileNotFoundException();
        }

        if(pattern.exists()){
            pattern.delete();
        }else{
            new FileNotFoundException();
        }

        PFPM app = new PFPM();

        final CmdLineParser argsParser = new CmdLineParser(app);

        try {
            argsParser.parseArgument(args);

            Stream<Transaction> dataset = app.getDataset();

            PBCD<Transaction, Feature, PeriodSet, Boolean> detector = app.getPBCD();

            detector.registerListener(new PBCDEventListener<Feature, PeriodSet>() {
                private int changeDetected = 1, changeNotDetected = 0, numChangePoints = 0, totalNumOfPattern = 0;

                private String patters = "";

                @Override
                public void patternUpdateStarted(PatternUpdateStartedEvent<Feature, PeriodSet> patternUpdateStartedEvent) {

                }

                @Override
                public void patternUpdateCompleted(PatternUpdateCompletedEvent<Feature, PeriodSet> patternUpdateCompletedEvent) {

                }

                @Override
                public void changeDescriptionStarted(ChangeDescriptionStartedEvent<Feature, PeriodSet> changeDescriptionStartedEvent) {

                }

                @Override
                public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<Feature, PeriodSet> changeDescriptionCompletedEvent) {

                }

                @Override
                public void changeDetected(ChangeDetectedEvent<Feature, PeriodSet> changeDetectedEvent) {
                    System.out.println("Change detected " + changeDetectedEvent.getAmount());

                    System.out.println("\nNumber of change points " + ++numChangePoints);

                    Instant W1_FIRST = changeDetectedEvent.getLandmarkWindow().getFirstInstant();
                    Instant W1_LAST = changeDetectedEvent.getLandmarkWindow().getLastInstant();
                    Instant B_FIRST = changeDetectedEvent.getLatestBlock().getFirstInstant();
                    Instant B_LAST = changeDetectedEvent.getLatestBlock().getLastInstant();

                    String tempolarWindow = "[" + W1_FIRST + " " + W1_LAST + "] [" + B_FIRST + " " + B_LAST + "]";

                    String content = tempolarWindow + ";" + changeDetectedEvent.getAmount() + ";" + changeDetected + "\n";

                    try {
                        FileWriter writer = new FileWriter("PFPM changes.csv", true);
                        writer.write(content);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    changeDetectedEvent.getDescription().forEach(p -> {
                        double freqReference = p.getFirstEval().getRelativeFrequency() * 100;
                        double freqTarget = p.getSecondEval().getRelativeFrequency() * 100;

                        String message;
                        if (freqTarget > freqReference) {
                            message = "increased frequency from ";
                        } else {
                            message = "decreased frequency from ";
                        }
                        message += Double.toString(freqReference) + "% to " + Double.toString(freqTarget) + "%";
                        System.out.println("\t\t" + p.getItemSet() + " " + message);

                        totalNumOfPattern += p.getItemSet().getLength();

                        System.out.println("\nTotal number of pattern discovered " + totalNumOfPattern);

                        System.out.println("Average number of pattern discovered per change point " + totalNumOfPattern / numChangePoints);

                        System.out.println();

                        patters += p.getItemSet() + ";";
                    });
                    patters += "\n";

                    try {
                        FileWriter writer = new FileWriter("PFPM pattern.csv", true);
                        writer.write(patters);
                        patters = "";
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void changeNotDetected(ChangeNotDetectedEvent<Feature, PeriodSet> changeNotDetectedEvent) {
                    System.out.println("Change not detected " + changeNotDetectedEvent.getAmount());

                    Instant W1_FIRST = changeNotDetectedEvent.getLandmarkWindow().getFirstInstant();
                    Instant W1_LAST = changeNotDetectedEvent.getLandmarkWindow().getLastInstant();
                    Instant B_FIRST = changeNotDetectedEvent.getLatestBlock().getFirstInstant();
                    Instant B_LAST = changeNotDetectedEvent.getLatestBlock().getLastInstant();

                    String tempolarWindow = "[" + W1_FIRST + " " + W1_LAST + "] [" + B_FIRST + " " + B_LAST + "]";

                    String content = tempolarWindow + ";" + changeNotDetectedEvent.getAmount() + ";" + changeNotDetected + "\n";

                    try {
                        FileWriter writer = new FileWriter("PFPM changes.csv", true);
                        writer.write(content);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            dataset.forEach(detector);

            System.out.println("Running time " + (System.currentTimeMillis() - startTime) + " ms");

            if (app.inputFile.getName().equals("dataset 1.csv") ||
                    app.inputFile.getName().equals("dataset 2.csv") ||
                    app.inputFile.getName().equals("dataset 3.csv") ||
                    app.inputFile.getName().equals("dataset 4.csv") ||
                    app.inputFile.getName().equals("dataset 5.csv")) {
                ArrayList labels = new ArrayList();
                ArrayList groudtruth = new ArrayList();

                BufferedReader br = new BufferedReader(new FileReader("PFPM changes.csv"));
                String line = "";
                labels.add(0);
                while ((line = br.readLine()) != null) {
                    String[] split = line.split(";");
                    labels.add(Integer.parseInt(split[2]));
                }
                br.close();

                br = new BufferedReader(new FileReader("dataset/synthetic/groudtruth"));
                line = "";
                while ((line = br.readLine()) != null) {
                    groudtruth.add(Integer.parseInt(line));
                }
                br.close();

                System.out.println("\nLabels " + labels.toString());
                System.out.println("Groud truth " + groudtruth.toString());

                int TP = 0, FN = 0, FP = 0, TN = 0;

                for (int i = 0; i < labels.size(); i++) {
                    if (labels.get(i).equals(0) && groudtruth.get(i).equals(0)) {
                        TN++;
                    } else {
                        if (labels.get(i).equals(0) && groudtruth.get(i).equals(1)) {
                            FN++;
                        } else {
                            if (labels.get(i).equals(1) && groudtruth.get(i).equals(0)) {
                                FP++;
                            } else {
                                if (labels.get(i).equals(1) && groudtruth.get(i).equals(1)) {
                                    TP++;
                                }
                            }
                        }
                    }
                }

                System.out.println("\nTN " + TN);
                System.out.println("TP " + TP);
                System.out.println("FN " + FN);
                System.out.println("FP " + FP);

                System.out.printf("\nAccuracy %.3f", (double) (TP + TN) / (TN + TP + FN + FP));
                System.out.printf("\nPrecision %.3f", (double) TP / (TP + FP));
                System.out.printf("\nRecall %.3f", (double) TP / (TP + FN));
                System.out.printf("\nF1 %.3f", (double) (2 * TP) / (2 * TP + FP + FN));
            }

            } catch(CmdLineException e){
                System.err.println(e.getMessage());
                System.err.println("java SampleMain [options...] arguments...");
                argsParser.printUsage(System.err);
                System.err.println();
                System.err.println(" Example: java SampleMain " + argsParser.printExample(OptionHandlerFilter.ALL));
            }

        }
    }
