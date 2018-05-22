package de.ulf_schreiber.fastbike.core;

public interface BrouterClient {
    Cancellable call(double fromLat, double fromLon, double toLat, double toLon, OnRoute resultHandler);


    interface Cancellable {
        void cancel();
    }

    interface OnRoute {
        void fail(String why);
        void success(String gpx);
    }
}
