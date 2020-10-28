package unsw.gloriaromanus.Backend;

import org.json.*;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.StackWalker.Option;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;


import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;

public class Database {

    
    private String loadAddress;
    public Map<String,ArrayList<Unit>> provinceUnit;

    public Map<String,Faction> provinceList;

    public Map<String, ArrayList<Province>> factionList;
    public String address;

    // assign default unit to each province 
    public Database() throws IOException {

        address = "src/unsw/gloriaromanus/initial_province_ownership.json";


        provinceList = setProvinceToOwningFactionMap();
        provinceUnit = setOwningUnit();

        factionList = setOwningProvince();
        loadAddress = "hello";


    }
 
    public Map<String,ArrayList<Unit>> getProvinceUnit() {

        return provinceUnit;

    }

    public Map<String,Faction > getFactionProvince() {
        return provinceList;
    }


    public void addFaction(String faction) throws IOException {
        addtoFile(faction, "F", " ");
    }

    public void addProvince(String province, String faction) throws IOException {
        addtoFile(faction, "P", province);
    }

    /**
     * adds faction/province to the database
     * @param faction
     * @param option
     * @param province
     * @throws IOException
     */
    public void addtoFile(String faction, String option, String province) throws IOException {

        String content = Files.readString(Paths.get(address));
        JSONObject ownership = new JSONObject(content);

        if (option.equals("F")) {
            JSONArray empty = new JSONArray();
            ownership.put(faction, empty);
        }

        else {
            Object object = ownership.get(faction);
            JSONArray list = (JSONArray) object;
            list.put(province);
        }
            // Files.write(path, bytes, options)
            Files.writeString(Paths.get(address), ownership.toString());


        
    }

    /**
     * Function initilises the map of province to faction and set the province list for each faction 
     * @return map of province to faction 
     * @throws IOException
     */

    private Map<String, ArrayList<Unit>> setOwningUnit() {
        Map<String, ArrayList<Unit>> m = new HashMap<String, ArrayList<Unit>>();
        for (String provinceName : provinceList.keySet()) {
            ArrayList<Unit> u = new ArrayList<Unit> ();
            m.put(provinceName, u);
          }

          return m;
    }

    private Map<String,Faction> setProvinceToOwningFactionMap() throws IOException {
        ArrayList<Province> ps = new ArrayList<Province> ();
        Map<String,Faction> m = new HashMap<String,Faction>();
        String content = Files.readString(Paths.get(address));
        JSONObject ownership = new JSONObject(content);
        for (String key : ownership.keySet()) {
          // key will be the faction name
          JSONArray ja = ownership.getJSONArray(key);
          // value is province name
          for (int i = 0; i < ja.length(); i++) {
            String value = ja.getString(i);

            Faction f = new Faction(key);
            m.put(value,f);

          }
          
        }
        return m;
      }

      private Map<String,ArrayList<Province>> setOwningProvince() throws IOException {
        Map<String,ArrayList<Province>> m = new HashMap<String,ArrayList<Province>>();
        String content = Files.readString(Paths.get(address));
        JSONObject ownership = new JSONObject(content);
        for (String key : ownership.keySet()) {
            ArrayList<Province> ps = new ArrayList<Province> ();

          // key will be the faction name
          JSONArray ja = ownership.getJSONArray(key);
          // value is province name
          for (int i = 0; i < ja.length(); i++) {
            String value = ja.getString(i);

            Faction f = new Faction(key);
            ps.add(new Province(value,this));

          }
          m.put(key,ps);
          
        }
        return m;
      }

    

    public void saveGame() throws IOException {

        for (String f: factionList.keySet()) {
            for (Province p: factionList.get(f)) {
                saveProvince(f,p);
            }
        }
      
    }

    public void loadGame() throws IOException {

        for (String f: factionList.keySet()) {
            for (Province p: factionList.get(f)) {
                loadProvince(f,p);
            }
        }
      
    } 
    
    public void saveProvince(String f, Province p) throws IOException {

        //TODO: save other features of the province in the file 
        var saveAdress = "/Users/eli/Desktop/t13a-oop/src/unsw/gloriaromanus/Backend/configs/load.json";
        String content = Files.readString(Paths.get(saveAdress));
        JSONObject ownership = new JSONObject(content);
        JSONObject faction = ownership.getJSONObject(f);
        JSONObject province = faction.getJSONObject(p.name);

        province.put("unit", p.ListOfUnitString());
        Files.writeString(Paths.get(saveAdress), ownership.toString());


    } 

    // creating the database --> set up the ownership again
    // laod the information of the player
    //load the units for each province
    //load wealth, taxfactory, texrate

    public void loadProvince(String f, Province p) throws IOException {


        var saveAdress = "/Users/eli/Desktop/t13a-oop/src/unsw/gloriaromanus/Backend/configs/load.json";

        String content = Files.readString(Paths.get(saveAdress));
        JSONObject ownership = new JSONObject(content);
        JSONObject faction = ownership.getJSONObject(f);
        JSONObject province = faction.getJSONObject(p.name);
        JSONArray ul = province.getJSONArray("unit");
        
        for (int i = 0; i < ul.length();i++) {
            Unit u = new Unit(ul.getString(i));
            provinceUnit.get(p.name).add(u);
        }

        resetFile();


    }
    private void resetFile() throws IOException {
        var saveAdress = "/Users/eli/Desktop/t13a-oop/src/unsw/gloriaromanus/Backend/configs/layout_load.json";
        String content = Files.readString(Paths.get(saveAdress));
        JSONObject ownership = new JSONObject(content);
        Files.writeString(Paths.get("/Users/eli/Desktop/t13a-oop/src/unsw/gloriaromanus/Backend/configs/load.json"), ownership.toString());

    }
    
}








