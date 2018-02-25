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


    public class SearchResult {

        SearchResult() {
        }

        void miss() {

        }

        void hit(R read) {

        }
    }
    public SearchResult search(R northWest, R southEast){
        SearchResult ret = new SearchResult();

        M searchBox = createMutableBounds();
        extendBy(searchBox, northWest);
        extendBy(searchBox, southEast);

        W middle = createMutableVal();
        middle.setLat((northWest.getLat()+southEast.getLat())/2);
        middle.setLon((northWest.getLon()+southEast.getLon())/2);

        LinePointFinder lpf = new LinePointFinder(middle, northWest, southEast);


        R lastPoint = null;
        for(Piece piece : pieces){
            if(overlaps(piece.bounds, searchBox.read())) {
                searchNode(piece.offset, piece.bounds.getDistance(), piece.root, searchBox.read(), middle.read(), createAggregateWriter(), createElementWriter(), ret, lastPoint, lpf);
            }
            lastPoint = piece.lastPoint;
        }

        return ret;
    }

    class LinePointFinder {
        private final R center;
        private final M bounds;
        private final M tempBounds;

        LinePointFinder(W center, R northWest, R southEast){
            this.center = center.read();
            this.bounds = createMutableBounds();
            extendBy(bounds, northWest);
            extendBy(bounds, southEast);

            tempBounds = createMutableBounds();

        }


        void find(R from, R to, SearchResult result) {

            if(true)throw new RuntimeException("TODO");

            result.miss();
        }
    }

    private R searchNode(double skip, double length,
                         Node<?> node, G searchBox, R middle,
                         A groupReader, L elementReader,
                         SearchResult result, R lastPoint, LinePointFinder lpf){
        if(node.depth()>0){
            GroupNode group = GroupNode.class.cast(node);
            G read = groupReader.wrap(group.array).read();
            for(int i=0;i<group.elements && length>precision;i++){
                groupReader.moveAbsolute(i);

                double distance = read.getDistance();

                if(distance <skip+precision){
                    skip-= distance;
                }else{
                    if(overlaps(read, searchBox)){
                        lastPoint = searchNode(skip, length, group.children[i], searchBox, middle, groupReader, elementReader, result, lastPoint, lpf);
                        groupReader.wrap(group.array);
                    }else{
                        result.miss();
                        lastPoint = group.getLastPoint();
                    }
                    length-= distance-skip;
                    skip=0;
                }
            }
        }else{
            LeafNode leaf = LeafNode.class.cast(node);
            R read = elementReader.wrap(leaf.array).read();
            for(int i=0;i<leaf.elements && length>precision;i++) {
                elementReader.moveAbsolute(i);


                double distance = read.getDistance();

                if(distance - precision < skip){
                    skip-= distance;
                }else{
                    if(lastPoint==null || read.getDistance()<precision){
                        if(contains(searchBox, read)){
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
