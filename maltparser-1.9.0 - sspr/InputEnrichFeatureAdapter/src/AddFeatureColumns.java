import org.maltparser.Malt;

import java.io.*;
import java.util.List;

import uaic.postagger.tagger.Annotation;
import uaic.postagger.tagger.MorphologicDictionary;

import maltProcessing.EvaluateMaltModel;

/**
 * Created by Planetaria on 10/25/2016.
 */
public class AddFeatureColumns {
    static MorphologicDictionary dictionary;

    public  static void main(String[] args) throws IOException {
        addFeatures(new File(args[0]), new File(args[1]));
    }

    public static void addFeatures(File conllIn, File conllOut) throws IOException {
        if (dictionary == null){
            dictionary = new MorphologicDictionary();
            dictionary.load(new FileInputStream("posDictRoDiacr.txt"));
        }

        BufferedReader reader = new BufferedReader(new FileReader(conllIn));
        String line;
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
            if (extraFeats.equals("_")) extraFeats+="|";


            //add verbal feats
            {
                String verbalFeats = null;

                if (pos.startsWith("Vm")) {
                    List<Annotation> annotations = dictionary.get(lemma);
                    if (annotations != null)
                        for (Annotation a : annotations) {
                            if (a.msd.equals("Vmn")) {
                                verbalFeats = featsForVerb(a);
                                if (verbalFeats != null)
                                    break;
                            }
                        }
                } else if (pos.startsWith("Nc")) {
                    if (!word.endsWith("ele") && !word.endsWith("elor")) {
                        verbalFeats = verbalFeatsForWord(lemma, "Vmp"); //substantivizare din supin
                    }

                    if (verbalFeats == null) {
                        String possibleVerb = null;
                        if (lemma.endsWith("ție")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3); //demonstratie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                            if (verbalFeats == null)
                                possibleVerb = lemma.substring(0, lemma.length() - 1) + "ona"; //tranzactie ambitie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("zie")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "da";//explozie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                            if (verbalFeats == null)
                                possibleVerb = lemma.substring(0, lemma.length() - 3) + "de";//decizie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("ie")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 1); //calarie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("ătură")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //aruncatura sapatura
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("ură")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3); //fosnitura, scursura, taratura,
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
                        } else if (lemma.endsWith("re")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 2); //cunoastere
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("iune")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "ona"; //actiune, operatiune
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("eală")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //pacaleala, vrajeala, socoteala, greseala
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("aj")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 1); //pilotaj bricolaj
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        } else if (lemma.endsWith("oare")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4); //tunsoare
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
                        }
                    }
                }

                if (verbalFeats == null) {
                    verbalFeats = "is_action=false|transitive=NA";
                }

                extraFeats += verbalFeats;
            }

            //add other feats to extraFeats HERE

            columns[5]=extraFeats;

            out.write(String.join("\t", columns));
            out.write("\n");
        }
        out.close();
    }

    static String verbalFeatsForWord(String word, String ifHasPos) {
        String extraFeats = null;
        List<Annotation> annotations = dictionary.get(word);
        if (annotations != null)
            for (Annotation a : annotations) {
                if (a.msd.equals(ifHasPos)) {
                    extraFeats = featsForVerb(a);
                    if (extraFeats != null)
                        break;
                }
            }
        return extraFeats;
    }

    static String featsForVerb(Annotation a) {
        String extraFeats = null;
        if (a.extra != null) {
            if (a.extra.contains("Stripped"))
                return null;
            extraFeats = "is_action=true";
            if (a.extra.contains("intranzitiv")) {
                extraFeats += "|transitive=false";
            } else if (a.extra.contains("tranzitiv")) {
                extraFeats += "|transitive=true";
            }
        }

        return extraFeats;
    }
}
