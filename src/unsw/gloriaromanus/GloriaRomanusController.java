
package unsw.gloriaromanus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.GeoPackage;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol.HorizontalAlignment;
import com.esri.arcgisruntime.symbology.TextSymbol.VerticalAlignment;
import com.esri.arcgisruntime.data.Feature;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.util.Pair;
import unsw.gloriaromanus.Backend.*;

public class GloriaRomanusController extends MenuController {

  @FXML
  private MapView mapView;

  @FXML
  private StackPane stackPaneMain;

  // could use ControllerFactory?
  private ArrayList<Pair<MenuController, VBox>> controllerParentPairs;

  private ArcGISMap map;

  private Map<String, String> provinceToOwningFactionMap;

  private Map<String, Integer> provinceToNumberTroopsMap;

  private String humanFaction;

  private Feature currentlySelectedHumanProvince;
  private Feature currentlySelectedEnemyProvince;

  private FeatureLayer featureLayer_provinces;

  private Database db;

  private Player player;

  private currentStatusController status;

  private Map<String, MenuController> menusList;

  private String currentMenu;

  @FXML
  private void initialize() throws JsonParseException, JsonMappingException, IOException, InterruptedException {
    // TODO = you should rely on an object oriented design to determine ownership
    provinceToOwningFactionMap = getProvinceToOwningFactionMap();

    provinceToNumberTroopsMap = new HashMap<String, Integer>();

    // Random r = new Random();
    for (String provinceName : provinceToOwningFactionMap.keySet()) {
      provinceToNumberTroopsMap.put(provinceName, 0);
    }
    /**
     * set up list of observers in provinces
     */
    // parent();

    db = new Database();

    currentlySelectedHumanProvince = null;
    currentlySelectedEnemyProvince = null;

    controllerParentPairs = new ArrayList<Pair<MenuController, VBox>>();

    menusList = new HashMap<String, MenuController>();

    setMenu();

    stackPaneMain.getChildren().add(controllerParentPairs.get(0).getValue());

    checkLoad();


    initializeProvinceLayers();

  }

  
  
  private void checkLoad() throws IOException {
    String content = Files.readString(Paths.get("src/unsw/gloriaromanus/load.json"));
    JSONObject ownership = new JSONObject(content);
    System.out.println(ownership.get("load").toString()+"loading the data");
    if (ownership.get("load").toString().equals("true")) {
      load();
      System.out.println("this is current menu"+currentMenu);
      nextMenu("unsw.gloriaromanus.SignupPaneController","unsw.gloriaromanus.ActionController");

    }
  }
  

  private void load() throws IOException {
    db.loadGame();
    JSONObject jo = new JSONObject();
    jo.put("load",false);
    try (FileWriter file = new FileWriter("src/unsw/gloriaromanus/load.json")) {

        file.write(jo.toString());
        file.flush();

    } catch (IOException e) {
        e.printStackTrace();
    }

    player = db.getCurrentPlayer();
    subscribe();
    for (Player p: db.getPlayers()) {
      for (Province pp: p.getFaction().getProvinces()) {
        provinceToNumberTroopsMap.put(pp.getName(), pp.getNTroops());

      }
    }
    System.out.println( player.getUsername());
    status.setName(player.getUsername());
    status.setYear(db.getGameYear());


  }
  private void setMenu() throws IOException {

    String[] menus = { "signupPane.fxml", "currentStatus.fxml", "Action.fxml", "invasion_menu.fxml", "moveMenu.fxml",
        "getUnit.fxml" };

    for (String fxmlName : menus) {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
      VBox root = (VBox) loader.load();
      MenuController menuController = (MenuController) loader.getController();
      menuController.setParent(this);
      controllerParentPairs.add(new Pair<MenuController, VBox>(menuController, root));

      menusList.put(menuController.getClass().getName(), menuController);
    }
    status = (currentStatusController) controllerParentPairs.get(1).getKey();
    currentMenu = controllerParentPairs.get(0).getKey().getClass().getName();

  }

  /**
   * TODO: Player selecting units in the province to attack
   */
  public void clickedInvadeButton(String human, String enemy, String unit) throws IOException {
    player.selectProvince(human);
    player.trainUnit("soldier");
    Unit select = null;
    for (Unit u : player.getFaction().getSelectedProvince().getUnits()) {
      if (u.getName().equals(unit)) {
        select = u;
        break;
      }
    }
    if (select != null)
      player.selectUnit(select.getUnitID());
    InvasionMenuController men = (InvasionMenuController) controllerParentPairs.get(3).getKey();
    int result = player.invade(enemy);
    if (result == -1)
      men.appendToTerminal("You lost the battle");
    if (result == 0)
      men.appendToTerminal("It's a tie!");
    if (result == 1)
      men.appendToTerminal("Congradulation you won the battle");
    featureLayer_provinces
        .unselectFeatures(Arrays.asList(currentlySelectedEnemyProvince, currentlySelectedHumanProvince));

  }

  public void getUnit(String province, String unit) throws IOException {
    String message;
    player.selectProvince(province);
    if (player.trainUnit(unit) == 1)
      message = "successfully added the unit to be trained";
    else
      message = "cant train the unit currently";
    ((getUnitController) controllerParentPairs.get(5).getKey()).appendToTerminal(message);
  }

  /**
   * moves the troop / TODO: check if getID or getUNItId
   * 
   * @param to
   * @param from
   * @param unit
   * @throws IOException
   */
  public void MoveUnit(String to, String from, String unit) throws IOException {
    player.selectProvince(from);
    Unit x = null;
    for (Unit u : player.getFaction().getSelectedProvince().getUnits()) {
      if (u.getName().equals(unit))
        x = u;
    }
    String message;
    player.selectUnit(x.getUnitID());
    boolean result = player.moveUnits(to);
     if (result) message = "successfully moved units";
    else message = "can't move uni";
   ((moveMenuController)controllerParentPairs.get(4).getKey()).appendToTerminal( message);
    featureLayer_provinces.unselectFeature(currentlySelectedHumanProvince);
    featureLayer_provinces.unselectFeature(currentlySelectedEnemyProvince);

  }

  /**
   * run this initially to update province owner, change feature in each
   * FeatureLayer to be visible/invisible depending on owner. Can also update
   * graphics initially
   */

  private void initializeProvinceLayers() throws JsonParseException, JsonMappingException, IOException {

    Basemap myBasemap = Basemap.createImagery();
    // myBasemap.getReferenceLayers().remove(0);
    map = new ArcGISMap(myBasemap);
    mapView.setMap(map);

    // note - tried having different FeatureLayers for AI and human provinces to
    // allow different selection colors, but deprecated setSelectionColor method
    // does nothing
    // so forced to only have 1 selection color (unless construct graphics overlays
    // to give color highlighting)
    GeoPackage gpkg_provinces = new GeoPackage("src/unsw/gloriaromanus/provinces_right_hand_fixed.gpkg");
    gpkg_provinces.loadAsync();
    gpkg_provinces.addDoneLoadingListener(() -> {
      if (gpkg_provinces.getLoadStatus() == LoadStatus.LOADED) {
        // create province border feature
        featureLayer_provinces = createFeatureLayer(gpkg_provinces);
        map.getOperationalLayers().add(featureLayer_provinces);

      } else {
        System.out.println("load failure");
      }
    });

    addAllPointGraphics();
  }

  private void addAllPointGraphics() throws JsonParseException, JsonMappingException, IOException {
    mapView.getGraphicsOverlays().clear();

    InputStream inputStream = new FileInputStream(new File("src/unsw/gloriaromanus/provinces_label.geojson"));
    FeatureCollection fc = new ObjectMapper().readValue(inputStream, FeatureCollection.class);

    GraphicsOverlay graphicsOverlay = new GraphicsOverlay();

    for (org.geojson.Feature f : fc.getFeatures()) {
      if (f.getGeometry() instanceof org.geojson.Point) {
        org.geojson.Point p = (org.geojson.Point) f.getGeometry();
        LngLatAlt coor = p.getCoordinates();
        Point curPoint = new Point(coor.getLongitude(), coor.getLatitude(), SpatialReferences.getWgs84());
        PictureMarkerSymbol s = null;
        String province = (String) f.getProperty("name");
        String faction = provinceToOwningFactionMap.get(province);

        TextSymbol t = new TextSymbol(10, faction + "\n" + province + "\n" + provinceToNumberTroopsMap.get(province),
            0xFFFF0000, HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);

        switch (faction) {
          case "Gaul":
            // note can instantiate a PictureMarkerSymbol using the JavaFX Image class - so
            // could
            // construct it with custom-produced BufferedImages stored in Ram
            // http://jens-na.github.io/2013/11/06/java-how-to-concat-buffered-images/
            // then you could convert it to JavaFX image
            // https://stackoverflow.com/a/30970114

            // you can pass in a filename to create a PictureMarkerSymbol...
            s = new PictureMarkerSymbol(new Image((new File("images/Celtic_Druid.png")).toURI().toString()));
            break;
          case "Rome":
            // you can also pass in a javafx Image to create a PictureMarkerSymbol
            // (different to BufferedImage)
            s = new PictureMarkerSymbol("images/legionary.png");
            break;
          // TODO = handle all faction names, and find a better structure...
        }
        t.setHaloColor(0xFFFFFFFF);
        t.setHaloWidth(2);
        // Graphic gPic = new Graphic(curPoint, s);
        Graphic gText = new Graphic(curPoint, t);
        // graphicsOverlay.getGraphics().add(gPic);
        graphicsOverlay.getGraphics().add(gText);
      } else {
        System.out.println("Non-point geo json object in file");
      }

    }

    inputStream.close();
    mapView.getGraphicsOverlays().add(graphicsOverlay);
  }

  private FeatureLayer createFeatureLayer(GeoPackage gpkg_provinces) {
    FeatureTable geoPackageTable_provinces = gpkg_provinces.getGeoPackageFeatureTables().get(0);

    // Make sure a feature table was found in the package
    if (geoPackageTable_provinces == null) {
      System.out.println("no geoPackageTable found");
      return null;
    }

    // Create a layer to show the feature table
    FeatureLayer flp = new FeatureLayer(geoPackageTable_provinces);

    // https://developers.arcgis.com/java/latest/guide/identify-features.htm
    // listen to the mouse clicked event on the map view
    mapView.setOnMouseClicked(e -> {
      // was the main button pressed?
      if (e.getButton() == MouseButton.PRIMARY) {
        // get the screen point where the user clicked or tapped
        Point2D screenPoint = new Point2D(e.getX(), e.getY());

        // specifying the layer to identify, where to identify, tolerance around point,
        // to return pop-ups only, and
        // maximum results
        // note - if select right on border, even with 0 tolerance, can select multiple
        // features - so have to check length of result when handling it
        final ListenableFuture<IdentifyLayerResult> identifyFuture = mapView.identifyLayerAsync(flp, screenPoint, 0,
            false, 25);

        // add a listener to the future
        identifyFuture.addDoneListener(() -> {
          try {
            // get the identify results from the future - returns when the operation is
            // complete
            IdentifyLayerResult identifyLayerResult = identifyFuture.get();
            // a reference to the feature layer can be used, for example, to select
            // identified features
            if (identifyLayerResult.getLayerContent() instanceof FeatureLayer) {
              FeatureLayer featureLayer = (FeatureLayer) identifyLayerResult.getLayerContent();
              // select all features that were identified
              List<Feature> features = identifyLayerResult.getElements().stream().map(f -> (Feature) f)
                  .collect(Collectors.toList());

              if (features.size() > 1) {
                printMessageToTerminal("Have more than 1 element - you might have clicked on boundary!");
              } else if (features.size() == 1) {
                // note maybe best to track whether selected...
                Feature f = features.get(0);
                String province = (String) f.getAttributes().get("name");

                if (currentMenu.equals("unsw.gloriaromanus.moveMenuController"))
                  addMoveProvinces(f, province);
                if (currentMenu.equals("unsw.gloriaromanus.InvasionMenuController"))
                  addInvadeProvinces(f, province);
                else
                  addUnitProvince(f, province);

              }

            }
          } catch (InterruptedException | ExecutionException ex) {
            // ... must deal with checked exceptions thrown from the async identify
            // operation
            System.out.println("InterruptedException occurred");
          }
        });
      }
    });
    return flp;
  }

  public void addMoveProvinces(Feature f, String province) {
    if (provinceToOwningFactionMap.get(province).equals(humanFaction)) {

      if (currentlySelectedHumanProvince != null) {
        if (currentlySelectedEnemyProvince != null)
          featureLayer_provinces.unselectFeature(currentlySelectedEnemyProvince);
        currentlySelectedEnemyProvince = f;
        ((moveMenuController) controllerParentPairs.get(4).getKey()).setToProvince(province);

      } else {
        currentlySelectedHumanProvince = f;
        ((moveMenuController) controllerParentPairs.get(4).getKey()).setFromProvince(province);

      }
      featureLayer_provinces.selectFeature(f);
    }

  }

  public void addInvadeProvinces(Feature f, String province) {

    if (provinceToOwningFactionMap.get(province).equals(humanFaction)) {

      if (currentlySelectedHumanProvince != null) {
        featureLayer_provinces.unselectFeature(currentlySelectedHumanProvince);

      }
      currentlySelectedHumanProvince = f;
      ((InvasionMenuController) controllerParentPairs.get(3).getKey()).setInvadingProvince(province);

    } else {
      if (currentlySelectedEnemyProvince != null) {
        featureLayer_provinces.unselectFeature(currentlySelectedEnemyProvince);
      }
      currentlySelectedEnemyProvince = f;
      if (controllerParentPairs.get(3).getKey() instanceof InvasionMenuController) {
        ((InvasionMenuController) controllerParentPairs.get(3).getKey()).setOpponentProvince(province);

      }
    }

    featureLayer_provinces.selectFeature(f);

  }

  public void addUnitProvince(Feature f, String province) {

    if (provinceToOwningFactionMap.get(province).equals(humanFaction)) {

      if (currentlySelectedHumanProvince != null) {
        featureLayer_provinces.unselectFeature(currentlySelectedHumanProvince);
      }
      currentlySelectedHumanProvince = f;

      featureLayer_provinces.selectFeature(f);
      ((getUnitController) controllerParentPairs.get(5).getKey()).setProvince(province);
      ;

    }
  }

  public void clean() {
    if (currentlySelectedEnemyProvince != null) {
      featureLayer_provinces.unselectFeature(currentlySelectedEnemyProvince);
      currentlySelectedEnemyProvince = null;
    }
    if (currentlySelectedHumanProvince != null) {
      featureLayer_provinces.unselectFeature(currentlySelectedHumanProvince);
      currentlySelectedHumanProvince = null;
    }

  }

  private Map<String, String> getProvinceToOwningFactionMap() throws IOException {
    String content = Files.readString(Paths.get("src/unsw/gloriaromanus/initial_province_ownership.json"));
    JSONObject ownership = new JSONObject(content);
    Map<String, String> m = new HashMap<String, String>();
    for (String key : ownership.keySet()) {
      // key will be the faction name
      JSONArray ja = ownership.getJSONArray(key);
      // value is province name
      for (int i = 0; i < ja.length(); i++) {
        String value = ja.getString(i);
        m.put(value, key);
      }
    }
    return m;
  }

  private ArrayList<String> getHumanProvincesList() throws IOException {
    // https://developers.arcgis.com/labs/java/query-a-feature-layer/

    String content = Files.readString(Paths.get("src/unsw/gloriaromanus/initial_province_ownership.json"));
    JSONObject ownership = new JSONObject(content);
    return ArrayUtil.convert(ownership.getJSONArray(humanFaction));
  }

  /**
   * returns query for arcgis to get features representing human provinces can
   * apply this to FeatureTable.queryFeaturesAsync() pass string to
   * QueryParameters.setWhereClause() as the query string
   */
  private String getHumanProvincesQuery() throws IOException {
    LinkedList<String> l = new LinkedList<String>();
    for (String hp : getHumanProvincesList()) {
      l.add("name='" + hp + "'");
    }
    return "(" + String.join(" OR ", l) + ")";
  }

  private boolean confirmIfProvincesConnected(String province1, String province2) throws IOException {
    String content = Files
        .readString(Paths.get("src/unsw/gloriaromanus/province_adjacency_matrix_fully_connected.json"));
    JSONObject provinceAdjacencyMatrix = new JSONObject(content);
    return provinceAdjacencyMatrix.getJSONObject(province1).getBoolean(province2);
  }

  private void resetSelections() {
    featureLayer_provinces
        .unselectFeatures(Arrays.asList(currentlySelectedEnemyProvince, currentlySelectedHumanProvince));
    currentlySelectedEnemyProvince = null;
    currentlySelectedHumanProvince = null;
    if (controllerParentPairs.get(0).getKey() instanceof InvasionMenuController) {
      ((InvasionMenuController) controllerParentPairs.get(0).getKey()).setInvadingProvince("");
      ((InvasionMenuController) controllerParentPairs.get(0).getKey()).setOpponentProvince("");
    }
  }

  private void printMessageToTerminal(String message) {
    if (controllerParentPairs.get(0).getKey() instanceof InvasionMenuController) {
      ((InvasionMenuController) controllerParentPairs.get(0).getKey()).appendToTerminal(message);
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {
    if (mapView != null) {
      mapView.dispose();
    }
  }

  // test commend
  public void switchMenu() throws JsonParseException, JsonMappingException, IOException {
    System.out.println("trying to switch menu");
    stackPaneMain.getChildren().remove(controllerParentPairs.get(0).getValue());
    Collections.reverse(controllerParentPairs);
    stackPaneMain.getChildren().add(controllerParentPairs.get(0).getValue());
  }

  public void nextMenu(String current, String next) throws JsonParseException, JsonMappingException, IOException {
    currentMenu = next;
    MenuController mcr = menusList.get(current);
    MenuController mca = menusList.get(next);
    int indexRemove = 0;
    int indexAdd = 0;
    for (int i = 0; i < controllerParentPairs.size(); i++) {

      if (controllerParentPairs.get(i).getKey().equals(mcr)) {
        indexRemove = i;

      }
      if (controllerParentPairs.get(i).getKey().equals(mca))
        indexAdd = i;
    }

    stackPaneMain.getChildren().removeAll(controllerParentPairs.get(indexRemove).getValue(),
        controllerParentPairs.get(1).getValue());


    stackPaneMain.getChildren().addAll(controllerParentPairs.get(indexAdd).getValue(),
        controllerParentPairs.get(1).getValue());

  }

  /**
   * register user and add it to the database
   * 
   * @param user_name
   * @param faction
   */

  public void registerUser(String user_name, String faction) {

    Player p = db.addNewPlayer(user_name, faction);
    if (p == null)
      ((SignupPaneController) controllerParentPairs.get(0).getKey()).appendToTerminal("invalid user name or faction");
    else
      ((SignupPaneController) controllerParentPairs.get(0).getKey()).appendToTerminal("successfully joined");

  }

  /**
   * starting the game and assigning the current player of the game
   */
  public void startGame() throws IOException {
    // TODO: add UI feature for this event handler
    if (db.startGame().equals("start")) {
      nextMenu("unsw.gloriaromanus.SignupPaneController", "unsw.gloriaromanus.ActionController");
      player = db.getCurrentPlayer();
      humanFaction = player.getFaction().getName();
      ((SignupPaneController) controllerParentPairs.get(0).getKey()).appendToTerminal("successfully started the game");
      subscribe();
      status.setName(player.getUsername());
      status.setYear(db.getGameYear());

    } else
      ((SignupPaneController) controllerParentPairs.get(0).getKey()).appendToTerminal(db.startGame());
  }

  private Observer observer;
  private FactionObserver factionObserver;

  /**
   * subscribing to provinces that the players are playing with
   */
  public void subscribe() throws JsonParseException, JsonMappingException, IOException {
    observer = (province) -> {
      provinceToNumberTroopsMap.put(province.getName(), province.getNTroops());
      addAllPointGraphics();
    };

    factionObserver = (faction) -> {
      List<Province> pro = faction.getProvinces();
      for (Province p : pro) {
        provinceToOwningFactionMap.put(p.getName(), faction.getName());

      }
      addAllPointGraphics();

    };
    for (Player p : db.getPlayers()) {
      p.getFaction().subscribe(factionObserver);
      for (Province pro : p.getFaction().getProvinces()) {
        pro.subscribe(observer);
      }
    }
  }

  /**
   * given the unit, train the unit for selected province TODO: price
   * implementation TODO: fix the messages for display --> UI
   * 
   * @param unit
   */
  public void trainUnit(String unit) throws IOException {

    String humanProvince = (String) currentlySelectedHumanProvince.getAttributes().get("name");
    player.selectProvince(humanProvince);
    if (player.trainUnit(unit) == -1)
      System.out.println("could not add the unit you alraedy have two units training");
    else
      System.out.println("successfull! currently training the units they will be available from the next round");

  }

  /**
   * player finishing their turn
   */

  public void endTurn() throws JsonParseException, JsonMappingException, IOException {
    System.out.println(player.getUsername());
    player.endTurn();
    player = db.getCurrentPlayer();
    humanFaction = player.getFaction().getName();
    status.setName(player.getUsername());
    status.setYear(db.getGameYear());
    System.out.println(db.getGameYear());
  }

  public String setName() {
    return player.getUsername();
  }

  public List<String> getAvailableUnit(String p) {
    List<Unit> u;
    List<String> name = new ArrayList<String>();
    for (Province pp : player.getFaction().getProvinces()) {
      if (pp.getName().equals(p)) {
        u = pp.getUnits();
        for (Unit unit : u) {
          name.add(unit.getName());
        }
      }
    }
    return name;
  }
  public void saveGame() throws IOException {
    System.out.println("you saved game");
    db.saveGame();
    JSONObject jo = new JSONObject();

    jo.put("load",true);
        try (FileWriter file = new FileWriter("src/unsw/gloriaromanus/load.json")) {
 
            file.write(jo.toString());
            file.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
  }
}
