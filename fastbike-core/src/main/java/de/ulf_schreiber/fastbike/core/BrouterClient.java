package de.ulf_schreiber.fastbike.core;

import de.ulf_schreiber.fastbike.boundingpiecechain.Route;

public interface BrouterClient {
    Cancellable call(double fromLat, double fromLon, double toLat, double toLon, OnRoute resultHandler);


    interface Cancellable {
        void cancel();
    }

    interface OnRoute {
        void fail(Throwable why);
        void success(Iterable<Route.Reading> gpx);
        Route getRoute();
    }
}
