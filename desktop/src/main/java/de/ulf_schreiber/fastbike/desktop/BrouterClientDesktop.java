package de.ulf_schreiber.fastbike.desktop;

import de.ulf_schreiber.fastbike.core.BrouterClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BrouterClientDesktop implements BrouterClient {


    private final String baseUrl;
    String profile = "fastbike";


    public BrouterClientDesktop(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Cancellable call(double fromLat, double fromLon, double toLat, double toLon, OnRoute resultHandler){
        //param params--> Map of params:
        //  "pathToFileResult"-->String with the path to where the result must be saved, including file name and extension
        //                    -->if null, the track is passed via the return argument
        //  "maxRunningTime"-->String with a number of seconds for the routing timeout, default = 60
        //  "turnInstructionFormat"-->String selecting the format for turn-instructions values: osmand, locus
        //  "trackFormat"-->[kml|gpx] default = gpx
        //  "lats"-->double[] array of latitudes; 2 values at least.
        //  "lons"-->double[] array of longitudes; 2 values at least.
        //  "nogoLats"-->double[] array of nogo latitudes; may be null.
        //  "nogoLons"-->double[] array of nogo longitudes; may be null.
        //  "nogoRadi"-->double[] array of nogo radius in meters; may be null.
        //  "fast"-->[0|1]
        //  "v"-->[motorcar|bicycle|foot]
        //  "remoteProfile"--> (String), net-content of a profile. If remoteProfile != null, v+fast are ignored
        //return null if all ok and no path given, the track if ok and path given, an error message if it was wrong
        //call in a background thread, heavy task!


        // ?lonlats=10.929337,49.531894|10.876465,49.545707&nogos=&profile=trekking&alternativeidx=0&format=geojson

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?lonlats=")
                .append(fromLon)
                .append(fromLat)
                .append("|")
                .append(toLon)
                .append(toLat)
        ;
        sb.append("&profile=").append(encode(profile));
        sb.append("&alternativeidx=").append(encode(""+1));
        sb.append("&format=gpx");

        return new RouteReq(sb.toString(), resultHandler);
    }

    private final static String UTF_8 = StandardCharsets.UTF_8.name();
    private String encode(String profile) {
        try {
            return URLEncoder.encode(profile, UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return profile;
        }
    }


    public static class RouteReq implements Cancellable {
        public RouteReq(String s, OnRoute resultHandler) {
            try {
                URL url = new URL(s);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancel() {

        }
    }
}
