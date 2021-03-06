package unsw.gloriaromanus;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ActionController extends MenuController {
    @FXML
    public void trainUnit() throws JsonParseException, JsonMappingException, IOException {
        getParent().nextMenu("unsw.gloriaromanus.ActionController", "unsw.gloriaromanus.getUnitController");

    }
    @FXML
    public void moveTroop() throws JsonParseException, JsonMappingException, IOException{
        
        getParent().nextMenu("unsw.gloriaromanus.ActionController", "unsw.gloriaromanus.moveMenuController");
    }
    @FXML
    public void invade() throws JsonParseException, JsonMappingException, IOException {
        getParent().nextMenu("unsw.gloriaromanus.ActionController", "unsw.gloriaromanus.InvasionMenuController");
    }
   
}
