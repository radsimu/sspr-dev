package com.sspr.semantics;

import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Created by Planetaria on 11/20/2016.
 */


public class SemanticAttractionModel {
    public RoWordNet roWordNet;
    public ContextBuilder contextBuilder;
    public SynsetContext starContext = new SynsetContext("*");
    public Map<String, SynsetContext> contexts = new TreeMap<>();
    public List<Map.Entry<Float, SynsetContext>> contextsByMaxAttractionScore = new ArrayList<>();
    public List<Map.Entry<Float, SynsetContext>> contextsByMaxGeneralizationIndex = new ArrayList<>();
    public Map<String, Map<String, Float>> synsetsToAttractiveContexts = new HashMap<>(); // each synset(as string) is attracted to a set of different contexts, with different scores for each

    public SemanticAttractionModel (RoWordNet roWordNet, InputStream conllCorpus, ContextBuilder contextBuilder) throws ParserConfigurationException, SAXException, IOException {
        this.roWordNet = roWordNet;
        this.contextBuilder = contextBuilder;
        build(conllCorpus);
    }

    public static abstract class ContextBuilder{
        public abstract String buildContext(int tokenToBuildContextFor, List<String> sentLemmas, List<String> sentPos, List<Integer> sentHeads);
    }

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

        public Map<String, Float> qualityGeneralizationIndexes = new HashMap<>(); //how much does a quality represent a generalisation of the occuring synsets
        public List<Map.Entry<String, Float>> qualityGeneralizationIndexOrdered = new ArrayList<>();
        public float maxQualityGeneralizationIndex;


        public void computeMetrics(SynsetContext starContext) {
            maxQualityAttractionScore = 0;
            if (occurringLemmas.size() < 10)
                return;
            for (Map.Entry<String, Integer> entry : qualitiesOccurrenceCount.entrySet()) {
                qualitiesOccurrenceProbabilities.put(entry.getKey(), (float) entry.getValue() / occurringLemmas.size());
            }

            if (starContext != null) {
                //tf-idf attraction score
                for (Map.Entry<String, Integer> entry : qualitiesOccurrenceCount.entrySet()) {
                    float qualityAttractionScore = (float) Math.log(1 + qualitiesOccurrenceProbabilities.get(entry.getKey())) * (float)Math.log(1/starContext.qualitiesOccurrenceProbabilities.get(entry.getKey()));
                    if (maxQualityAttractionScore < qualityAttractionScore)
                        maxQualityAttractionScore = qualityAttractionScore;

                    qualityAttractionScores.put(entry.getKey(), qualityAttractionScore);
                    qualityAttractionScoresOrdered.add(new AbstractMap.SimpleEntry<>(entry.getKey(), qualityAttractionScore));
                }
                qualityAttractionScoresOrdered.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

                //generalization index
                maxQualityGeneralizationIndex = 0;
                HashMap<RoWordNet.Synset, RoWordNet.Synset> maxAttractionHypernimPerSynset = buildMapWithHypernmWithMaxAttractionScoreForEachSynset();
                for (RoWordNet.Synset synset : occurringSynsets) {
                    if (!qualityAttractionScores.containsKey(synset.toString()))
                        continue;
                    float qualityGeneralizationIndex = qualityAttractionScores.get(maxAttractionHypernimPerSynset.get(synset).toString()) - qualityAttractionScores.get(synset.toString());

                    if (maxQualityGeneralizationIndex < qualityGeneralizationIndex)
                        maxQualityGeneralizationIndex = qualityGeneralizationIndex;

                    qualityGeneralizationIndexes.put(synset.toString() + " ---> " + maxAttractionHypernimPerSynset.get(synset), qualityGeneralizationIndex);
                    qualityGeneralizationIndexOrdered.add(new AbstractMap.SimpleEntry<>(synset.toString() + " ---> " + maxAttractionHypernimPerSynset.get(synset), qualityGeneralizationIndex));
                }
                qualityGeneralizationIndexOrdered.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            }
        }

        private HashMap<RoWordNet.Synset, RoWordNet.Synset> buildMapWithHypernmWithMaxAttractionScoreForEachSynset() {
            HashMap<RoWordNet.Synset, RoWordNet.Synset> rez = new HashMap<>();
            //mark all the synsets in the hypernym ascendence chain for the synsets of this lemma occurence
            for (RoWordNet.Synset synset : occurringSynsets) {
                buildMaxAttractionHypernimForSynsetRecursive(synset, rez);
            }
            return rez;
        }

        private RoWordNet.Synset buildMaxAttractionHypernimForSynsetRecursive(RoWordNet.Synset synset, HashMap<RoWordNet.Synset, RoWordNet.Synset> partialMapWithHypernimsMaxAttractionScores) {
            if (qualityAttractionScores.get(synset.toString()) == null)
                return null;
            if (partialMapWithHypernimsMaxAttractionScores.containsKey(synset))
                return partialMapWithHypernimsMaxAttractionScores.get(synset);

            Set<RoWordNet.Synset> hypernims = synset.relations.get(RoWordNet.WNRelation.hypernym);
            Float max = qualityAttractionScores.get(synset.toString());
            RoWordNet.Synset maxHypernim = synset;

            partialMapWithHypernimsMaxAttractionScores.put(synset, null);//this avoids infinite loop... it seems that hypernims can create loops (maybe an wn error?)
            if (hypernims != null) {
                for (RoWordNet.Synset hypernim : hypernims){
                    RoWordNet.Synset s = buildMaxAttractionHypernimForSynsetRecursive(hypernim, partialMapWithHypernimsMaxAttractionScores);
                    if (s == null)
                        continue;
                    if (qualityAttractionScores.get(s.toString()) > max) {
                        max = qualityAttractionScores.get(s.toString());
                        maxHypernim = s;
                    }
                }
            }

            partialMapWithHypernimsMaxAttractionScores.put(synset, maxHypernim);
            return maxHypernim;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(identifier);
            if (qualityAttractionScoresOrdered.size() > 0) {
                sb.append("  ::  ");
                sb.append("(bestAttr=").append(qualityAttractionScoresOrdered.get(0).getValue()).append(" ").append(qualityAttractionScoresOrdered.get(0).getKey()).append(")");
            }

            if (qualityGeneralizationIndexOrdered.size() > 0) {
                sb.append("  ::  ");
                sb.append("(bestGen=").append(qualityGeneralizationIndexOrdered.get(0).getValue()).append(" ").append(qualityGeneralizationIndexOrdered.get(0).getKey()).append(")");
            }

            return sb.toString();
        }
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        //for demo purposes:
        RoWordNet rown = new RoWordNet();
        rown.load(args[0]);
        SemanticAttractionModel semanticAttractionModel = new SemanticAttractionModel (rown, new FileInputStream(args[1]), new ContextBuilder() {
            @Override
            public String buildContext(int tokenToBuildContextFor, List<String> sentLemmas, List<String> sentPos, List<Integer> sentHeads) {
                //TODO: remove the constraint below for more generic context analysis
                //if (!sentPos.get(i).startsWith("Nc"))
                //continue;
//                        if (sentHeads.get(i) == 0 || !sentPos.get(sentHeads.get(i) - 1).startsWith("Vm") || !sentPos.get(i).startsWith("Nc"))//se ne uitam un pic doar la cazul verb->substantiv regent
//                            continue;

                //compute the prepositions attached to word i
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < sentLemmas.size(); j++) {
                    if (sentHeads.get(j) - 1 == tokenToBuildContextFor && (sentPos.get(j).startsWith("S") || sentPos.get(j).startsWith("Q") || sentPos.get(j).startsWith("R"))) {//word j has i as head
                        sb.append(sentLemmas.get(j)).append("_");
                    }
                }
                String preps;
                if (sb.length() == 0)
                    preps = "_";
                else
                    preps = sb.toString().substring(0, sb.length() - 1);
                String context = ((sentHeads.get(tokenToBuildContextFor) != 0) ? sentLemmas.get(sentHeads.get(tokenToBuildContextFor) - 1) : "ROOT") + " -> [" + preps + " -> " + sentPos.get(tokenToBuildContextFor).substring(0, 1) + "]";
                //else
                //    context = "ROOT";
                return context;
            }
        });
        semanticAttractionModel = semanticAttractionModel;
    }

    private void build(InputStream conllCorpus) throws IOException, SAXException, ParserConfigurationException {
        //count lemma by synset

        BufferedReader reader = new BufferedReader(new InputStreamReader(conllCorpus));
        String line;
        List<Integer> sentHeads = new ArrayList<>();
        List<String> sentWords = new ArrayList<>();
        List<String> sentLemmas = new ArrayList<>();
        List<String> sentPos = new ArrayList<>();

        //compute coverage of the overlap index of the hypernims of all synsets that occur in identical contexts
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                if (sentWords.size() > 0) {
                    for (int i = 0; i < sentWords.size(); i++) {
                        String context = contextBuilder.buildContext(i, sentLemmas, sentPos, sentHeads);

                        //ensure that each  lemma is considered only once for each context. We don't count for the frequency of a lemma being correlated with a particular context. We just need 1 proof that a lemma is correlated with a context
                        SynsetContext synsetContext = contexts.get(context);
                        if (synsetContext == null) {
                            synsetContext = new SynsetContext(context);
                        }

                        if (synsetContext.occurringLemmas.contains(sentLemmas.get(i)))
                            continue;
                        synsetContext.occurringLemmas.add(sentLemmas.get(i));

                        Set<RoWordNet.Synset> synsets = roWordNet.getSynsetsForLiterral(sentLemmas.get(i).split(" \\(")[0]);
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
                        contexts.put(context, synsetContext);

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

        for (SynsetContext synsetContext : contexts.values()) {
            synsetContext.computeMetrics(starContext);
            contextsByMaxAttractionScore.add(new AbstractMap.SimpleEntry<>(synsetContext.maxQualityAttractionScore, synsetContext));
            contextsByMaxGeneralizationIndex.add(new AbstractMap.SimpleEntry<>(synsetContext.maxQualityGeneralizationIndex, synsetContext));
        }

        Comparator<Map.Entry<Float, SynsetContext>> comparator = (o1, o2) -> Float.compare(o2.getKey(), o1.getKey());
        contextsByMaxAttractionScore.sort(comparator);
        contextsByMaxGeneralizationIndex.sort(comparator);


        for (Map.Entry<Float, SynsetContext> floatSynsetContextEntry : contextsByMaxAttractionScore) {
            if (floatSynsetContextEntry.getKey() == 0)
                continue;
            for (Map.Entry<String, Float> entry : floatSynsetContextEntry.getValue().qualityAttractionScores.entrySet()){
                if (entry.getValue() == 0)
                    continue;
                Map<String, Float> attractedToContexts = synsetsToAttractiveContexts.get(entry.getKey());
                if (attractedToContexts == null){
                    attractedToContexts = new HashMap<>();
                    synsetsToAttractiveContexts.put(entry.getKey(), attractedToContexts);
                }
                attractedToContexts.put(floatSynsetContextEntry.getValue().identifier, entry.getValue());
            }
        }
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

    public float computeLemmaAttraction(String lemma, String contex){
        float max = 0;
        for (RoWordNet.Synset synset : roWordNet.getSynsetsForLiterral(lemma)){
            Map<String, Float> attractiveContexts = synsetsToAttractiveContexts.get(synset.toString());
            Float attraction = attractiveContexts.get(contex);
            if (attraction != null && attraction > max)
                max = attraction;
        }
        return max;
    }
}
