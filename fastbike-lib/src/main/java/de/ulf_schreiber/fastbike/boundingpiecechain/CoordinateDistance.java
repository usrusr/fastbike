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

    protected interface Reading <
            R extends Reading<R>
            > extends Coordinate.Reading<R> {
        double getDistance();
    }
    protected interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, Coordinate.Writing<R,W> {
        void setDistance(double distance);
    }
    protected interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends Coordinate.Grouping<R,G> {
        double getDistance();

    }
    protected interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, Coordinate.Merging<R,G,M> {
        void setDistance(double distance);
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

    @Override void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);

        toExtend.setDistance(toExtend.getDistance() + point.getDistance());
    }

    @Override void extendBy(M toExtend, G other) {
        super.extendBy(toExtend, other);

        toExtend.setDistance(toExtend.getDistance() + other.getDistance());
    }


    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R,W>,
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
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>,
            A extends VaringAggregate<R,G,M,A> & Merging<R,G,M>
        > extends Coordinate.VaringAggregate<R,G,M,A> implements Merging<R,G,M> {
        private double distance = 0d;
        @Override public double getDistance() {
            return distance;
        }

        @Override public void setDistance(double distance) {
            this.distance=distance;
        }
    }

    @Override
    protected W interpolate(R from, R to, double fraction, W result) {
        result.setDistance(to.getDistance()*fraction);
        {
            double fVal = from.getLat();
            result.setLat(fVal + (to.getLat() - fVal) * fraction);
        }
        {
            double fVal = from.getLon();
            result.setLon(fVal + (to.getLon() - fVal) * fraction);
        }
        super.interpolate(from, to, fraction, result);
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
            sw.append(")]");
        }
    }


    public class SearchResultHandler {

        SearchResultHandler() {
        }

        void miss() {

        }

        void hit(R read) {

        }
    }
    public void search(R northWest, R southEast, SearchResultHandler resultHandler){

        M searchBox = createMutableBounds();
        extendBy(searchBox, northWest);
        extendBy(searchBox, southEast);

        W middle = createMutableVal();
        middle.setLat((northWest.getLat()+southEast.getLat())/2);
        middle.setLon((northWest.getLon()+southEast.getLon())/2);

        LinePointFinder lpf = new LinePointFinder(middle, northWest, southEast, resultHandler);


        R lastPoint = null;
        for(Piece piece : pieces){
            if(overlaps(piece.bounds, searchBox.read())) {
                searchNode(piece.offset, piece.bounds.getDistance(), piece.root, lpf);
            }
            lastPoint = piece.lastPoint;
        }
    }

    class LinePointFinder {
        private final R center;
        private final SearchResultHandler resultListener;
        private final M bounds;

        private final M tempBounds = createMutableBounds();
        private final W tempMutable = createMutableVal();

        private final A groupReader = createAggregateWriter();
        private final L elementReader = createElementWriter();

        private R lastPoint;
        private double done = 0d;

        LinePointFinder(W center, R northWest, R southEast, SearchResultHandler resultListener){
            this.center = center.read();
            this.resultListener = resultListener;
            this.bounds = createMutableBounds();
            extendBy(bounds, northWest);
            extendBy(bounds, southEast);

        }


        void find(R from, R to, SearchResultHandler result) {

            if(true)throw new RuntimeException("TODO");

            result.miss();
        }
    }

    private R searchNode(double skip, double length,
                         Node<?> node, LinePointFinder lpf){
        if(node.depth()>0){
            GroupNode group = GroupNode.class.cast(node);
            G read = lpf.groupReader.wrap(group.array).read();
            for(int i=0;i<group.elements && length>precision;i++){
                lpf.groupReader.moveAbsolute(i);

                double distance = read.getDistance();

                if(distance <skip+precision){
                    skip-= distance;
                }else{
                    if(overlaps(read, lpf.bounds.read())){
                        searchNode(skip, length, group.children[i], lpf);
                        lpf.groupReader.wrap(group.array);
                    }else{
                        lpf.lastPoint = group.getLastPoint();
                        lpf.done += distance;
                    }
                    length-= distance-skip;
                    skip=0;
                }
            }
        }else{
            LeafNode leaf = LeafNode.class.cast(node);
            R read = lpf.elementReader.wrap(leaf.array).read();
            for(int i=0;i<leaf.elements && length>precision;i++) {
                lpf.elementReader.moveAbsolute(i);


                double distance = read.getDistance();

                if(distance - precision < skip){
                    skip-= distance;
                }else{
                    if(lpf.lastPoint==null){

                    } else if(distance-skip < precision){
                        if(contains(lpf.bounds.read(), read)){
                            result.hit(read);
                        }else{
                            result.miss();
                        }
                    }else{
                        lpf.find(lastPoint, read, result);
                    }

                    length -= distance-skip;
                    skip=0;
                }

            }


        }
        return lastPoint;
    }

}
