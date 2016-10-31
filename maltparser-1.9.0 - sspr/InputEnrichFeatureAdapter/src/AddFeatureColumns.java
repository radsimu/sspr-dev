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
            String extraFeats = null;

            if (pos.startsWith("Vm")) {
                List<Annotation> annotations = dictionary.get(lemma);
                if (annotations != null)
                    for (Annotation a : annotations) {
                        if (a.msd.equals("Vmn")) {
                            extraFeats = featsForVerb(a);
                            if (extraFeats != null)
                                break;
                        }
                    }
            } else if (pos.startsWith("Nc")) {
                if (!word.endsWith("ele") && !word.endsWith("elor")) {
                    extraFeats = verbalFeatsForWord(lemma, "Vmp"); //substantivizare din supin
                }

                if (extraFeats == null) {
                    String possibleVerb = null;
                    if (lemma.endsWith("ție")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 3); //demonstratie
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        if (extraFeats == null)
                            possibleVerb = lemma.substring(0, lemma.length() - 1) + "ona"; //tranzactie ambitie
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("zie")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 3) + "da";//explozie
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        if (extraFeats == null)
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "de";//decizie
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("ie")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 1); //calarie
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("ătură")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //aruncatura sapatura
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("ură")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 3); //fosnitura, scursura, taratura,
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmp");
                    } else if (lemma.endsWith("re")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 2); //cunoastere
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("iune")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 3) + "ona"; //actiune, operatiune
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("eală")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //pacaleala, vrajeala, socoteala
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("aj")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 1); //pilotaj bricolaj
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                    } else if (lemma.endsWith("oare")) {
                        possibleVerb = lemma.substring(0, lemma.length() - 4); //tunsoare
                        extraFeats = verbalFeatsForWord(possibleVerb, "Vmp");
                    }
                }
            }

            if (extraFeats != null) {
                columns[5] = extraFeats;
                //System.out.println(word + "  =>  " + extraFeats);
            }

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
            extraFeats = "action";
            if (a.extra.contains("intranzitiv")) {
                extraFeats += "|intranzitiv";
            } else if (a.extra.contains("tranzitiv")) {
                extraFeats += "|tranzitiv";
            }
        }

        return extraFeats;
    }
}
