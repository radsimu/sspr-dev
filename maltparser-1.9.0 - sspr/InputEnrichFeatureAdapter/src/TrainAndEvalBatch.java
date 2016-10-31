import maltProcessing.EvaluateMaltModel;
import org.maltparser.Malt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Planetaria on 10/26/2016.
 */
public class TrainAndEvalBatch {
    public static void main (String[] args) throws IOException {
        AddFeatureColumns.addFeatures(new File(args[0]), new File("train_corpus_++.txt"));
        AddFeatureColumns.addFeatures(new File(args[1]), new File("test_corpus_++.txt"));
        Malt.main(new String[]{"-c", "maltmodel", "-f", "train_options.xml", "-i", "train_corpus_++.txt", "-m", "learn", "-ic", "UTF-8"});
        Malt.main(new String[]{"-c", "maltmodel", "-i", "test_corpus_++.txt", "-o", "test_corpus_parsed.txt", "-m", "parse", "-ic", "UTF-8"});
        Map<String, Float> eval = EvaluateMaltModel.Eval(new FileInputStream("test_corpus_++.txt"), new FileInputStream("test_corpus_parsed.txt"));
        System.out.println(eval.toString());
    }
}
