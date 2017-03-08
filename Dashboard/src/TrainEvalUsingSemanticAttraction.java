import com.sspr.semantics.RoWordNet;
import com.sspr.semantics.SemanticAttractionModel;
import maltProcessing.EvaluateMaltModel;
import org.maltparser.MaltParserService;
import org.maltparser.core.options.OptionManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by Planetaria on 3/8/2017.
 */
public class TrainEvalUsingSemanticAttraction {
    public static void main (String[] args) throws Exception {
        RoWordNet rown = new RoWordNet();
        rown.load(args[2]);
        SemanticAttractionModel semanticAttractionScores = new SemanticAttractionModel(rown, new FileInputStream(args[3]), new SemanticAttractionModel.ContextBuilder() {
            @Override
            public String buildContext(int tokenToBuildContextFor, List<String> sentLemmas, List<String> sentPos, List<Integer> sentHeads) {
                //find the rightmost preposition attached to word i
                String prep = "_";
                for (int j = 0; j < sentLemmas.size(); j++) {
                    if (sentHeads.get(j) - 1 == tokenToBuildContextFor && (sentPos.get(j).startsWith("S"))) {//word j has i as head
                        prep = sentLemmas.get(j);
                    }
                }

                String context = ((sentHeads.get(tokenToBuildContextFor) != 0) ? sentLemmas.get(sentHeads.get(tokenToBuildContextFor) - 1) : "ROOT") + " -> [" + prep + " -> " + sentPos.get(tokenToBuildContextFor).substring(0, 1) + "]";
                //else
                //    context = "ROOT";
                return context;
            }
        });
        OptionManager.semanticAttractionModel = semanticAttractionScores;

        AddFeatureColumns.addFeatures(new File(args[0]), new File("train_corpus_++.txt"));
        AddFeatureColumns.addFeatures(new File(args[1]), new File("test_corpus_++.txt"));
        MaltParserService service = new MaltParserService(0);
        service.runExperiment("-c maltmodel -f train_options.xml -i train_corpus_++.txt -m learn -ic UTF-8");
        service.runExperiment("-c maltmodel -f train_options.xml -i test_corpus_++.txt -o test_corpus_parsed.txt -m parse -ic UTF-8");
        Map<String, Float> eval = EvaluateMaltModel.Eval(new FileInputStream("test_corpus_++.txt"), new FileInputStream("test_corpus_parsed.txt"));
        System.out.println(eval.toString());
    }
}
