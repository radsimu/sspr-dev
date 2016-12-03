import java.io.*;
import java.util.*;

import uaic.postagger.tagger.Annotation;
import uaic.postagger.tagger.HybridPOStagger;
import uaic.postagger.tagger.MorphologicDictionary;
import uaic.segmenter.WordStruct;

/**
 * Created by Planetaria on 10/25/2016.
 */
public class AddFeatureColumns {
    static HybridPOStagger _tagger;
    static MorphologicDictionary _dic;

    static MorphologicDictionary getDic() throws Exception {
        if (_dic == null){
            _dic = new MorphologicDictionary();
            _dic.load(new FileInputStream("uaicPosTaggerResources/posDictRoDiacr.txt"));
        }
        return _dic;
    }

    static HybridPOStagger getTagger() throws Exception {
        if (_tagger == null)
            _tagger = new HybridPOStagger(new FileInputStream("uaicPosTaggerResources/posRoDiacr.model"), getDic(), new FileInputStream("uaicPosTaggerResources/guesserTagset.txt"), new FileInputStream("uaicPosTaggerResources/posreduction.ggf"));
        return _tagger;
    }

    public static void main(String[] args) throws Exception {
        addFeatures(new File(args[0]), new File(args[1]));
    }

    public static void addFeatures(File conllIn, File conllOut) throws Exception {


        BufferedReader reader = new BufferedReader(new FileReader("ro_derivations_compiled.txt"));
        Map<String, String> eventNouns = new HashMap<String, String>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split("\t");
            eventNouns.put(WordStruct.getCanonicalWord(split[0]), WordStruct.getCanonicalWord(split[2]));
        }
        reader.close();


        reader = new BufferedReader(new FileReader(conllIn));
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllOut)));

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                out.write(line);
                out.write("\n");
                continue;
            }
            String word = columns[1];
            String lemma = columns[2];
            String pos = columns[4];
            String extraFeats = columns[5].trim();
            if (extraFeats.equals("_"))
                extraFeats = "";
            else
                extraFeats += "|";


            //add verbal feats extracted for event nouns derived from verbs
            {
                String verbalFeats = null;

                if (pos.startsWith("Vm")) {
                    verbalFeats = verbalFeatsForVerb(lemma, "Vmn");
                } else if (pos.startsWith("Nc")){
                    String possibleVerb = eventNouns.get(WordStruct.getCanonicalWord(lemma));
                    if (possibleVerb != null) {
                        verbalFeats = verbalFeatsForVerb(possibleVerb, "Vmp"); //substantivizare indicata de derivarile extrase/inferate wordnet si uaic pos tagger
                    }
                } else if (pos.startsWith("Af")) {
                    if (!word.endsWith("ele") && !word.endsWith("elor")) {
                        verbalFeats = verbalFeatsForVerb(lemma, "Vmp"); //participiu
                    }
                }

                if (verbalFeats == null) {
                    verbalFeats = "is_verbal=false|transitive=NA";
                }

                extraFeats += verbalFeats;
            }

            //TODO: add other feats to extraFeats HERE

            columns[5] = extraFeats;

            out.write(String.join("\t", columns));
            out.write("\n");
        }
        reader.close();
        out.close();
    }

    private static String verbalFeatsForVerb(String lemma, String pos) throws Exception {
        Set<Annotation> annotations = getDic().get(lemma);
        if (annotations == null)
            return null;
        for (Annotation annotation : annotations) {
            if (annotation.getMsd().startsWith(pos)) {
                if (annotation.getExtra() == null)
                    continue;
                String transitive = "NA";
                if (annotation.getExtra().contains("intranzitiv"))
                    transitive = "false";
                else if (annotation.getExtra().contains("tranzitiv"))
                    transitive = "true";
                return String.format("is_verbal=true|transitive=" + transitive);
            }
        }
        return null;
    }
}
