import ggs.engine.core.GGSException;
import org.maltparser.MaltParserService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uaic.postagger.tagger.HybridPOStagger;
import uaic.postagger.tagger.MorphologicDictionary;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Created by radu.simionescu on 02/12/16.
 */
public class ParseCorola {

    public static void main (String[] args) throws Exception {
        //BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        //String line;
        //Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));

        for (File f : new File("corola/1_corola_raw").listFiles()){
            File xmlFile = new File("corola/2_corola_posUaic_xml/" + f.getName() +".xml");
            if ( xmlFile.exists())
                System.out.println("Skipped tagging " + f.getName() + ". Tagged version already exists");
            else {
                System.out.println("Tagging " + f.getName());

                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF8"));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    sb.append(line).append("\n");
                }

                OutputStream os = new FileOutputStream(xmlFile);
                HybridPOStagger tagger = AddFeatureColumns.getTagger();
                Document doc = tagger.tagTextXmlDetailed_en(sb.toString());

                TransformerFactory tfactory = TransformerFactory.newInstance();
                Transformer serializer;
                try {
                    serializer = tfactory.newTransformer();
                    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                    serializer.transform(new DOMSource(doc), new StreamResult(os));
                } catch (TransformerException e) {
                    throw new RuntimeException(e);
                }

                br.close();
                os.close();

            }
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        for (File f : new File("corola/2_corola_posUaic_xml").listFiles()) {
            File conllFile = new File("corola/3_corola_conll++/" + f.getName()+".conll");
            File conllFilePlus = new File ("corola/3_corola_conll++/" + f.getName()+".conll++");
            if (conllFilePlus.exists())
                System.out.println("Skipping converting to conll and adding features to " + f.getName() + ". conll++ version already exists");
            else {
                System.out.println("Converting to conll and adding features to " + f.getName());
                Document doc = dBuilder.parse(new FileInputStream(f));
                Element root = doc.getDocumentElement();
                root.normalize();
                NodeList sentences = doc.getElementsByTagName("S");
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(conllFile), "UTF-8"));

                for (int i = 0; i < sentences.getLength(); i++) {
                    StringBuilder sb = new StringBuilder();
                    NodeList words = ((Element) sentences.item(i)).getElementsByTagName("W");

                    for (int j = 0; j < words.getLength(); j++) {
                        Element word = (Element) words.item(j);
                        String form = word.getTextContent();
                        String postag = word.getAttribute("MSD");
                        if (postag.length() > 5)
                            postag = postag.substring(0, 5);
                        String lemma = word.getAttribute("LEMMA");

                        sb.append(Integer.toString(j + 1));
                        sb.append("\t");
                        sb.append(form);
                        sb.append("\t");
                        sb.append(lemma);
                        sb.append("\t");
                        sb.append(postag);
                        sb.append("\t");
                        sb.append(postag);
                        sb.append("\t_\t_\t_\t_\t_\n");
                    }

                    sb.append("\n");
                    out.write(sb.toString());
                }
                out.close();

                AddFeatureColumns.addFeatures(conllFile,conllFilePlus);
                conllFile.delete();
            }
        }

        Files.copy(Paths.get("batches_taggedWithUaic", "batches_taggedWithUaic_10.uaic.model.mco"), Paths.get("batches_taggedWithUaic_10.uaic.model.mco"), StandardCopyOption.REPLACE_EXISTING);

        for (File f : new File("corola/3_corola_conll++").listFiles()){
            File parsedConll = new File ("corola/4_corola_conll++_parsed/" + f.getName()+".parsed");
            if (parsedConll.exists())
                System.out.println("Skipping parsing " + f.getName() + ". Parsed version already exists");
            else{
                System.out.println("Parsing " + f.getName());
                MaltParserService service = new MaltParserService(0);
                Files.copy(f.toPath(), Paths.get(f.getName()), StandardCopyOption.REPLACE_EXISTING);
                service.runExperiment("-c batches_taggedWithUaic_10.uaic.model.mco -f train_options.xml -i " + f.getName() + " -o " + parsedConll.getName() + " -m parse -ic UTF-8");
                Files.copy(Paths.get(parsedConll.getName()), parsedConll.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        for (File f : new File ("corola/5_corola_conll++_parsed_simple").listFiles())
            f.delete();

        for (File f : new File ("corola/4_corola_conll++_parsed").listFiles()){
            File parsedSimpleFile = new File("corola/5_corola_conll++_parsed_simple/simple_" + f.getName());
            ExtractSimpleSentences.main(new String[]{f.toString(), parsedSimpleFile.toString()});
        }
    }
}
