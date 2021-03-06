package unsw.gloriaromanus.Backend.tax;

import org.json.JSONArray;
import org.json.JSONObject;

public class VeryHighTax implements TaxRate {
        
    final public static String TYPE = "VeryHigh";
    private int wealth;
    private double taxRate;

    public VeryHighTax() {
        wealth = 10;
        taxRate = 0.1;
    }

    public int getTaxWealth() {
        return wealth;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public JSONObject getMoraleModifier() {
        JSONObject json = new JSONObject();
        json.put("type", "morale");
        json.put("value", -1);
        json.put("strategy", "add");
        JSONObject modifier = new JSONObject();
        modifier.put("friendly", new JSONArray(json));
        return modifier;
    }

    @Override
    public String toString() {
        return "Very High tax";
    }
}
