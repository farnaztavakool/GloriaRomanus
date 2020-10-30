package unsw.gloriaromanus.Backend;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;


public class Unit {
    private static Long ID = 0L;
    
    private String name;
    private Long unitID;
    private String type;
    private boolean melee;
    private int numTroops;
    private int cost;
    private int trainTime;
    private double attack;
    private double speed;
    private double morale;
    private double shield;
    private double defence;  // Melee units only
    private double charge;   // Cavalry units only
    private String abilityType;
    private JSONArray ability;
    private JSONArray modifiers;
    private JSONObject baseValues;

    
    public Unit(String name, JSONObject unitConfig, JSONObject abilityConfig) {
        this.name = name;
        this.unitID = ID;
        ID = ID + 1;
        loadUnitFromConfig(name, unitConfig, abilityConfig);
    }


    public String getName() {
        return name;
    }

    public Long getUnitID() {
        return unitID;
    }


    public String getType() {
        return type;
    }


    public int getNumTroops() {
        return numTroops;
    }


    public int getCost() {
        return cost;
    }


    public int getTrainTime() {
        return trainTime;
    }


    public double getAttack() {
        return attack;
    }


    public double getSpeed() {
        return speed;
    }


    public double getMorale() {
        return morale;
    }


    public double getShield() {
        return shield;
    }


    public double getDefence() {
        return defence;
    }


    public double getCharge() {
        return charge;
    }


    public String getAbilityType() {
        return abilityType;
    }


    public JSONArray getAbility() {
        return ability;
    }


    public JSONArray getModifiers() {
        return modifiers;
    }
    

    public boolean isMelee() {
        return melee;
    }


    public boolean isRanged() {
        return !isMelee();
    }

    public boolean isTrained() {
        return trainTime == 0;
    }


    /**
     * Inflicts given number of casualties on unit,
     * minimum of 0 troops remaining
     * 
     * @param num Number of troops to 'kill'
     */
    public void inflictCasualties(int num) {
        if (numTroops - num < 0) {
            numTroops = 0;
        } else {
            numTroops = numTroops - num;
        }
    }


    /**
     * Returns True if unit has troops remaining,
     * otherwise False
     * 
     * @return True if unit is alive, otherwise False
     */
    public boolean isAlive() {
        return numTroops != 0;
    }


    /**
     * Called at start of a new turn
     * Changes anything that needs to be changed at start of a turn
     */
    public void newTurn() {
        if (trainTime != 0) {
            trainTime = trainTime - 1;
        }
    }

    
    /**
     * Adds a new modifier to Unit
     * 
     * @param modifier New modifier object
     * @param who Apply to friendly of enemy unit
     */
    public void addModifier(JSONObject modifier) {
        modifiers.put(modifier);
    }


    /**
     * Removes given modifier from modifiers,
     * if that modifier is in the JSONArray
     * 
     * @param modifier Modifier to remove
     */
    public void removeModifier(JSONObject modifier) {
        if (!modifiers.isEmpty()) {
            for (int i = 0; i < modifiers.length(); i++) {
                JSONObject mod = modifiers.getJSONObject(i);
                if (mod.equals(modifier)) {
                    modifiers.remove(i);
                }
            }
        }
    }



    /**
     * Returns the value of specified attribute after applying
     * the friendly modifiers to it
     * 
     * @param type Value to modifiy
     * @param who Friendly or enemy modifiers
     * @return Modified value
     */
    public double getFriendlyModifiedValue(String type) {
        Iterator<Object> json = modifiers.iterator();
        double val = baseValues.optDouble(type, 0);
        while (json.hasNext()) {
            JSONObject mod = (JSONObject)json.next();
            if (type.equals(mod.getString(type)) && "friendly".equals(mod.getString("who"))) {
                if ("add".equals(mod.getString("strategy"))) {
                    val = val + mod.optDouble("value", 0);
                } else if ("multiply".equals(mod.getString("strategy"))) {
                    val = val * mod.optDouble("value", 1);
                }
            }
        }
        return val;
    }


    /**
     * Returns the value of specified attribute after applying
     * the enemy modifiers to it
     * 
     * @param type Value to modifiy
     * @param who Friendly or enemy modifiers
     * @return Modified value
     */
    public double getEnemyModifiedValue(String type) {
        Iterator<Object> json = modifiers.iterator();
        double val = baseValues.optDouble(type, 0);
        while (json.hasNext()) {
            JSONObject mod = (JSONObject)json.next();
            if (type.equals(mod.getString(type)) && "enemy".equals(mod.getString("who"))) {
                if ("add".equals(mod.getString("strategy"))) {
                    val = val + mod.optDouble("value", 0);
                } else if ("multiply".equals(mod.getString("strategy"))) {
                    val = val * mod.optDouble("value", 1);
                }
            }
        }
        return val;
    }

    
    /**
     * Loads the base config values for the specified unit
     * from the configs/unit_config.json file
     * 
     * @param name of unit to train
     * @throws IOException
     */
    private void loadUnitFromConfig(String name, JSONObject unitsConfig, JSONObject abilityConfig) {
        JSONObject config = unitsConfig.getJSONObject(this.name);
        
        this.type = config.optString("type", "infantry");
        this.melee = "melee".equals(config.optString("attackType", "melee")) ? true : false;
        this.numTroops = config.optInt("numTroops", 1);
        this.cost = config.optInt("cost", 1);
        this.trainTime = config.optInt("trainTime", 1);
        this.attack = config.optDouble("attack", 1);
        this.morale = config.optDouble("morale", 1);
        this.shield = config.optDouble("shield", 1);
        this.abilityType = config.optString("ability", "noAbility");
        this.ability = getAbilityJSON(this.abilityType, abilityConfig);
        this.charge = "cavalry".equals(this.type) ? config.optDouble("charge", 1) : 0;
        this.defence = isMelee() ? config.optDouble("defence", 1) : 0;
        switch (this.type) {
            case "cavalry":
                this.speed = 15;
                break;
            case "infantry":
                this.speed = 10;
                break;
            case "artillery":
                this.speed = 4;
                break;
            default:
                this.speed = 1;
        }
        this.modifiers = new JSONObject();
        modifiers.put("friendly", new JSONArray());
        modifiers.put("enemy", new JSONArray());

        this.baseValues = config;
    }


    /**
     * Loads the modifiers JSONObject
     * for the specified ability
     * 
     * @param ability
     * @return JSONObject of specified ability
     * @throws IOException
     */
    private JSONArray getAbilityJSON(String ability, JSONObject abilityConfig) {
        JSONArray config = abilityConfig.getJSONArray(this.abilityType);
        return config;
    }


    @Override
    public String toString() {
        return "unit (" + name + ", id: " + unitID + ")";
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Unit u = (Unit)obj;
        return unitID == u.getUnitID();
    }
}
