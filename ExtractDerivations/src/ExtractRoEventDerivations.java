import uaic.postagger.tagger.Annotation;
import uaic.postagger.tagger.MorphologicDictionary;
import uaic.segmenter.WordStruct;

import java.io.*;
import java.util.*;

/**
 * Created by radu.simionescu on 02/11/16.
 */
public class ExtractRoEventDerivations {
    static MorphologicDictionary dictionary;

    public static void main(String[] args) throws Exception {
        BufferedReader wnAligned = new BufferedReader(new FileReader("partial_ro_derivations/ro_derivations_pwn_aligned.txt"));
        String line;
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ro_derivations_compiled.txt")));
        Map<String, String> outSet = new TreeMap<String, String>();

        //these have been automatically correlated from alignment with the english wordnet derivations and should all be correct (ack Elenea)
        while ((line = wnAligned.readLine()) != null) {
            String[] split = line.split("\t");
            if (split[2].toUpperCase().equals("EVENT")) {
                String s = String.format("%s\tNc\t%s\tVmn\t%s", WordStruct.getCorrectedDiacritics(split[1]), WordStruct.getCorrectedDiacritics(split[0]), split[2].toUpperCase());
                outSet.put(split[1], s);
            }
        }

        //these are some automatic correlations based on ro wordnet entries and a derivation model. But do not contain a semantic label.
        //the bellow tries to detect the EVENT type derivations from verbs to nouns, based on the formulation of the word definition
        wnAligned.close();
        BufferedReader autoDerivations = new BufferedReader(new FileReader("partial_ro_derivations/ro_derivations_auto.txt"));
        String outLine;
        String w1, w2, pos1, pos2, g1, g2;
        w1 = w2 = pos1 = pos2 = g1 = g2 = null;
        int k = 0;
        while ((line = autoDerivations.readLine()) != null) {
            k++;
            if (line.trim().isEmpty() || Character.isUpperCase(line.charAt(0))) {
                if (w1 != null && !w1.equals("skip_entry")) {
                    if (w2 == null)
                        throw new Exception("bad format at line " + k + ": " + line);
                    String label = null;

                    if (pos1.equals("Vmn") && pos2.equals("Nc")) {
                        //swap
                        String aux = pos1;
                        pos1 = pos2;
                        pos2 = aux;
                        aux = w1;
                        w1 = w2;
                        w2 = aux;
                        aux = g1;
                        g1 = g2;
                        g2 = aux;
                    }

                    outLine = String.format("%s\t%s\t%s\t%s\t%s", w1, pos1, w2, pos2, label);
                    if (pos1.equals("Nc") && pos2.equals("Vmn")) {
                        if (g1.startsWith("faptul de a") || g1.startsWith("acţiunea de a") || g1.startsWith("a "))
                            label = "EVENT";
                        else {
                            String firstWord = g1.split(" ")[0];
                            if (firstWord.equals("o") || firstWord.equals("un"))
                                firstWord = g1.split(" ")[1];
                            if (outSet.containsKey(firstWord)) {
                                label = "EVENT";
                                //System.out.println(outLine + " " + g1);
                            }
                        }
                    }

                    outLine = String.format("%s\t%s\t%s\t%s\t%s", w1, pos1, w2, pos2, label);
                    if (label != null) {
                        outSet.put(w1, outLine);
                        //System.out.println(outLine);
                    }
                }
                outLine = w1 = w2 = pos1 = pos2 = g1 = g2 = null;
                continue;
            }
            String[] split = line.split("\t");

            try {
                if (w1 == null) {
                    w1 = WordStruct.getCorrectedDiacritics(split[0]);
                    pos1 = convertPos(split[1].split("-")[2]);
                    g1 = WordStruct.getCorrectedDiacritics(split[2].toLowerCase());
                } else {
                    if (w2 != null)
                        throw new Exception("bad format at line " + k + ": " + line);
                    w2 = WordStruct.getCorrectedDiacritics(split[0]);
                    pos2 = convertPos(split[1].split("-")[2]);
                    g2 = WordStruct.getCorrectedDiacritics(split[2].toLowerCase());
                }
            } catch (Exception ex) {
                //System.out.println("weird format at line " + k + ": " + line);
                w1 = "skip_entry";
                continue;
            }
        }
        autoDerivations.close();


        //bellow we extract more derived nouns that might represent EVENT/action from uaic's pos dictionary, based on some rigid derivation rules
        dictionary = new MorphologicDictionary();
        dictionary.diacriticsPolicy = MorphologicDictionary.StrippedDiacriticsPolicy.NeverStripped;
        dictionary.load(new FileInputStream("posDictRoDiacr.txt"));
        for (Map.Entry<String, List<Annotation>> entry : dictionary.entrySet()) {
            for (Annotation a : entry.getValue()) {
                if (outSet.containsKey(a.getLemma()))
                    continue;
                if ((a.msd.startsWith("Nc") || a.msd.startsWith("Af")) && WordStruct.getCanonicalWord(a.word).equals(WordStruct.getCanonicalWord(a.lemma))) {
                    String lemma = a.getLemma();
                    String verb = getEventDerivationVerbForNoun(lemma);
                    if (verb != null)
                        outSet.put(lemma, String.format("%s\t%s\t%s\tVmn\tEVENT", WordStruct.getCorrectedDiacritics(a.lemma), a.msd.substring(0,2), WordStruct.getCorrectedDiacritics(verb)));
                }
            }
        }

        for (String val : outSet.values()) {
            out.write(val);
            out.write("\n");
        }

        out.close();
    }

    private static String getEventDerivationVerbForNoun(String lemma) {
        String possibleVerb;
        String baseVerb;
        if (lemma.endsWith("ție")) {
            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ție = Vmn demonstratie
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("ție")) {
            possibleVerb = lemma.substring(0, lemma.length() - 1) + "ona"; //-ție +ona = Vmn  tranzactie ambitie
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("zie")) {
            possibleVerb = lemma.substring(0, lemma.length() - 3) + "da";//-zie +da = Vmn explozie
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("zie")) {
            possibleVerb = lemma.substring(0, lemma.length() - 3) + "de";//-zie +de = Vmn decizie
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("ie")) {
            possibleVerb = lemma.substring(0, lemma.length() - 1); //-ie + i = Vmn calarie
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
//        if (lemma.endsWith("ătură")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //-ătură + a = Vmn aruncatura sapatura  înjurătură
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("ură")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ură = Vmp fosnitura, scursura, taratura, lovitură
//            baseVerb = isVerb(possibleVerb, "Vmp");
//            if (baseVerb != null) return baseVerb;
//        }
        if (lemma.endsWith("re")) {
            possibleVerb = lemma.substring(0, lemma.length() - 2); //cunoastere
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("iune")) {
            possibleVerb = lemma.substring(0, lemma.length() - 3) + "ona"; //-iune +ona=Vmn actiune, operatiune
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("eală")) {
            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-eală + i = Vmn pacaleala, vrajeala, socoteala, greseala
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("aj")) {
            possibleVerb = lemma.substring(0, lemma.length() - 1); //-aj = Vmn pilotaj bricolaj
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
//        if (lemma.endsWith("oare")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 4); //-oare = Vmp tunsoare
//            baseVerb = isVerb(possibleVerb, "Vmp");
//            if (baseVerb != null) return baseVerb;
//        }
        if (lemma.endsWith("ință")) {
            possibleVerb = lemma.substring(0, lemma.length() - 3); //-ință +i = Vmn biruință
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("ință")) {
            possibleVerb = lemma.substring(0, lemma.length() - 4) + "e"; //-ință + e = Vmn credință
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("ență")) {
            possibleVerb = lemma.substring(0, lemma.length() - 4) + "a"; //-ență + a = Vmn insistență, audiență
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
//        if (lemma.endsWith("ământ")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 5) + "a"; //-ământ +a = Vmn apărământ, legământ, secământ
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("ătură")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 5); //-ătură = Vmp întorsătură
//            baseVerb = isVerb(possibleVerb, "Vmp");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("anie")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-anie +i = Vmn pățanie împărtășanie
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("anie")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 3); //-anie +a = Vmn furișanie
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("enie")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 4) + "i"; //-enie +i = Vmn slobozenie, tâmpenie, cumințenie, curățenie
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
        if (lemma.endsWith("ment")) {
            possibleVerb = lemma.substring(0, lemma.length() - 4); //-ment = Vmn antrenament, angajament
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
//        if (lemma.endsWith("ăciune")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 6) + "a"; //-ăciune +a = Vmn stricăciune
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("ăciune")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 6) + "â"; //-ăciune +â = Vmn amărăciune
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (lemma.endsWith("iciune")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 5); //-iciune +i =Vmn pricăjiciune
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
        if (lemma.endsWith("ător")) {
            possibleVerb = lemma.substring(0, lemma.length() - 4) + "a"; //-ător +a = Vmn strigător
            baseVerb = isVerb(possibleVerb, "Vmn");
            if (baseVerb != null) return baseVerb;
        }
        if (lemma.endsWith("or")) {
            possibleVerb = lemma.substring(0, lemma.length() - 2); //-or = Vmp doritor
            baseVerb = isVerb(possibleVerb, "Vmp");
            if (baseVerb != null) return baseVerb;
        }
//        if (lemma.endsWith("ă")) {
//            possibleVerb = lemma.substring(0, lemma.length() - 1) + "a"; //-ă +a = Vmn influență
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }
//        if (baseVerb == null) {
//            possibleVerb = lemma + "a"; //+a = Vmn schimb, accident, șantaj, partaj - regula asta nu e buna pt ca genereaza multe perechi eronate (ex parlament, caz etc.)
//            baseVerb = isVerb(possibleVerb, "Vmn");
//            if (baseVerb != null) return baseVerb;
//        }

        possibleVerb = lemma;
        baseVerb = isVerb(possibleVerb, "Vmp");
        if (baseVerb != null) return baseVerb;

        return null;
    }

    static String convertPos(String p) {
        if (p.equals("n"))
            return "Nc";
        if (p.equals("v"))
            return "Vmn";
        if (p.equals("a"))
            return "Afp";
        if (p.equals("r"))
            return "Rg";
        return "X";
    }

    static String isVerb(String word, String ifHasPos) {
        List<Annotation> annotations = dictionary.get(word);
        if (annotations != null)
            for (Annotation a : annotations) {
                if (a.msd.startsWith(ifHasPos)) {
                    return a.getLemma();
                }
            }
        return null;
    }
}
