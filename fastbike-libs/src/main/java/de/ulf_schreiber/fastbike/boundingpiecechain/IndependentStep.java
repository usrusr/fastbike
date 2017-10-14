package de.ulf_schreiber.fastbike.boundingpiecechain;

public class IndependentStep implements Step.Interface, Step.Writers<IndependentStep, Step.Interface>{
    private double dist;
    private double lat;
    private double lng;

    @Override
    public double getLat() {
        return lat;
    }

    @Override
    public double getLng() {
        return lng;
    }

    @Override
    public boolean sameAs(Step.Interface other, double precision) {
        return false;
    }

    @Override
    public double getDist() {
        return dist;
    }

    @Override
    public void setDist(double dist) {
        this.dist = dist;
    }

    @Override
    public void copyFrom(Step.Interface from) {
        setDist(from.getDist());
        setLatLng(from.getLat(), from.getLng());
    }

    @Override
    public void setLatLng(double lat, double lng) {

        this.lat = lat;
        this.lng = lng;
    }
}
