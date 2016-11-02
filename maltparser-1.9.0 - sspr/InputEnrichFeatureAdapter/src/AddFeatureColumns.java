import org.maltparser.Malt;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    public static Map<String, String> verbalFeatsCache = new TreeMap<String, String>();

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
            if (extraFeats.equals("_"))
                extraFeats="";
            else
                extraFeats+="|";


            //add verbal feats extracted for event nouns derived from verbs
            //derivation rules commented bellow have proved to obtain no improvement for malt training
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
                } else if (pos.startsWith("Nc") || pos.startsWith("Afp")) {
                    if (!word.endsWith("ele") && !word.endsWith("elor")) {
                        verbalFeats = verbalFeatsForWord(lemma, "Vmp"); //substantivizare din supin
                    }

                    if (verbalFeats == null) {
                        String possibleVerb = null;
                        if (lemma.endsWith("ție")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ție = Vmn demonstratie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("ție")){
                                possibleVerb = lemma.substring(0, lemma.length() - 1) + "ona"; //-ție +ona = Vmn  tranzactie ambitie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("zie")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "da";//-zie +da = Vmn explozie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("zie")){
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "de";//-zie +de = Vmn decizie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("ie")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 1); //-ie + i = Vmn calarie
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
//                        if (verbalFeats == null && lemma.endsWith("ătură")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //-ătură + a = Vmn aruncatura sapatura  înjurătură
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("ură")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ură = Vmp fosnitura, scursura, taratura, lovitură
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
//                        }
                        if (verbalFeats == null && lemma.endsWith("re")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 2); //cunoastere
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("iune")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3) + "ona"; //-iune +ona=Vmn actiune, operatiune
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("eală")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-eală + i = Vmn pacaleala, vrajeala, socoteala, greseala
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("aj")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 1); //-aj = Vmn pilotaj bricolaj
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
//                        if (verbalFeats == null && lemma.endsWith("oare")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 4); //-oare = Vmp tunsoare
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
//                        }

                        if (verbalFeats == null && lemma.endsWith("ință")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ință +i = Vmn biruință
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("ință")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "e"; //-ință + e = Vmn credință
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("ență")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "a"; //-ență + a = Vmn insistență, audiență
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
//                        if (verbalFeats == null && lemma.endsWith("ământ")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //-ământ +a = Vmn apărământ, legământ, secământ
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("ătură")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 5); //-ătură = Vmp întorsătură
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("anie")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-anie +i = Vmn pățanie împărtășanie
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("anie")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 3); //-anie +a = Vmn furișanie
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("enie")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-enie +i = Vmn slobozenie, tâmpenie, cumințenie, curățenie
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
                        if (verbalFeats == null && lemma.endsWith("ment")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4); //-ment = Vmn antrenament, angajament
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
//                        if (verbalFeats == null && lemma.endsWith("ăciune")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 6) + "a"; //-ăciune +a = Vmn stricăciune
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("ăciune")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 6) + "â"; //-ăciune +â = Vmn amărăciune
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
//                        if (verbalFeats == null && lemma.endsWith("iciune")) {
//                            possibleVerb = lemma.substring(0, lemma.length() - 5); //-iciune +i =Vmn pricăjiciune
//                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                        }
                        if (verbalFeats == null && lemma.endsWith("ător")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 4) + "a"; //-ător +a = Vmn strigător
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
                        }
                        if (verbalFeats == null && lemma.endsWith("or")) {
                            possibleVerb = lemma.substring(0, lemma.length() - 2); //-or = Vmp doritor
                            verbalFeats = verbalFeatsForWord(possibleVerb, "Vmp");
                        }

//                        if (verbalFeats == null && pos.startsWith("Nc")) {
//                            if (verbalFeats == null && lemma.endsWith("ă")) {
//                                possibleVerb = lemma.substring(0, lemma.length() - 1) + "a"; //-ă +a = Vmn influență
//                                verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                            }
//                            if (verbalFeats == null) {
//                                possibleVerb = lemma + "a"; //+a = Vmn schimb, accident, șantaj, partaj
//                                verbalFeats = verbalFeatsForWord(possibleVerb, "Vmn");
//                            }
//                            if (verbalFeats != null)
//                                System.out.println(lemma);
//                        }
                    }

                    if (verbalFeats != null){
                        verbalFeatsCache.put(lemma, verbalFeats);
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
                if (a.msd.startsWith(ifHasPos)) {
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
