import java.io.*;
import java.util.*;

import uaic.postagger.tagger.Annotation;
import uaic.postagger.tagger.MorphologicDictionary;
import uaic.segmenter.WordStruct;

/**
 * Created by Planetaria on 10/25/2016.
 */
public class AddFeatureColumns {
    static MorphologicDictionary dictionary;

    public static void main(String[] args) throws IOException {
        addFeatures(new File(args[0]), new File(args[1]));
    }

    public static void addFeatures(File conllIn, File conllOut) throws IOException {
        if (dictionary == null) {
            dictionary = new MorphologicDictionary();
            dictionary.diacriticsPolicy = MorphologicDictionary.StrippedDiacriticsPolicy.NeverStripped;
            dictionary.load(new FileInputStream("posDictRoDiacr.txt"));
        }

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

    private static String verbalFeatsForVerb(String lemma, String pos) {
        List<Annotation> annotations = dictionary.get(lemma);
        if (annotations == null)
            return null;
        for (Annotation annotation : annotations) {
            if (annotation.msd.startsWith(pos)) {
                if (annotation.getExtraFeature() == null)
                    continue;
                String transitive = "NA";
                if (annotation.getExtraFeature().contains("intranzitiv"))
                    transitive = "false";
                else if (annotation.getExtraFeature().contains("tranzitiv"))
                    transitive = "true";
                return String.format("is_verbal=true|transitive=" + transitive);
            }
        }
        return null;
    }
}
