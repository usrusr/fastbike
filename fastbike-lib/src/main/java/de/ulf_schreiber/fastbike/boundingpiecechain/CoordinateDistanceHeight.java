package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;

abstract public class CoordinateDistanceHeight<
        V extends CoordinateDistanceHeight<V,R,W,G,M,B,L,A>,
        R extends CoordinateDistanceHeight.Reading<R> & CoordinateDistance.Reading<R>,
        W extends CoordinateDistanceHeight.Writing<R,W> & CoordinateDistanceHeight.Reading<R> & CoordinateDistance.Writing<R,W>,
        G extends CoordinateDistanceHeight.Grouping<R,G> & CoordinateDistance.Grouping<R,G>,
        M extends CoordinateDistanceHeight.Merging<R,G,M> & CoordinateDistanceHeight.Grouping<R,G> & CoordinateDistance.Merging<R,G,M>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
        > extends CoordinateDistance<V,R,W,G,M,B,L,A>{

    public CoordinateDistanceHeight(int blocksize, double precision) {
        super(blocksize, precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends CoordinateDistance.Reading<R>, Read {
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, CoordinateDistance.Writing<R,W> {
        void setHeight(double height);
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends CoordinateDistance.Grouping<R,G>, Group {
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, CoordinateDistance.Merging<R,G,M> {
        void setClimb(double climb);
        void setLastHeight(double lastHeight);
    }
    public interface Read extends CoordinateDistance.Read {
        double getHeight();
        class Immutable extends CoordinateDistance.Read.Immutable implements Read {
            private final double height;
            public Immutable(double lat, double lng, double distance, double height) {
                super(lat, lng, distance);
                this.height = height;
            }
            @Override public double getHeight() {
                return height;
            }
        }
    }
    public interface Group extends CoordinateDistance.Group {
        double getClimb();
        double getLastHeight();
    }


    @Override void copy(R from, W to) {
        to.setHeight(from.getHeight());
        super.copy(from, to);
    }

    @Override W clearWrite(W toClear) {
        toClear.setHeight(0d);
        return super.clearWrite(toClear);
    }

    @Override M clearMerge(M toClear) {
        toClear.setClimb(0d);
        toClear.setLastHeight(Double.NaN);
        return super.clearMerge(toClear);
    }

    @Override boolean sameAs(R one, R other) {
        return
//                Math.abs(one.getHeight() - other.getHeight()) < precision &&
                        super.sameAs(one, other);
    }

    @Override void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);
        double height = point.getHeight();
        if( ! Double.isNaN(height)) {
            double lastHeight = toExtend.getLastHeight();
            if (!Double.isNaN(lastHeight) && lastHeight < height) {
                toExtend.setClimb(toExtend.getClimb() + (height - lastHeight));
            }
            toExtend.setLastHeight(height);
        }
    }

    @Override void extendBy(M toExtend, G other) {
        super.extendBy(toExtend, other);

        toExtend.setClimb(toExtend.getClimb() + other.getClimb());
        toExtend.setLastHeight(other.getLastHeight());
    }


//    protected abstract class ElementLooking<L extends Looking<L> & Writing<R,W>> extends CoordinateDistance<R,W,G,M>.ElementLooking<L> implements Writing<R,W>{
//        private final int skip;
//        protected ElementLooking(int subsize, int fieldLimit) {
//            super(subsize + 4, fieldLimit);
//            skip = weight - subsize;
//        }
//
//        @Override public double getDistance() {
//            return buffer.getDouble(actualIndex + CoordinateDistance.ElementLooking.levellen);
//        }
//
//        @Override public void setDistance(double distance) {
//            buffer.putDouble(actualIndex + CoordinateDistance.ElementLooking.levellen, distance);
//        }
//    }

    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R, W>,
            V extends Varing<R,W,V>
        > extends CoordinateDistance.Varing<R,W,V> implements Writing<R,W> {
        private double height = Double.NaN;

        @Override
        public final double getHeight() {
            return height;
        }

        @Override
        public final void setHeight(double height) {
            this.height=height;
        }
    }

    protected abstract static class VaringAggregate<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>,
            A extends VaringAggregate<R, G, M, A> & Merging<R,G,M>
        > extends CoordinateDistance.VaringAggregate<R, G, M, A> implements Merging<R,G,M> {
        private double lastHeight = Double.NaN;
        private double climb = 0d;

        @Override public double getLastHeight() {
            return lastHeight;
        }

        @Override public void setLastHeight(double lastHeight) {
            this.lastHeight = lastHeight;
        }

        @Override public double getClimb() {
            return climb;
        }

        @Override public void setClimb(double climb) {
            this.climb = climb;
        }
    }

    @Override
    protected void interpolate(R from, R to, double fraction, W result) {
        super.interpolate(from, to, fraction, result);
        result.setHeight(to.getHeight()*fraction);
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
            sw.append('^');
            stringifyDouble(sw, point.getHeight());
            sw.append(")]");
        }
    }
}
