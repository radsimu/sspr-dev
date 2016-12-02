import ggs.engine.core.GGSException;
import uaic.postagger.tagger.HybridPOStagger;
import uaic.postagger.tagger.MorphologicDictionary;

import java.io.*;
import java.util.*;

/**
 * Created by Planetaria on 12/2/2016.
 */

//we want to build the semantic attraction model on a bigger corpus. We will use sintactically parsed, simple sentences from corola. But for this we will need to parse sentences tagged with UAIC pos tagger.
    //the parser need to be trained on data that is tagged with uaic pos tagger. We retag the tb for this purpose
public class RetagWithUaic {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        String line;
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1])));
        MorphologicDictionary dic = new MorphologicDictionary();
        dic.load(new FileInputStream("uaicPosTaggerResources/posDictRoDiacr.txt"));
        HybridPOStagger tagger = new HybridPOStagger(new FileInputStream("uaicPosTaggerResources/posRoDiacr.model"), dic, new FileInputStream("uaicPosTaggerResources/guesserTagset.txt"), new FileInputStream("uaicPosTaggerResources/posreduction.ggf"));

        List<String[]> sentLines = new ArrayList<>();
        int k =0;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                if (sentLines.size() > 0) {
                    List<String> words = new ArrayList<>();
                    for (String[] cols : sentLines){
                        words.add(cols[1]);
                    }
                    List<String> tags = tagger.tag(words);
                    for (int i = 0; i < words.size(); i++) {
                        String pos = tags.get(i);
                        if (pos.length() > 5)
                            pos = pos.substring(0,5);
                        sentLines.get(i)[3] = pos;
                        for (String col : sentLines.get(i)) {
                            out.write(col);
                            out.write("\t");
                        }
                        out.write("\n");
                    }
                    System.out.println("Finished sent " + k);
                    k++;
                }
                out.write(line);
                out.write("\n");
                sentLines.clear();
            }else
                sentLines.add(columns);
        }
        out.close();
    }
}
