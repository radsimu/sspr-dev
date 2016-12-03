import uaic.postagger.tagger.HybridPOStagger;
import uaic.postagger.tagger.MorphologicDictionary;

import java.io.*;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by radu.simionescu on 02/12/16.
 */
public class ExtractSimpleSentences {
    public static void main(String[] args) throws IOException {
        //simple sentences have only one main verb and are smaller than 10 words
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        String line;
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
        System.out.println("Extracting simple sentences from " + args[0]);
        List<String[]> sentLines = new ArrayList<>();
        int sentCount = 0;
        int simpleSentCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 5) {
                if (sentLines.size() > 0) {//this is the end of a sentence
                    sentCount++;
                    if (sentLines.size() < 10) {
                        int verbsCount = 0;
                        for (String[] cols : sentLines) {
                            if (cols[3].toLowerCase().matches("^v[^a].*"))
                                verbsCount++;
                        }

                        if (verbsCount == 1) {
                            for (String[] cols : sentLines) {
                                for (String col : cols) {
                                    out.write(col);
                                    out.write("\t");
                                }
                                out.write("\n");
                            }
                            out.write("\n");
                            simpleSentCount++;
                        }
                    }
                }
                sentLines.clear();
            } else
                sentLines.add(columns);
        }
        out.close();
        System.out.println("Extracted " + simpleSentCount + " simple sentences out of " + sentCount);
    }
}
