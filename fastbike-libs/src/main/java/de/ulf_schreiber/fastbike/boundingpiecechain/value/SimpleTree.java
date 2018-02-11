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
        SimpleTree.AggregateWriter,
        SimpleTree
> {

    public SimpleTree() {
        super(new Meta(0.00001d));
    }


    static class Meta extends CoordinateDistance<Reading,Writing,Grouping,Merging>{
        public Meta(double precision) {
            super(precision);
        }
        @Override ElementWriter createElementWriter() {
            return new ElementWriter();
        }
        @Override AggregateWriter createAggregateWriter() {
            return new AggregateWriter();
        }
        @Override SimpleTree.Merging createMutableBounds() {
            return new MutableBounds().write();
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

    static class MutableBounds extends CoordinateDistance.VaringAggregate<
            Reading, Grouping, Merging, MutableBounds
    >implements Merging{

    }
    static class MutableVal extends CoordinateDistance.Varing<
            Reading, Writing, MutableVal
            >{

    }

    static final class ElementWriter
            extends Value.Editor<ElementWriter,Reading,Writing,double[]>
            implements Writing, Reading
    {
        protected ElementWriter() {
            super(3, 3);
        }

        @Override
        public double[] createBuffer(int blocksize) {
            return new double[blocksize * 3];
        }
        @Override public void setDistance(double distance) {
            buffer[actualIndex + 2] = distance;
        }
        @Override public void setLat(double latitude) {
            buffer[actualIndex + 0] = latitude;
        }
        @Override public void setLng(double longitude) {
            buffer[actualIndex + 1] = longitude;
        }
        @Override public double getDistance() {
            return buffer[actualIndex + 2];
        }
        @Override public double getLat() {
            return buffer[actualIndex + 0];
        }
        @Override public double getLng() {
            return buffer[actualIndex + 1];
        }
        @Override public Reading read() {
            return this;
        }
        @Override public Writing write() {
            return this;
        }
    }
    static final class AggregateWriter extends Value.Editor<SimpleTree.AggregateWriter, Grouping, Merging, double[]> implements Grouping, Merging {
        protected AggregateWriter() {
            super(5, 5);
        }

        @Override
        public double[] createBuffer(int blocksize) {
            return new double[blocksize * 5];
        }

        @Override public double getDistance() {
            return buffer[actualIndex + 4];
        }
        @Override public double getWest() {
            return buffer[actualIndex + 0];
        }
        @Override public double getNorth() {
            return buffer[actualIndex + 1];
        }
        @Override public double getEast() {
            return buffer[actualIndex + 2];
        }
        @Override public double getSouth() {
            return buffer[actualIndex + 3];
        }



        @Override public void setDistance(double distance) {
            buffer[actualIndex + 4] = distance;
        }
        @Override public void setWest(double west) {
            buffer[actualIndex + 0] = west;
        }
        @Override public void setNorth(double north) {
            buffer[actualIndex + 1] = north;
        }
        @Override public void setEast(double east) {
            buffer[actualIndex + 2] = east;
        }
        @Override public void setSouth(double south) {
            buffer[actualIndex + 3] = south;
        }

        @Override public Grouping read() {
            return this;
        }
        @Override public Merging write() {
            return this;
        }
    }
}
