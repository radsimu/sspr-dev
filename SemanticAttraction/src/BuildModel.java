import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Created by Planetaria on 11/20/2016.
 */


public class BuildModel {

    public static class SynsetContext {

        public SynsetContext(String context){
            identifier = context;
        }

        public String identifier;
        public Set<String> occuringLemmas = new HashSet<>();
        public Set<RoWordNet.Synset> occuringSynsets = new HashSet<>();
        public Map<RoWordNet.Synset, Float> hypernimCoverage = new HashMap<>(); //how many synsets have occurred under each synset
        public Map<RoWordNet.Synset, Float> hypernimGeneralisationFactors = new HashMap<>(); //
        public Set<RoWordNet.Synset> relevantCommonHypernyms = new HashSet<>();
        public float hypernimGeneralisationFactor = 0;
        public Set<RoWordNet.Synset> rootSynsets = new HashSet<>();
        public float hypernymCoverageRootsCount = 0;

        public float hypernimsMaxOverlapIndex;
        public float hypernimsAvgOverlapIndex;

        public void computeMetrics() {
            //coverageOverlapRootsCount

            for (RoWordNet.Synset synset : hypernimCoverage.keySet()) {
                Set<RoWordNet.Synset> h = synset.relations.get(RoWordNet.WNRelation.hypernym);
                if (h == null)
                    rootSynsets.add(synset);
            }
            hypernymCoverageRootsCount = rootSynsets.size();


            //hypernimsMaxOverlapIndex
            //hypernimsAvgOverlapIndex
            hypernimsMaxOverlapIndex = 0;
            hypernimsAvgOverlapIndex = 0;
            for (float value : hypernimCoverage.values()) {
                if (hypernimsMaxOverlapIndex < value)
                    hypernimsMaxOverlapIndex = value;
                hypernimsAvgOverlapIndex += value;
            }
            hypernimsAvgOverlapIndex = hypernimsAvgOverlapIndex / hypernimCoverage.size();

            //relevantCommonHypernyms
            Map<String, Float> domainsFrequencies = new HashMap<>();
            for (RoWordNet.Synset synset : occuringSynsets){
                Float f = domainsFrequencies.get(synset.domain);
                if (f==null)
                    f=0f;
                f++;
                domainsFrequencies.put(synset.domain, f);
            }

            for (Map.Entry<String, Float> entry : domainsFrequencies.entrySet())
                domainsFrequencies.put(entry.getKey(), entry.getValue()/domainsFrequencies.size());

            for (RoWordNet.Synset synset : occuringSynsets){
                RoWordNet.Synset hypernym = synset;

                if (hypernym.domain == null)
                    continue;
                boolean reachedRoot = false;
                while (true){
                    Set<RoWordNet.Synset> h = hypernym.relations.get(RoWordNet.WNRelation.hypernym);
                    if (h == null) {
                        reachedRoot = true;
                        break;
                    }
                    RoWordNet.Synset hy = h.iterator().next();
                    if (!hypernym.domain.equals(hy.domain))
                        break;
                    hypernym = hy;
                }
                if (reachedRoot)
                    continue;
                if (hypernym == synset)
                    continue;
                relevantCommonHypernyms.add(hypernym);
            }
            if (!relevantCommonHypernyms.isEmpty())
                hypernimGeneralisationFactor = (float)occuringLemmas.size() / relevantCommonHypernyms.size();
        }

        @Override
        public String toString(){
            return  identifier;
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

        Map<String, SynsetContext> contextsToHypernymCoverage = new TreeMap<>();
        Set<String> addedContextWithLemma = new HashSet<>();

        //compute coverage of the overlap index of the hypernims of all synsets that occur in identical contexts
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                if (sentWords.size() > 0) {
                    for (int i = 0; i < sentWords.size(); i++) {
                        //build a context - in this case the lemma and pos of the head of the word + pos of word
                        //TODO: remove the constraint below for more generic context analysis
                        if (sentHeads.get(i) == 0 || !sentPos.get(sentHeads.get(i) - 1).startsWith("Vm") || !sentPos.get(i).startsWith("Nc"))//se ne uitam un pic doar la cazul verb->substantiv regent
                            continue;
                        String context = null;

                        if (sentHeads.get(i) != 0) {
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
                            context = sentLemmas.get(sentHeads.get(i) - 1) + "(" + sentPos.get(sentHeads.get(i) - 1).substring(0, 2) + ") -> [" + preps + " -> " + sentPos.get(i).substring(0, 2) + "]";
                        }
                        //else
                        //    context = "ROOT";


                        //ensure that each context and lemma synset list is added only once. We don't count for the frequency of a lemma being correlated with a particular context. We just need 1 proof that a lemma is correlated with a context
                        {
                            assert context != null;
                            String k = context + "_$$_" + sentLemmas.get(i);
                            if (addedContextWithLemma.contains(k))
                                continue;
                            addedContextWithLemma.add(k);
                        }

                        Set<RoWordNet.Synset> synsets = rown.getSynsetsForLiterral(sentLemmas.get(i));
                        if (synsets == null)
                            continue;
                        ;
                        Iterator<RoWordNet.Synset> iter = synsets.iterator();
                        while (iter.hasNext()) {
                            RoWordNet.Synset synset = iter.next();
                            if (Character.toLowerCase(synset.pos.charAt(0)) != Character.toLowerCase(sentPos.get(i).charAt(0)))
                                iter.remove();
                        }
                        if (synsets.isEmpty())
                            continue;

                        SynsetContext synsetContext = contextsToHypernymCoverage.get(context);
                        if (synsetContext == null) {
                            synsetContext = new SynsetContext(context);
                            contextsToHypernymCoverage.put(context, synsetContext);
                        }
                        synsetContext.occuringLemmas.add(sentLemmas.get(i));
                        synsetContext.occuringSynsets.addAll(synsets);

                        Set<RoWordNet.Synset> synsetsHypernimTree = new HashSet<>();
                        //mark all the synsets in the hypernym ascendence chain for the synsets of this lemma occurence
                        for (RoWordNet.Synset synset : synsets) {
                            RoWordNet.Synset hypernim = synset;
                            while (!synsetsHypernimTree.contains(hypernim)) {
                                synsetsHypernimTree.add(hypernim);
                                Set<RoWordNet.Synset> h = hypernim.relations.get(RoWordNet.WNRelation.hypernym);
                                if (h == null)
                                    break;//no more hypernyms
                                hypernim = h.iterator().next();
                            }
                        }

                        //add hypernym tree to the coverage of this context
                        for (RoWordNet.Synset synset : synsetsHypernimTree) {
                            Float f = synsetContext.hypernimCoverage.get(synset);
                            if (f == null)
                                f = 0f;
                            f += 1;
                            synsetContext.hypernimCoverage.put(synset, f);
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
            sentLemmas.add(columns[2]);
            sentPos.add(columns[4]);
            sentHeads.add(Integer.parseInt(columns[6]));
        }

        //extract contexts for which a high overlap index has been observed
        List<Map.Entry<Float, SynsetContext>> contextsByHypernimsMaxOverlapIndex = new ArrayList<>();
        List<Map.Entry<Float, SynsetContext>> contextsByHypernimsAvgOverlapIndex = new ArrayList<>();
        List<Map.Entry<Float, SynsetContext>> contextsByHypernymCoverageRootsCount = new ArrayList<>();
        List<Map.Entry<Float, SynsetContext>> contextsByHypernimGeneralisationFactor = new ArrayList<>();
        for (SynsetContext synsetContext : contextsToHypernymCoverage.values()) {
            synsetContext.computeMetrics();
            contextsByHypernymCoverageRootsCount.add(new AbstractMap.SimpleEntry<>(synsetContext.hypernymCoverageRootsCount, synsetContext));
            contextsByHypernimsMaxOverlapIndex.add(new AbstractMap.SimpleEntry<>(synsetContext.hypernimsMaxOverlapIndex, synsetContext));
            contextsByHypernimsAvgOverlapIndex.add(new AbstractMap.SimpleEntry<>(synsetContext.hypernimsAvgOverlapIndex, synsetContext));
            contextsByHypernimGeneralisationFactor.add(new AbstractMap.SimpleEntry<>(synsetContext.hypernimGeneralisationFactor, synsetContext));
        }


        Comparator<Map.Entry<Float, SynsetContext>> comparator = (o1, o2) -> Float.compare(o2.getKey(), o1.getKey());
        contextsByHypernymCoverageRootsCount.sort(comparator);
        contextsByHypernimsMaxOverlapIndex.sort(comparator);
        contextsByHypernimsAvgOverlapIndex.sort(comparator);
        contextsByHypernimGeneralisationFactor.sort(comparator);

        reader.close();
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
