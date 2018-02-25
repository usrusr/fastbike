package de.ulf_schreiber.fastbike.boundingpiecechain;


public class SimpleTreeHeight extends CoordinateDistanceHeight<
        SimpleTreeHeight,
        SimpleTreeHeight.Reading,
        SimpleTreeHeight.Writing,
        SimpleTreeHeight.Grouping,
        SimpleTreeHeight.Merging,
        double[],
        SimpleTreeHeight.ElementWriter,
        SimpleTreeHeight.AggregateWriter
    > {

    public SimpleTreeHeight(int blocksize, double precision) {
        super(blocksize, precision);
    }

    @Override
    SimpleTreeHeight.ElementWriter createElementWriter() {
        return new ElementWriter(blocksize);
    }

    @Override
    SimpleTreeHeight.AggregateWriter createAggregateWriter() {
        return new AggregateWriter(blocksize);
    }

    @Override
    Merging createMutableBounds() {
        return new MutableBounds();
    }

    @Override
    Writing createMutableVal() {
        return new MutableVal();
    }

    interface Reading extends CoordinateDistanceHeight.Reading<Reading> {
    }
    interface Writing extends CoordinateDistanceHeight.Writing<Reading,Writing>, CoordinateDistanceHeight.Reading<Reading>{
    }
    interface Grouping extends CoordinateDistanceHeight.Grouping<Reading,Grouping>{
    }
    interface Merging extends CoordinateDistanceHeight.Merging<Reading,Grouping,Merging>, CoordinateDistanceHeight.Grouping<Reading,Grouping>{
    }

    static class MutableBounds extends VaringAggregate<
            Reading, Grouping, Merging, MutableBounds
    > implements Merging{

    }
    static class MutableVal extends Varing<
            Reading, Writing, MutableVal
    > implements Writing{

    }

    static final class ElementWriter
            extends BaseTree.Editor<ElementWriter,Reading,Writing,double[]>
            implements Reading, Writing
    {
        protected ElementWriter(int blocksize) {
            super(4, blocksize);
        }

        @Override
        public double[] createBuffer() {
            return new double[size * 4];
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
        @Override
        public void setHeight(double height) {
            buffer[actualIndex + 3] = height;
        }
        @Override
        public double getHeight() {
            return buffer[actualIndex + 3];
        }

        @Override public Reading read() {
            return this;
        }

        @Override public Writing write() {
            return this;
        }
    }
    static final class AggregateWriter extends BaseTree.Editor<SimpleTreeHeight.AggregateWriter, Grouping, Merging, double[]> implements Grouping, Merging {
        protected AggregateWriter(int blocksize) {
            super(7, blocksize);
        }

        @Override
        public double[] createBuffer() {
            return new double[size * 7];
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

        @Override public double getClimb() {
            return buffer[actualIndex + 5];
        }
        @Override public double getLastHeight() {
            return buffer[actualIndex + 6];
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

        @Override public void setClimb(double climb) {
            buffer[actualIndex + 5] = climb;
        }
        @Override public void setLastHeight(double lastHeight) {
            buffer[actualIndex + 6] = lastHeight;
        }


        @Override public Grouping read() {
            return this;
        }
        @Override public Merging write() {
            return this;
        }
    }
}
