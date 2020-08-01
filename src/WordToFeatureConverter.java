import com.univocity.parsers.conversions.Conversion;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class WordToFeatureConverter implements Conversion<String, Set<Feature>> {

    @Override
    public Set<Feature> execute(String s) {
        if (s == null) {
            return Collections.emptySet();
        }

        Set<Feature> out = new LinkedHashSet<>();
        for (String token : s.split(" ")) {
            Feature f = new Feature(token.trim());
            out.add(f);
        }

        return out;
    }

    @Override
    public String revert(Set<Feature> f) {
        return null;
    }
}
