package gov.nasa.worldwindx.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Material;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RealTimeFlightPaths extends ApplicationTemplate {

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        private RenderableLayer layer;
        private ObjectMapper objectMapper;

        public AppFrame() {
            super(true, false, false);
            this.layer = new RenderableLayer();
            this.objectMapper = new ObjectMapper();
            insertBeforeCompass(getWwd(), layer);
            Timer timer = new Timer(5000, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    fetchFlightData();
                }
            });
            timer.start();
        }

        private void fetchFlightData() {
            try {
                URL url = new URL("https://opensky-network.org/api/states/all?lamin=33.0&lomin=124.0&lamax=39.0&lomax=132.0");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                int responsecode = conn.getResponseCode();

                if (responsecode != 200) {
                    throw new RuntimeException("HttpResponseCode: " + responsecode);
                } else {
                    Scanner scanner = new Scanner(url.openStream());
                    String inline = "";
                    while (scanner.hasNext()) {
                        inline += scanner.nextLine();
                    }
                    scanner.close();
                    updateFlights(inline);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void updateFlights(String jsonResponse) {
            try {
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode states = rootNode.path("states");
                layer.removeAllRenderables();

                for (JsonNode state : states) {
                    double lat = state.get(6).asDouble();
                    double lon = state.get(5).asDouble();
                    if (lat != 0 && lon != 0) {
                        Position position = Position.fromDegrees(lat, lon, 10000); // altitude is set to 10,000 meters for all planes
                        PointPlacemark placemark = new PointPlacemark(position);
                        placemark.setLabelText(state.get(1).asText()); // callsign
                        placemark.setValue(AVKey.DISPLAY_NAME, state.get(1).asText());
                        PointPlacemarkAttributes attrs = new PointPlacemarkAttributes();
                        attrs.setImageAddress("airplane.png"); // airplane icon
                        attrs.setImageColor(new Color(1f, 1f, 1f, 0.6f));
                        attrs.setScale(0.6);
                        attrs.setLineMaterial(new Material(Color.YELLOW));
                        placemark.setAttributes(attrs);
                        layer.addRenderable(placemark);
                    }
                }
                this.getWwd().redraw();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ApplicationTemplate.start("Real-time Flight Paths", AppFrame.class);
    }
}
