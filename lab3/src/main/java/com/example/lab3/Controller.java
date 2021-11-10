package com.example.lab3;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javafx.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

public class Controller {
    private final Map<String, Pair<Double, Double>> positions = new HashMap<>();
    private final Map<String, Pair<Double, Double>> placeChords = new HashMap<>();
    private final Map<String, String> placeDescription = new HashMap<>();

    @FXML
    private TextArea description;

    @FXML
    private ListView<String> listPlaces = new ListView<>();

    @FXML
    private ListView<String> listPositions = new ListView<>();

    @FXML
    private TextField localTime;

    @FXML
    private TextField place;

    @FXML
    private Button search;

    @FXML
    private TextField wheather;

    @FXML
    private void initialize() {
        search.setOnAction(event -> {
            String getUserPlace = place.getText().trim();
            String output = getUrlContent("https://graphhopper.com/api/1/geocode?q=" + getUserPlace +
                    "&locale=en&limit=10&debug=true&key=fcfda8b5-e4a2-4e83-9887-e566d4d04ac7");

            listPositions.getItems().clear();
            positions.clear();

            if(!output.isEmpty()) {
                JSONArray obj = new JSONArray(new JSONObject(output).getJSONArray("hits"));
                for (var el: obj){
                    JSONObject curObj = new JSONObject(el.toString());
                    String pos = curObj.getString("name") + ", " + curObj.getString("country");
                    positions.put(pos, new Pair<>(curObj.getJSONObject("point").getDouble("lng"), curObj.getJSONObject("point").getDouble("lat")));
                }
                for (Map.Entry<String, Pair<Double, Double>> el: positions.entrySet()) {
                    listPositions.getItems().add(el.getKey());
                }
            }
            else {
                listPositions.getItems().add("Ничего не найдено(");
            }
        });
    }

    @FXML
    private void displaySelectedPlaces() {
        String cur = listPositions.getSelectionModel().getSelectedItem();

        listPlaces.getItems().clear();
        listPlaces.refresh();

        if (cur == null || cur.isEmpty()) {
            listPlaces.getItems().add("Ничего не выбрано");
        }
        else {
            Pair<Double, Double> chords = positions.get(cur);
            String output = getUrlContent("https://api.opentripmap.com/0.1/ru/places/radius?radius=5000&lon=" + chords.getKey().toString() +
                    "&lat=" + chords.getValue().toString() +
                    "&format=json&limit=100&apikey=5ae2e3f221c38a28845f05b61237c8a774ce9cbca2f9348768ce577d");

            placeChords.clear();
            placeDescription.clear();
            JSONArray obj;
            if(!output.isEmpty() && (obj = new JSONArray(output)).length() != 0) {
                for (var el: obj){
                    JSONObject curObj = new JSONObject(el.toString());
                    String name = curObj.getString("name");
                    if (name.equals(""))
                        continue;
                    String xid = curObj.getString("xid");
                    Pair<Double, Double> chord = new Pair<>(curObj.getJSONObject("point").getDouble("lon"), curObj.getJSONObject("point").getDouble("lat"));
                    placeDescription.put(name, xid);
                    placeChords.put(name, chord);
                }
                for (Map.Entry<String, String> el: placeDescription.entrySet()) {
                    listPlaces.getItems().add(el.getKey());
                }
            }
            else {
                listPlaces.getItems().add("Ничего не найдено(");
            }
        }
    }

    @FXML
    private void displayDescriptions() {
        String cur = listPlaces.getSelectionModel().getSelectedItem();

        description.clear();
        localTime.clear();
        wheather.clear();
        localTime.setText("Не могу найти");

        if (cur == null || cur.isEmpty()) {
            description.setText("Не могу найти");
            wheather.setText("Не могу найти");
        }
        else {
            Pair<Double, Double> chords = placeChords.get(cur);
            String outputDescription = getUrlContent("https://api.opentripmap.com/0.1/ru/places/xid/" + placeDescription.get(cur) +
                    "?apikey=5ae2e3f221c38a28845f05b61237c8a774ce9cbca2f9348768ce577d");

            String outputWheather = getUrlContent("https://api.openweathermap.org/data/2.5/weather?lat=" + chords.getValue().toString() +
                    "&lon=" + chords.getKey().toString() +
                    "&appid=6e8861404cb3cf5d50947e4ee2b77ac5");

            if (!outputWheather.isEmpty()) {
                JSONObject obj = new JSONObject(outputWheather);
                double temp = Math.round((obj.getJSONObject("main").getDouble("temp") - 273.15) * 100) / 100.0;
                wheather.setText(temp + "`C");
            }
            else wheather.setText("Не могу найти");

            if (!outputDescription.isEmpty()){
                JSONObject obj = new JSONObject(outputDescription);
                try {
                    description.setText(obj.getJSONObject("info").getString("descr"));
                } catch (Exception e) {
                    description.setText("Не могу найти");
                }
            }
            else description.setText("Не могу найти");

        }
    }

    private static String getUrlContent(String urlAddress) {
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(urlAddress);
            URLConnection urlConnection = url.openConnection();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) content.append(line).append("\n");
            bufferedReader.close();
        } catch (Exception e) {
            System.err.println("Ошибка url:" + urlAddress);
        }
        return content.toString();
    }
}
