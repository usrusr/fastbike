package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;


/**
 * height is not aggregated into climb because getting the correct previous node height would be prohibitively bug-prone and the utility is questionable
 *
 */
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

    protected interface Reading <
            R extends Reading<R>
            > extends CoordinateDistance.Reading<R> {
        double getHeight();
    }
    protected interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, CoordinateDistance.Writing<R,W> {
        void setHeight(double height);
    }
    protected interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends CoordinateDistance.Grouping<R,G> {
    }
    protected interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, CoordinateDistance.Merging<R,G,M> {
    }


    @Override void copy(R from, W to) {
        to.setHeight(from.getHeight());
        super.copy(from, to);
    }

    @Override W clearWrite(W toClear) {
        toClear.setHeight(0d);
        return super.clearWrite(toClear);
    }



    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R,W>,
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
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>,
            A extends VaringAggregate<R,G,M,A> & Merging<R,G,M>
        > extends CoordinateDistance.VaringAggregate<R,G,M,A> implements Merging<R,G,M> {
    }

    @Override
    protected W interpolate(R from, R to, double fraction, W result) {
        super.interpolate(from, to, fraction, result);
        result.setHeight(to.getHeight()*fraction);
        return result;
    }

    @Override
    void stringifyPoint(Appendable sw, R point) throws IOException {
        if(point==null) {
            sw.append("null");
        }else{
            sw.append('[');
            stringifyDouble(sw, point.getLat());
            sw.append(':');
            stringifyDouble(sw, point.getLon());
            sw.append('(');
            stringifyDouble(sw, point.getDistance());
            sw.append('^');
            stringifyDouble(sw, point.getHeight());
            sw.append(")]");
        }
    }
}
