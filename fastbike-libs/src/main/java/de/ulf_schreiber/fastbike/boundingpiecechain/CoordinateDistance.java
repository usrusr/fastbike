package de.ulf_schreiber.fastbike.boundingpiecechain;

abstract public class CoordinateDistance<
        R extends CoordinateDistance.Reading<R> & Coordinate.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R> & Coordinate.Writing<R,W>,
        G extends CoordinateDistance.Grouping<R,G> & Coordinate.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G> & Coordinate.Merging<R,G,M>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>
        > extends Coordinate<R,W,G,M,B,L,A>{

    public CoordinateDistance(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends Coordinate.Reading<R> {
        double getDistance();
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
            > extends Coordinate.Grouping<R,G> {
        double getDistance();
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, Coordinate.Merging<R,G,M> {
        void setDistance(double distance);
    }
    public interface PublicRead extends Reading<PublicRead> {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup> {

    }


    @Override
    public void copy(R from, W to) {
        to.setDistance(from.getDistance());
        super.copy(from, to);
    }

    @Override
    public boolean sameAs(R one, R other) {
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


    @Override
    public void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);

        toExtend.setDistance(toExtend.getDistance() + point.getDistance());
    }

    @Override
    public void extendBy(M toExtend, G other) {
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
        private double distance;

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
        private double distance;
        @Override public double getDistance() {
            return distance;
        }

        @Override public void setDistance(double distance) {
            this.distance=distance;
        }
    }


}
