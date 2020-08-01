import com.univocity.parsers.annotations.Convert;
import com.univocity.parsers.annotations.Format;
import com.univocity.parsers.annotations.Parsed;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class Transaction implements org.jkarma.model.Transaction<Feature> {

    private static int lastTid = 0;

    public static int getNextTid() {
        int value = Transaction.lastTid;
        Transaction.lastTid++;
        return value;
    }

    private int tid;

    @Format(formats = {"yyyy-MM-dd HH:mm:ss"})
    @Parsed(index = 0)
    private Date timestamp;

    @Parsed(index = 1)
    @Convert(conversionClass = WordToFeatureConverter.class)
    private Set<Feature> features;

    public Transaction() {
        this.tid = Transaction.getNextTid();
    }

    @Override
    public Instant getTimestamp() {
        return this.timestamp.toInstant();
    }

    @Override
    public Collection<Feature> getItems() {
        return this.features;
    }

    @Override
    public int getId() {
        return this.tid;
    }

    @Override
    public Iterator<Feature> iterator() {
        return this.features.iterator();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "tid=" + tid +
                ", timestamp=" + timestamp +
                ", features=" + features.toString() +
                '}';
    }
}
