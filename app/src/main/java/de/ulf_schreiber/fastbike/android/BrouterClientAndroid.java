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
import de.ulf_schreiber.fastbike.core.BrouterClient;

import java.util.AbstractMap;
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
            resultHandler.fail("not bound");
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


    private class RouteTask extends AsyncTask<Bundle, Void, Map.Entry<String, String>> implements Cancellable{

        private final OnRoute resultHandler;

        public RouteTask(OnRoute resultHandler) {
            this.resultHandler = resultHandler;
        }

        @Override
        protected void onPostExecute(Map.Entry<String, String> gpxOrFail) {
            if(gpxOrFail.getKey()!=null){
                resultHandler.success(gpxOrFail.getKey());
            }else{
                resultHandler.fail(gpxOrFail.getValue());
            }
        }

        @Override
        protected Map.Entry<String, String> doInBackground(Bundle... voids) {
            Bundle bundle = voids[0];
            try {
                return new AbstractMap.SimpleImmutableEntry(service.getTrackFromParams(bundle), null);
            } catch (RemoteException e) {
                e.printStackTrace();
                return new AbstractMap.SimpleImmutableEntry(null, e.getMessage());
            }

        }

        @Override
        public void cancel() {
            this.cancel(true);
        }
    }
}
