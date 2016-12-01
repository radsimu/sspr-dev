import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uaic.segmenter.WordStruct;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by radu.simionescu on 21/11/16.
 */
public class RoWordNet {
    Map<String, Synset> synsetsByIds;
    Map<String, Set<Synset>> literalsToSynsents;

    public enum WNRelation {
        hyponym,
        hypernym,
        cause,
        entailment,
        attribute,
        verb_group,
        also_see,
        similar_to,
        substance_meronym,
        substance_holonym,
        member_holonym,
        member_meronym,
        part_hypernym,
        part_holonym,
        part_meronym,
        instance_hyponym,
        instance_hypernym,
        near_eng_derivat,
        near_also_see,
        near_derived_from,
        near_pertainym,
        near_participle,
        near_antonym,
        near_verb_group,
        near_domain_USAGE,
        near_domain_REGION,
        near_domain_TOPIC,
        near_domain_member_USAGE,
        near_domain_member_REGION,
        near_domain_member_TOPIC,
        domain_member_TOPIC,
        domain_member_USAGE,
        domain_member_REGION,
        domain_TOPIC,
        domain_USAGE,
        domain_REGION
    }

    public Set<Synset> getSynsetsForLiterral(String lemma) {
        return literalsToSynsents.get(WordStruct.getCanonicalWord(lemma));
    }

    public Map<String, Synset> getAllSynsetsById() {
        return synsetsByIds;
    }

    public static class Synset {
        public String id;
        public String pos;
        public Map<String, String> literals = new HashMap<>(); //literal lemma to synset sense
        Map<WNRelation, Set<String>> relationsById = new HashMap<>();
        public Map<WNRelation, Set<Synset>> relations = new HashMap<>();
        public String def;
        public String domain;
        public String sumo;

        @Override
        public String toString() {
            return literals.keySet().toString() + " -> (domain:" + domain + ") + (SUMO:" + sumo + ")";
        }
    }

    public void load(String file) throws ParserConfigurationException, IOException, SAXException {
        long start = System.currentTimeMillis();
        System.out.println("Loading wordnet");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new FileInputStream(file));
        Element root = doc.getDocumentElement();
        root.normalize();
        synsetsByIds = new HashMap<>();
        literalsToSynsents = new HashMap<>();
        NodeList synsetElems = doc.getElementsByTagName("SYNSET");

        for (int i = 0; i < synsetElems.getLength(); i++) {
            Element synsetElem = (Element) synsetElems.item(i);
            Synset synset = new Synset();
            synset.id = synsetElem.getElementsByTagName("ID").item(0).getTextContent();
            synset.pos = synsetElem.getElementsByTagName("POS").item(0).getTextContent();
            synset.def = synsetElem.getElementsByTagName("DEF").item(0).getTextContent();
            try {
                synset.domain = synsetElem.getElementsByTagName("DOMAIN").item(0).getTextContent();
            } catch (NullPointerException ex) {
            }
            try {
                synset.sumo = synsetElem.getElementsByTagName("SUMO").item(0).getTextContent();
            } catch (NullPointerException ex) {
            }

            NodeList literals = synsetElem.getElementsByTagName("SYNONYM");
            if (literals.getLength() > 0) {
                literals = ((Element) literals.item(0)).getElementsByTagName("LITERAL");
            }

            for (int j = 0; j < literals.getLength(); j++)
                synset.literals.put(WordStruct.getCanonicalWord(literals.item(j).getFirstChild().getTextContent()), ((Element) literals.item(j)).getElementsByTagName("SENSE").item(0).getTextContent());

            for (String literal : synset.literals.keySet()) {
                Set<Synset> syns = literalsToSynsents.get(literal);
                if (syns == null) {
                    syns = new HashSet<>();
                    literalsToSynsents.put(literal, syns);
                }
                syns.add(synset);
            }

            NodeList relations = synsetElem.getElementsByTagName("ILR");
            for (int j = 0; j < relations.getLength(); j++) {
                WNRelation rel = WNRelation.valueOf(((Element) relations.item(j)).getElementsByTagName("TYPE").item(0).getTextContent());
                Set<String> rels = synset.relationsById.get(rel);
                if (rels == null) {
                    rels = new HashSet<>();
                    synset.relationsById.put(rel, rels);
                }
                rels.add(relations.item(j).getFirstChild().getTextContent());
            }
            synsetsByIds.put(synset.id, synset);
        }

        for (Synset synset : synsetsByIds.values()) {
            for (Map.Entry<WNRelation, Set<String>> entry : synset.relationsById.entrySet()) {
                HashSet<Synset> rels = new HashSet<>();
                for (String id : entry.getValue())
                    rels.add(synsetsByIds.get(id));
                synset.relations.put(entry.getKey(), rels);
            }
            synset.relationsById = null;
        }

        System.out.println("Finished loading wordnet in " + (System.currentTimeMillis() - start));
    }
}
