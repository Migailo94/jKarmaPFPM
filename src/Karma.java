import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.TidSetProvider;
import org.jkarma.mining.providers.TidSetProvider;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;

public class Karma {

    @Option(required=true, name="-blockSize", usage="block size")
    public int blockSize = 10;

    @Option(required=true, name="-maxAvgPer", usage="maximun avarage periodicity")
    public float maxAvgPer = 20f;

    @Option(required=true, name="-minChange", usage="minimum change threshold")
    public float minChange = 0.5f;

    @Option(required=true, name="-i", usage="input filename")
    public File inputFile = null;
    // public File inputFle = new File("dataset/synthetic/dataset 1.csv");

    public PBCD<Transaction, Feature, TidSet, Boolean> getPBCD() throws IOException {
        Stream<Transaction> dataset = this.getDataset();
        long size = dataset.count();

        float minSupp = (size/maxAvgPer) - 1;
        float minRelSupp = minSupp/size;

        WindowingStrategy<TidSet> wmodel = Windows.cumulativeSliding(); // Mixed
        TidSetProvider<Feature> accessor = new TidSetProvider<Feature>(wmodel);

        MiningStrategy<Feature,TidSet> strategy = Strategies.uponItemsets(new HashSet<Feature>()).limitDepth(3)
                .eclat(minRelSupp).dfs(accessor);

        return Detectors.upon(strategy)
                .unweighted((p,s) -> Patterns.isFrequent(p, minRelSupp, s), new UnweightedJaccard())
                .describe(new org.jkarma.pbcd.descriptors.PartialEPDescriptor<>(minRelSupp, 2))
                .build(minChange, blockSize);
    }

    public Stream<Transaction> getDataset() throws IOException {
        return Utils.parseStream(this.inputFile, Transaction.class);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running jKarma for ECLAT...");

        final long startTime = System.currentTimeMillis();

        File changes = new File("ECLAT changes.csv");
        File pattern = new File("ECLAT pattern.csv");

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

        Karma app = new Karma();

        final CmdLineParser argsParser = new CmdLineParser(app);

        try { argsParser.parseArgument(args);

        Stream<Transaction> dataset = app.getDataset();

        PBCD<Transaction, Feature, TidSet, Boolean> detector = app.getPBCD();

        detector.registerListener(new PBCDEventListener<Feature, TidSet>() {
            private int changeDetected = 1, changeNotDetected = 0, numChangePoints = 0, totalNumOfPattern = 0;

            private String patters = "";

            @Override
            public void patternUpdateStarted(PatternUpdateStartedEvent<Feature, TidSet> patternUpdateStartedEvent) {

            }

            @Override
            public void patternUpdateCompleted(PatternUpdateCompletedEvent<Feature, TidSet> patternUpdateCompletedEvent) {

            }

            @Override
            public void changeDescriptionStarted(ChangeDescriptionStartedEvent<Feature, TidSet> changeDescriptionStartedEvent) {

            }

            @Override
            public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<Feature, TidSet> changeDescriptionCompletedEvent) {

            }

            @Override
            public void changeDetected(ChangeDetectedEvent<Feature, TidSet> changeDetectedEvent) {
                System.out.println("Change detected " + changeDetectedEvent.getAmount());

                System.out.println("\nNumber of change points " + ++numChangePoints);

                Instant W1_FIRST = changeDetectedEvent.getLandmarkWindow().getFirstInstant();
                Instant W1_LAST = changeDetectedEvent.getLandmarkWindow().getLastInstant();
                Instant B_FIRST = changeDetectedEvent.getLatestBlock().getFirstInstant();
                Instant B_LAST = changeDetectedEvent.getLatestBlock().getLastInstant();

                String tempolarWindow = "[" + W1_FIRST + " " + W1_LAST + "] [" + B_FIRST + " " + B_LAST + "]";

                String content = tempolarWindow + ";" + changeDetectedEvent.getAmount() + ";" + changeDetected + "\n";

                try {
                    FileWriter writer = new FileWriter("ECLAT changes.csv", true);
                    writer.write(content);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                changeDetectedEvent.getDescription().forEach(p -> {
                    double freqReference = p.getFirstEval().getRelativeFrequency()*100;
                    double freqTarget = p.getSecondEval().getRelativeFrequency()*100;

                    String message;
                    if(freqTarget > freqReference) {
                        message = "increased frequency from ";
                    }else {
                        message = "decreased frequency from ";
                    }
                    message += Double.toString(freqReference)+ "% to " + Double.toString(freqTarget)+"%";
                    System.out.println("\t\t" + p.getItemSet() + " " + message);

                    totalNumOfPattern += p.getItemSet().getLength();

                    System.out.println("\nTotal number of pattern discovered " + totalNumOfPattern);

                    System.out.println("Average number of pattern discovered per change point " + totalNumOfPattern/numChangePoints);

                    System.out.println();

                    patters += p.getItemSet() + ";";
                });
                patters += "\n";

                try {
                    FileWriter writer = new FileWriter("ECLAT pattern.csv", true);
                    writer.write(patters);
                    patters = "";
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void changeNotDetected(ChangeNotDetectedEvent<Feature, TidSet> changeNotDetectedEvent) {
                System.out.println("Change not detected " + changeNotDetectedEvent.getAmount());

                Instant W1_FIRST = changeNotDetectedEvent.getLandmarkWindow().getFirstInstant();
                Instant W1_LAST = changeNotDetectedEvent.getLandmarkWindow().getLastInstant();
                Instant B_FIRST = changeNotDetectedEvent.getLatestBlock().getFirstInstant();
                Instant B_LAST = changeNotDetectedEvent.getLatestBlock().getLastInstant();

                String tempolarWindow = "[" + W1_FIRST + " " + W1_LAST + "] [" + B_FIRST + " " + B_LAST + "]";

                String content = tempolarWindow + ";" + changeNotDetectedEvent.getAmount() + ";" + changeNotDetected + "\n";

                try {
                    FileWriter writer = new FileWriter("ECLAT changes.csv", true);
                    writer.write(content);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        dataset.forEach(detector);

        System.out.println("Running time " + (System.currentTimeMillis() - startTime) + " ms");

        ArrayList labels = new ArrayList();
        ArrayList groudtruth = new ArrayList();

        BufferedReader br = new BufferedReader(new FileReader("ECLAT changes.csv"));
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

        for(int i = 0; i < labels.size(); i++){
            if(labels.get(i).equals(0) && groudtruth.get(i).equals(0)){
                TN++;
            }else{
                if(labels.get(i).equals(0) && groudtruth.get(i).equals(1)){
                    FN++;
                }else{
                    if(labels.get(i).equals(1) && groudtruth.get(i).equals(0)){
                        FP++;
                    }else{
                        if(labels.get(i).equals(1) && groudtruth.get(i).equals(1)){
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

        System.out.printf("\nAccuracy %.3f", (double)(TP + TN)/(TN + TP + FN + FP));
        System.out.printf("\nPrecision %.3f", (double)TP/(TP + FP));
        System.out.printf("\nRecall %.3f", (double)TP/(TP + FN));
        System.out.printf("\nF1 %.3f", (double)(2*TP)/(2*TP + FP + FN));

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java SampleMain [options...] arguments...");
            argsParser.printUsage(System.err);
            System.err.println();
            System.err.println(" Example: java SampleMain " + argsParser.printExample(OptionHandlerFilter.ALL));
        }
    }
}
