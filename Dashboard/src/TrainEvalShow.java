import maltProcessing.EvaluateMaltModel;
import org.maltparser.MaltParserService;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Created by radsimu on 10/26/2016.
 * Train, Eval and show
 */
public class TrainEvalShow {
    public static void main (String[] args) throws Exception {
        AddFeatureColumns.addFeatures(new File(args[0]), new File("train_corpus_++.txt"));
        AddFeatureColumns.addFeatures(new File(args[1]), new File("test_corpus_++.txt"));
        MaltParserService service = new MaltParserService(0);
        service.runExperiment("-c maltmodel -f train_options.xml -i train_corpus_++.txt -m learn -ic UTF-8");
        service.runExperiment("-c maltmodel -f train_options.xml -i test_corpus_++.txt -o test_corpus_parsed.txt -m parse -ic UTF-8");
        Map<String, Float> eval = EvaluateMaltModel.Eval(new FileInputStream("test_corpus_++.txt"), new FileInputStream("test_corpus_parsed.txt"));
        System.out.println(eval.toString());
        se.vxu.msi.malteval.MaltEvalConsole.main(new String[]{"-v", "1", "-g", "test_corpus_++.txt", "-s", "test_corpus_parsed.txt"});
    }
}