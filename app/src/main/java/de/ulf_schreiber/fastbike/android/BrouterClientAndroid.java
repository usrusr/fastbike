package de.ulf_schreiber.fastbike.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;
import btools.routingapp.IBRouterService;
import de.ulf_schreiber.fastbike.app.R;
import de.ulf_schreiber.fastbike.boundingpiecechain.Route;
import de.ulf_schreiber.fastbike.core.BrouterClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrouterClientAndroid implements BrouterClient {


    int maxRunningTime = 600;
    String v = "bicycle";
    String fast = "1";


    private final Context context;
    private final ServiceConnection serviceConnection;

    private IBRouterService service;
    boolean isBound = false;

    public BrouterClientAndroid(final Context context) {
        this.context = context;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onBindingDied(ComponentName name) {
                System.out.println("onBindingDied: "+name);
                service = null;
                Toast.makeText(context, R.string.brouter_service_died, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                System.out.println("onServiceConnected: "+componentName);
                service = IBRouterService.Stub.asInterface(iBinder);
                Toast.makeText(context, R.string.brouter_service_connected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                System.out.println("onServiceDisconnected: "+componentName);
                service = null;
                Toast.makeText(context, R.string.brouter_service_gone, Toast.LENGTH_SHORT).show();
            }
        };
        bind();
    }

    @Override
    public Cancellable call(double fromLat, double fromLon, double toLat, double toLon, final OnRoute resultHandler){

        bind();

        if( ! isBound) {
            resultHandler.fail(new IllegalStateException("not bound"));
            return null;
        }

        Bundle bundle = new Bundle();
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

        bundle.putString("maxRunningTime", ""+maxRunningTime);
        bundle.putDoubleArray("lats", new double[]{fromLat, toLat});
        bundle.putDoubleArray("lons", new double[]{fromLon, toLon});
        bundle.putString("fast", ""+fast);
        bundle.putString("v", ""+v);

//        bundle.putString("alternativeidx", "1");
        bundle.putString("turnInstructionFormat", "osmand");
//        bundle.putString("trackFormat", "kml");

        RouteTask routeTask = new RouteTask(resultHandler);
        routeTask.execute(bundle);
        return routeTask;
    }

    private void bind() {
        if( ! isBound) {
            Intent intent = new Intent(context, IBRouterService.class);
//            intent.setAction(IBRouterService.class.getName());
//            intent.setClassName(IBRouterService.class.getPackage().getName(), IBRouterService.class.getName());
            intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");
            boolean b = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            System.out.println("bindService: "+b);
            if(b){
                isBound=true;
            }
        }
    }


    private class RouteTask extends AsyncTask<Bundle, Void, Map.Entry<Iterable<Route.Reading>, Throwable>> implements Cancellable{

        private final OnRoute resultHandler;

        public RouteTask(OnRoute resultHandler) {
            this.resultHandler = resultHandler;
        }

        @Override
        protected void onPostExecute(Map.Entry<Iterable<Route.Reading>, Throwable> gpxOrFail) {
            if(gpxOrFail.getKey()!=null){

                Route route = resultHandler.getRoute();

                resultHandler.success(gpxOrFail.getKey());
            }else{
                resultHandler.fail(new RuntimeException(gpxOrFail.getValue()));
            }
        }

        @Override
        protected Map.Entry<Iterable<Route.Reading>, Throwable> doInBackground(Bundle... voids) {
            Bundle bundle = voids[0];
            try {
                String trackFromParams = service.getTrackFromParams(bundle);
                Route route = resultHandler.getRoute();
                List<Route.Reading> list = new ArrayList<>();

                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(new StringReader(trackFromParams));


                boolean isTrkpt = false;
                boolean isElev = false;
                double ele = 0;
                double lat = Double.NaN;
                double lon = Double.NaN;
                double lastLat = Double.NaN;
                double lastLon = Double.NaN;
                double lastEle = Double.NaN;
                StringBuilder sb = new StringBuilder();
                int event;
                while((event = parser.next()) != XmlPullParser.END_DOCUMENT){
                    if (event == XmlPullParser.START_TAG) {
                        if (isTrkpt) {
                            if ("ele".equals(parser.getName())) {
                                isElev = true;
                                sb.setLength(0);
                            }
                        } else if ("trkpt".equals(parser.getName())) {
                            isTrkpt = true;
                            lon = Double.parseDouble(parser.getAttributeValue(null, "lon"));
                            lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
                            sb.setLength(0);
                        }
                    } else if (event == XmlPullParser.END_TAG) {
                        if (isElev && "ele".equals(parser.getName())) {
                            ele = Double.parseDouble(sb.toString());
                            isElev = false;
                        } else if (isTrkpt && "trkpt".equals(parser.getName())) {


                            double distance;
                            if(Double.isNaN(lastLat)){
                                distance = 0;
                            } else {
                                distance = route.calculateDistance(lat, lon, ele, lastLat, lastLon, lastEle);
                            }
                            Route.Reading val = route.immutable(lat, lon, distance, ele);

                            list.add(val);
                        }
                    } else if (event == XmlPullParser.TEXT) {
                        if (isElev) {
                            sb.append(parser.getText().trim());
                        }
                    }
                }



                return new AbstractMap.SimpleImmutableEntry<Iterable<Route.Reading>, Throwable>(list, null);
            } catch (RemoteException | XmlPullParserException | IOException e) {
                e.printStackTrace();
                return new AbstractMap.SimpleImmutableEntry<Iterable<Route.Reading>, Throwable>(null, e);
            }

        }

        @Override
        public void cancel() {
            this.cancel(true);
        }
    }
}
