package de.ulf_schreiber.fastbike.boundingpiecechain.value;


import java.util.Iterator;

public class SimpleTree extends Tree<
        SimpleTree.Meta,
        SimpleTree.Reading,
        SimpleTree.Writing,
        SimpleTree.Grouping,
        SimpleTree.Merging,
        double[],
        SimpleTree.ElementWriter,
        SimpleTree.AggregateWriter
> {

    public SimpleTree() {
        super(new Meta(0.00001d));
    }

    @Override LeafNode<Meta, Reading, Writing, Grouping, Merging, double[], ElementWriter, AggregateWriter> createLeafNode(Tree<Meta, Reading, Writing, Grouping, Merging, double[], ElementWriter, AggregateWriter> tree, Iterator<Reading> it, Value.Editor<ElementWriter, Reading, Writing, double[]> looker, Merging bounds) {
        return null;
    }

    @Override GroupNode<Meta, Reading, Writing, Grouping, Merging, double[], ElementWriter, AggregateWriter> createGroupNodeNode(Tree<Meta, Reading, Writing, Grouping, Merging, double[], ElementWriter, AggregateWriter> tree, Iterator<Reading> it, Value.Editor<ElementWriter, Reading, Writing, double[]> looker, Value.Editor<AggregateWriter, Grouping, Merging, double[]> groupLooker, Merging bounds, Node<Meta, Reading, Writing, Grouping, Merging, double[], ElementWriter, AggregateWriter, ?> firstChild, int depth) {
        return null;
    }

    static class Meta extends CoordinateDistance<Reading,Writing,Grouping,Merging>{
        public Meta(double precision) {
            super(precision);
        }
        @Override ElementWriter createElementWriter() {
            return null;
        }
        @Override AggregateWriter createAggregateWriter() {
            return null;
        }
        @Override SimpleTree.Merging createMutableBounds() {
            return null;
        }
    }
    interface Reading extends CoordinateDistance.Reading<Reading> {
    }
    interface Writing extends CoordinateDistance.Writing<Reading,Writing>, CoordinateDistance.Reading<Reading>{
    }
    interface Grouping extends CoordinateDistance.Grouping<Reading,Grouping>{
    }
    interface Merging extends CoordinateDistance.Merging<Reading,Grouping,Merging>, CoordinateDistance.Grouping<Reading,Grouping>{
    }

    static class MutableBounds extends CoordinateDistance.VaringAggregate<MutableBounds, Reading>{

    }

    static class ElementWriter
            extends Value.Editor<ElementWriter,Reading,Writing,double[]>
            implements Writing, Reading
    {
        /**
         * @param size       bytes per element (as determined by implementors)
         * @param fieldLimit
         */
        protected ElementWriter(int size, int fieldLimit) {
            super(size, fieldLimit);
        }

        @Override
        public double[] createBuffer(int blocksize) {
            return new double[blocksize * 3];
        }
        @Override public void setDistance(double distance) {
            buffer[2] = distance;
        }
        @Override public void setLat(double latitude) {
            buffer[0] = latitude;
        }
        @Override public void setLng(double longitude) {
            buffer[1] = longitude;
        }
        @Override public double getDistance() {
            return buffer[2];
        }
        @Override public double getLat() {
            return buffer[0];
        }
        @Override public double getLng() {
            return buffer[1];
        }
        @Override public Reading read() {
            return this;
        }
        @Override public Writing write() {
            return this;
        }
    }
    static class AggregateWriter extends Value.Editor<SimpleTree.AggregateWriter, Grouping, Merging, double[]> implements Grouping, Merging {
        /**
         * @param size       bytes per element (as determined by implementors)
         * @param fieldLimit
         */
        protected AggregateWriter(int size, int fieldLimit) {
            super(size, fieldLimit);
        }

        @Override
        public double[] createBuffer(int blocksize) {
            return new double[blocksize * 3];
        }

        @Override public double getDistance() {
            return buffer[4];
        }
        @Override public double getWest() {
            return buffer[0];
        }
        @Override public double getNorth() {
            return buffer[1];
        }
        @Override public double getEast() {
            return buffer[2];
        }
        @Override public double getSouth() {
            return buffer[3];
        }



        @Override public void setDistance(double distance) {
            buffer[4] = distance;
        }
        @Override public void setWest(double west) {
            buffer[0] = west;
        }
        @Override public void setNorth(double north) {
            buffer[1] = north;
        }
        @Override public void setEast(double east) {
            buffer[2] = east;
        }
        @Override public void setSouth(double south) {
            buffer[3] = south;
        }

        @Override public Grouping group() {
            return this;
        }
        @Override public Merging merge() {
            return this;
        }
    }
}
