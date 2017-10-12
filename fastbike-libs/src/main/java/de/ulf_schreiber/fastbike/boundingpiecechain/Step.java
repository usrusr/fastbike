package de.ulf_schreiber.fastbike.boundingpiecechain;


public interface Step<S extends Step.Implementation<S> & Step<S>> extends Point<S> {
    double getDist();

    abstract class Implementation<S extends Step.Implementation<S> & Step<S>> extends Point.Implementation<S> implements Step<S>{
        abstract protected void setLenImpl(double len); // package-external protected implementation package-visible setLatLng
        final void setLatLngLen(double lat, double lng, double len){
            setLatLngImpl(lat, lng);
            setLenImpl(len);
        }
        final void copyFrom(Step<?> other){
            setLatLng(other.getLat(),other.getLng());
            setLenImpl(other.getDist());
        }
    }
}
