package de.ulf_schreiber.fastbike.boundingpiecechain;

public interface Point<P extends Point.Implementation<P> & Point<P>>{
    double getLat();
    double getLng();

    boolean sameAs(P b, double precision);

    abstract class Implementation<P extends Implementation<P> & Point<P>> implements Point<P>  {
        abstract protected void setLatLngImpl(double lat, double lng); // package-external protected implementation package-visible setLatLng
        final void setLatLng(double lat, double lng){
            setLatLngImpl(lat, lng);
        }
        final void copyFrom(Point<?> other){
            setLatLng(other.getLat(),other.getLng());
        }

    }
}
