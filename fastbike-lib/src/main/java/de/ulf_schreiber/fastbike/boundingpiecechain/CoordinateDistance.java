package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;

abstract public class CoordinateDistance<
        V extends CoordinateDistance<V,R,W,G,M,B,L,A>,
        R extends CoordinateDistance.Reading<R> & Coordinate.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R> & Coordinate.Writing<R,W>,
        G extends CoordinateDistance.Grouping<R,G> & Coordinate.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G> & Coordinate.Merging<R,G,M>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
        > extends Coordinate<V,R,W,G,M,B,L,A>{

    public CoordinateDistance(int blocksize, double precision) {
        super(blocksize, precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends Coordinate.Reading<R>, Read {
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, Coordinate.Writing<R,W> {
        void setDistance(double distance);
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends Coordinate.Grouping<R,G>, Group {
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, Coordinate.Merging<R,G,M> {
        void setDistance(double distance);
    }
    public interface Read extends Coordinate.Read {
        double getDistance();

        class Immutable extends Coordinate.Read.Immutable implements Read {
            private final double distance;
            public Immutable(double lat, double lng, double distance) {
                super(lat, lng);
                this.distance = distance;
            }
            @Override public double getDistance() {
                return distance;
            }
        }
    }
    public interface Group extends Coordinate.Group {
        double getDistance();
    }


    @Override void copy(R from, W to) {
        to.setDistance(from.getDistance());
        super.copy(from, to);
    }

    @Override W clearWrite(W toClear) {
        toClear.setDistance(0d);
        return super.clearWrite(toClear);
    }

    @Override M clearMerge(M toClear) {
        toClear.setDistance(0d);
        return super.clearMerge(toClear);
    }

    @Override boolean sameAs(R one, R other) {
        return Math.abs(one.getDistance() - other.getDistance()) < precision && super.sameAs(one, other);
    }

    /** return rest > 0 one.getDistance < distance or 0*/
    public double restDistance(R one, double distance) {
        double rest = distance - one.getDistance();
        if(rest<precision) return 0d;
        return rest;
    }
    /** return rest > 0 one.getDistance < distance or 0*/
    public double restDistance(G one, double distance) {
        double rest = distance - one.getDistance();
        if(rest<precision) return 0d;
        return rest;
    }


    @Override void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);

        toExtend.setDistance(toExtend.getDistance() + point.getDistance());
    }

    @Override void extendBy(M toExtend, G other) {
        super.extendBy(toExtend, other);

        toExtend.setDistance(toExtend.getDistance() + other.getDistance());
    }


//    protected abstract class ElementLooking<L extends Looking<L> & Writing<R,W>> extends Coordinate<R,W,G,M>.ElementLooking<L> implements Writing<R,W>{
//        private final int skip;
//        protected ElementLooking(int subsize, int fieldLimit) {
//            super(subsize + 4, fieldLimit);
//            skip = weight - subsize;
//        }
//
//        @Override public double getDistance() {
//            return buffer.getDouble(actualIndex + Coordinate.ElementLooking.levellen);
//        }
//
//        @Override public void setDistance(double distance) {
//            buffer.putDouble(actualIndex + Coordinate.ElementLooking.levellen, distance);
//        }
//    }

    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R, W>,
            V extends Varing<R,W,V>
        > extends Coordinate.Varing<R,W,V> implements Writing<R,W> {
        private double distance = 0d;

        @Override
        public final double getDistance() {
            return distance;
        }

        @Override
        public final void setDistance(double distance) {
            this.distance=distance;
        }
    }

    protected abstract static class VaringAggregate<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>,
            A extends VaringAggregate<R, G, M, A> & Merging<R,G,M>
        > extends Coordinate.VaringAggregate<R, G, M, A> implements Merging<R,G,M> {
        private double distance = 0d;
        @Override public double getDistance() {
            return distance;
        }

        @Override public void setDistance(double distance) {
            this.distance=distance;
        }
    }

    @Override
    protected void interpolate(R from, R to, double fraction, W result) {
        result.setDistance(to.getDistance()*fraction);
        {
            double fVal = from.getLat();
            result.setLat(fVal + (to.getLat() - fVal) * fraction);
        }
        {
            double fVal = from.getLng();
            result.setLng(fVal + (to.getLng() - fVal) * fraction);
        }
        super.interpolate(from, to, fraction, result);
    }

    @Override
    void stringifyPoint(Appendable sw, R point) throws IOException {
        if(point==null) {
            sw.append("null");
        }else{
            sw.append('[');
            stringifyDouble(sw, point.getLat());
            sw.append(':');
            stringifyDouble(sw, point.getLng());
            sw.append('(');
            stringifyDouble(sw, point.getDistance());
            sw.append(")]");
        }
    }
}
