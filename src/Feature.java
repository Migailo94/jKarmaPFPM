public class Feature implements Comparable<Feature> {

    private String value;

    public Feature(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(Feature o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
