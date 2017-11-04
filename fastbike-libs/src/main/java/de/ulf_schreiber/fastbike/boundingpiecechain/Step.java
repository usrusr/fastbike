package de.ulf_schreiber.fastbike.boundingpiecechain;


import de.ulf_schreiber.fastbike.boundingpiecechain.value.Value;

public class Step extends Point {
    public interface Getters<T extends Getters<T> & Value.CT>
            extends Point.Getters<T>{
        double getDist();
    }
    public interface Writers<W extends Writers<W,T> & Getters<T>, T extends Getters<T> & Value.CT>
            extends Point.Writers<W, T> {
        void setDist(double dist);
    }
    public interface Interface extends Getters<Interface>, Value.CT {}

    public static abstract class Base implements Interface {
        @Override public boolean sameAs(Interface other, double precision) {
            return Math.abs(getLng() - other.getLng()) < precision
                    && Math.abs(getLat() - other.getLat()) < precision
                    && Math.abs(getDist() - other.getDist()) < precision
                    ;
        }
    }
}
