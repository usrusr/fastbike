package de.ulf_schreiber.fastbike.boundingpiecechain;

public class Point {
    public interface Getters<R extends ClosedType & Getters<R>> {
        double getLat();
        double getLng();
        boolean sameAs(R other, double precision);
    }

    interface Writers<W extends Writers<W, T> & Getters<T>, T extends Getters<T> & ClosedType> {
        void copyFrom(T from);
        void setLatLng(double lat, double lng);
    }

    /** closes the selftype hierarchy */
    public interface Interface extends Getters<Interface>, ClosedType{}

    public abstract static class Base implements Interface {
        @Override public boolean sameAs(Interface other, double precision) {
            return Math.abs(getLng() - other.getLng()) < precision
                    && Math.abs(getLat() - other.getLat()) < precision
            ;
        }
    }

}

