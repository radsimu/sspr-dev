import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Planetaria on 11/20/2016.
 */


public class BuildModel {
    public static void main(String[] args) throws FileNotFoundException, JWNLException {
        JWNL.initialize(new FileInputStream("wn/file_properties.xml"));
        Dictionary.getInstance().lookupIndexWord(POS.NOUN, "animal");
    }
}
