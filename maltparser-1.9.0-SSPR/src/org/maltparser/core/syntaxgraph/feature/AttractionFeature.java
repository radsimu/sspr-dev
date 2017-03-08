package org.maltparser.core.syntaxgraph.feature;

import com.sspr.semantics.SemanticAttractionModel;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.function.FeatureMapFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;
import org.maltparser.core.syntaxgraph.SyntaxGraphException;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class AttractionFeature implements FeatureMapFunction {
    public SemanticAttractionModel semanticAttractionModel;

    public final static Class<?>[] paramTypes = {org.maltparser.core.feature.function.FeatureFunction.class,
            org.maltparser.core.feature.function.FeatureFunction.class,
            org.maltparser.core.feature.function.FeatureFunction.class,
            org.maltparser.core.feature.function.FeatureFunction.class};
    String lemma1;
    String pos1;
    String lemma2;
    String pos2;


    private FeatureFunction lemma1Function;
    private FeatureFunction lemma2Function;
    private FeatureFunction pos1Function;
    private FeatureFunction pos2Function;
    private final SymbolTableHandler tableHandler;
    private SymbolTable table;
    private final SingleFeatureValue featureValue;

    public AttractionFeature(SymbolTableHandler tableHandler) throws MaltChainedException {
        this.featureValue = new SingleFeatureValue(this);
        this.tableHandler = tableHandler;
    }

    /**
     * Initialize the distance feature function
     *
     * @param arguments an array of arguments with the type returned by getParameterTypes()
     * @throws MaltChainedException
     */
    public void initialize(Object[] arguments) throws MaltChainedException {
        if (arguments.length != 4) {
            throw new SyntaxGraphException("Could not initialize DistanceFeature: number of arguments is not correct. ");
        }
        // Checks that the two arguments are address functions
        if (!(arguments[0] instanceof FeatureFunction)) {
            throw new SyntaxGraphException("Could not initialize DistanceFeature: the first argument is not a feature function. ");
        }
        if (!(arguments[1] instanceof FeatureFunction)) {
            throw new SyntaxGraphException("Could not initialize DistanceFeature: the second argument is not a feature function. ");
        }
        if (!(arguments[2] instanceof FeatureFunction)) {
            throw new SyntaxGraphException("Could not initialize DistanceFeature: the third argument is not a feature function. ");
        }
        if (!(arguments[3] instanceof FeatureFunction)) {
            throw new SyntaxGraphException("Could not initialize DistanceFeature: the fourth argument is not a feature function. ");
        }

        lemma1Function = (FeatureFunction)arguments[0];
        pos1Function = (FeatureFunction)arguments[1];
        lemma2Function = (FeatureFunction)arguments[2];
        pos2Function = (FeatureFunction)arguments[3];
    }

    /**
     * Returns an array of class types used by the feature extraction system to invoke initialize with
     * correct arguments.
     *
     * @return an array of class types
     */
    public Class<?>[] getParameterTypes() {
        return paramTypes;
    }

    /**
     * Returns the string representation of the integer <code>code</code> according to the distance feature function.
     *
     * @param code the integer representation of the symbol
     * @return the string representation of the integer <code>code</code> according to the distance feature function.
     * @throws MaltChainedException
     */
    public String getSymbol(int code) throws MaltChainedException {
        return table.getSymbolCodeToString(code);
    }

    /**
     * Returns the integer representation of the string <code>symbol</code> according to the distance feature function.
     *
     * @param symbol the string representation of the symbol
     * @return the integer representation of the string <code>symbol</code> according to the distance feature function.
     * @throws MaltChainedException
     */
    public int getCode(String symbol) throws MaltChainedException {
        return table.getSymbolStringToCode(symbol);
    }

    /**
     * Cause the feature function to update the feature value.
     *
     * @throws MaltChainedException
     */
    public void update() throws MaltChainedException {
        lemma1Function.update();
        lemma2Function.update();
        pos1Function.update();
        pos2Function.update();

        lemma1 = ((SingleFeatureValue)lemma1Function.getFeatureValue()).getSymbol();
        pos1 = ((SingleFeatureValue)pos1Function.getFeatureValue()).getSymbol();
        lemma2 = ((SingleFeatureValue)lemma2Function.getFeatureValue()).getSymbol();
        pos2 = ((SingleFeatureValue)pos2Function.getFeatureValue()).getSymbol();

        // if arg1 or arg2 is null, then set a NO_NODE null value as feature value
        if (lemma1 == null || lemma2 == null || pos1 == null || pos2 == null) {
            featureValue.setValue(0);
            featureValue.setNullValue(true);
        } else {
            featureValue.setValue(1);
            // Tells the feature value that the feature is known and is not a null value
            featureValue.setNullValue(false);
        }
    }

    /**
     * Returns the feature value
     *
     * @return the feature value
     */
    public FeatureValue getFeatureValue() {
        return featureValue;
    }

    /**
     * Returns the symbol table used by the distance feature function
     *
     * @return the symbol table used by the distance feature function
     */
    public SymbolTable getSymbolTable() {
        return table;
    }

    /**
     * Returns symbol table handler
     *
     * @return a symbol table handler
     */
    public SymbolTableHandler getTableHandler() {
        return tableHandler;
    }

    /**
     * Sets the symbol table used by the distance feature function
     *
     * @param table
     */
    public void setSymbolTable(SymbolTable table) {
        this.table = table;
    }

    public int getType() {
        return ColumnDescription.REAL;
    }

    public String getMapIdentifier() {
        return getSymbolTable().getName();
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return obj.toString().equals(this.toString());
    }

    public int hashCode() {
        return 217 + (null == toString() ? 0 : toString().hashCode());
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Attraction(");
        sb.append(lemma1);
        sb.append(", ");
        sb.append(pos1);
        sb.append(", ");
        sb.append(lemma2);
        sb.append(", ");
        sb.append(pos2);
        sb.append(')');
        return sb.toString();
    }
}

