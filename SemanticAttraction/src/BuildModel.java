import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Created by Planetaria on 11/20/2016.
 */


public class BuildModel {

    public static class SynsetContext {

        public SynsetContext(String context) {
            identifier = context;
        }

        public String identifier;
        public Set<String> occurringLemmas = new HashSet<>();
        public Set<RoWordNet.Synset> occurringSynsets = new HashSet<>();
        public Map<String, Integer> qualitiesOccurrenceCount = new HashMap<>(); //the number of synsets that have each quality
        public Map<String, Float> qualitiesOccurrenceProbabilities = new HashMap<>(); //the probability of a synset to have each quality
        public Map<String, Float> qualityAttractionScores = new HashMap<>();
        public List<Map.Entry<String, Float>> qualityAttractionScoresOrdered = new ArrayList<>();

        public float maxQualityAttractionScore;

        public void computeMetrics(SynsetContext starContext) {
            maxQualityAttractionScore = 0;
            if (occurringLemmas.size() < 10)
                return;
            for (Map.Entry<String, Integer> entry : qualitiesOccurrenceCount.entrySet()) {
                qualitiesOccurrenceProbabilities.put(entry.getKey(), (float) entry.getValue() / occurringSynsets.size());

                if (starContext != null) {
                    float qualityAttractionScore = (float) (1 + Math.log(entry.getValue())) * (float)Math.log(1 + 1/starContext.qualitiesOccurrenceProbabilities.get(entry.getKey()));
                    if (entry.getValue() < 5){
                        continue;
                    }
                    if (maxQualityAttractionScore < qualityAttractionScore)
                        maxQualityAttractionScore = qualityAttractionScore;

                    qualityAttractionScores.put(entry.getKey(), qualityAttractionScore);
                    qualityAttractionScoresOrdered.add(new AbstractMap.SimpleEntry<>(entry.getKey(), qualityAttractionScore));
                }
            }

            qualityAttractionScoresOrdered.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        }

        @Override
        public String toString() {
            return identifier;
        }
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        RoWordNet rown = new RoWordNet();
        rown.load(args[0]);

        //count lemma by synset

        BufferedReader reader = new BufferedReader(new FileReader(args[1]));
        String line;
        List<Integer> sentHeads = new ArrayList<>();
        List<String> sentWords = new ArrayList<>();
        List<String> sentLemmas = new ArrayList<>();
        List<String> sentPos = new ArrayList<>();

        SynsetContext starContext = new SynsetContext("*");
        Map<String, SynsetContext> contexts = new TreeMap<>();


        //compute coverage of the overlap index of the hypernims of all synsets that occur in identical contexts
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                if (sentWords.size() > 0) {
                    for (int i = 0; i < sentWords.size(); i++) {
                        //build a context - in this case the lemma and pos of the head of the word + pos of word
                        //TODO: remove the constraint below for more generic context analysis
//                        if (sentHeads.get(i) == 0 || !sentPos.get(sentHeads.get(i) - 1).startsWith("Vm") || !sentPos.get(i).startsWith("Nc"))//se ne uitam un pic doar la cazul verb->substantiv regent
//                            continue;
                        String context = null;


                        //compute the prepositions attached to word i
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < sentWords.size(); j++) {
                            if (sentHeads.get(j) - 1 == i && sentPos.get(j).startsWith("S")) {//word j has i as head
                                sb.append(sentLemmas.get(j)).append("_");
                            }
                        }
                        String preps;
                        if (sb.length() == 0)
                            preps = "_";
                        else
                            preps = sb.toString().substring(0, sb.length() - 1);
                        context = ((sentHeads.get(i) != 0) ? sentLemmas.get(sentHeads.get(i) - 1) : "ROOT") + " -> [" + preps + " -> " + sentPos.get(i).substring(0, 1) + "]";
                        //else
                        //    context = "ROOT";


                        //ensure that each  lemma is considered only once for each context. We don't count for the frequency of a lemma being correlated with a particular context. We just need 1 proof that a lemma is correlated with a context
                        SynsetContext synsetContext = contexts.get(context);
                        if (synsetContext == null) {
                            synsetContext = new SynsetContext(context);
                            contexts.put(context, synsetContext);
                        }

                        if (synsetContext.occurringLemmas.contains(sentLemmas.get(i)))
                            continue;
                        synsetContext.occurringLemmas.add(sentLemmas.get(i));

                        Set<RoWordNet.Synset> synsets = rown.getSynsetsForLiterral(sentLemmas.get(i).split(" \\(")[0]);
                        if (synsets == null)
                            continue;

                        //remove synsets associated with the found lemma, that have different pos from the one of the occuring lemma
                        Iterator<RoWordNet.Synset> iter = synsets.iterator();
                        while (iter.hasNext()) {
                            RoWordNet.Synset synset = iter.next();
                            if (Character.toLowerCase(synset.pos.charAt(0)) != Character.toLowerCase(sentPos.get(i).charAt(0)))
                                iter.remove();
                        }
                        if (synsets.isEmpty())
                            continue;

                        synsetContext.occurringSynsets.addAll(synsets);

                        Set<RoWordNet.Synset> synsetsHypernims = getHypernims(synsets);

                        //add the hypernym tree as qualities of the occuring synsets
                        for (RoWordNet.Synset synset : synsetsHypernims) {
                            Integer f = synsetContext.qualitiesOccurrenceCount.get(synset.toString());
                            if (f == null)
                                f = 0;
                            f += 1;
                            synsetContext.qualitiesOccurrenceCount.put(synset.toString(), f);
                        }

                        if (!starContext.occurringLemmas.contains(sentLemmas.get(i))) {
                            starContext.occurringLemmas.add(sentLemmas.get(i));
                            starContext.occurringSynsets.addAll(synsetsHypernims);
                            for (RoWordNet.Synset synset : synsetsHypernims) {
                                Integer f = starContext.qualitiesOccurrenceCount.get(synset.toString());
                                if (f == null)
                                    f = 0;
                                f += 1;
                                starContext.qualitiesOccurrenceCount.put(synset.toString(), f);
                            }
                        }
                    }
                }

                sentWords = new ArrayList<>();
                sentLemmas = new ArrayList<>();
                sentPos = new ArrayList<>();
                sentHeads = new ArrayList<>();
                continue;
            }
            sentWords.add(columns[1]);
            sentLemmas.add(columns[2] + " (" + columns[4].substring(0, 1) + ")"); //also adding the first letter of the pos, to separate identical lemmas having different pos type
            sentPos.add(columns[4]);
            sentHeads.add(Integer.parseInt(columns[6]));
        }

        starContext.computeMetrics(null);

        //extract contexts for which a high overlap index has been observed
        List<Map.Entry<Float, SynsetContext>> contextsByMaxAttractionScore = new ArrayList<>();

        for (SynsetContext synsetContext : contexts.values()) {
            synsetContext.computeMetrics(starContext);
            contextsByMaxAttractionScore.add(new AbstractMap.SimpleEntry<>((float) synsetContext.maxQualityAttractionScore, synsetContext));
        }


        Comparator<Map.Entry<Float, SynsetContext>> comparator = (o1, o2) -> Float.compare(o2.getKey(), o1.getKey());
        contextsByMaxAttractionScore.sort(comparator);


        reader.close();
    }

    private static Set<RoWordNet.Synset> getHypernims(Set<RoWordNet.Synset> synsets) {
        Set<RoWordNet.Synset> rez = new HashSet<>();
        //mark all the synsets in the hypernym ascendence chain for the synsets of this lemma occurence
        for (RoWordNet.Synset synset : synsets) {
            getHypernimsRecursive(synset, rez);
        }
        return rez;
    }

    private static void getHypernimsRecursive(RoWordNet.Synset synset, Set<RoWordNet.Synset> rez) {
        if (rez.contains(synset))
            return;
        rez.add(synset);

        Set<RoWordNet.Synset> hypernims = synset.relations.get(RoWordNet.WNRelation.hypernym);
        if (hypernims == null)
            return;//no more hypernyms
        for (RoWordNet.Synset hypernim : hypernims){
            getHypernimsRecursive(hypernim, rez);
        }
    }


    private static void ComputeSynsetsToDepths(Set<RoWordNet.Synset> synsets, Map<RoWordNet.Synset, Integer> synsetsToDepths) {
        for (RoWordNet.Synset synset : synsets)
            ComputeSynsetDepth(synset, synsetsToDepths);
    }

    private static int ComputeSynsetDepth(RoWordNet.Synset synset, Map<RoWordNet.Synset, Integer> synsetsToDepths) {
        if (synsetsToDepths.containsKey(synset))
            return synsetsToDepths.get(synset);
        Set<RoWordNet.Synset> h = synset.relations.get(RoWordNet.WNRelation.hypernym);
        if (h == null) {
            synsetsToDepths.put(synset, 0);
            return 0;
        }
        return ComputeSynsetDepth(h.iterator().next(), synsetsToDepths) + 1;
    }
}
