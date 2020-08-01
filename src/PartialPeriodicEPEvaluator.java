import org.jkarma.mining.joiners.FrequencyEvaluation;
import org.jkarma.mining.joiners.PeriodicityEvaluation;
import org.jkarma.pbcd.descriptors.Descriptor;
import org.jkarma.pbcd.patterns.Pattern;
import org.jkarma.pbcd.patterns.Patterns;

import java.util.stream.Stream;

public class PartialPeriodicEPEvaluator<A extends Comparable<A>,B extends PeriodicityEvaluation & FrequencyEvaluation> implements Descriptor<A, B> {

    public int minPer;

    public int maxPer;

    public float minAvgPer;

    public float maxAvgPer;

    public double growRate;

    public PartialPeriodicEPEvaluator(int minPer, int maxPer, float minAvgPer, float maxAvgPer, double growRate) {
        this.minPer = minPer;
        this.maxPer = maxPer;
        this.minAvgPer = minAvgPer;
        this.maxAvgPer = maxAvgPer;
        this.growRate = growRate;
    }

    @Override
    public Stream<Pattern<A, B>> apply(Stream<Pattern<A, B>> patternStream) {
        return patternStream.filter(p -> this.isXORPeriodic(p)).filter(p -> Patterns.isEmerging(p, growRate));
    }

    private boolean isXORPeriodic(Pattern<A, B> pattern){
        boolean isPeriodic = Patterns.isPeriodic(pattern, minPer, maxPer, minAvgPer, maxAvgPer);
        boolean wasPeriodic = Patterns.wasPeriodic(pattern, minPer, maxPer, minAvgPer, maxAvgPer);

        return isPeriodic ^ wasPeriodic;

    }
}
