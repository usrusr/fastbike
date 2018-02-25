package de.ulf_schreiber.fastbike.boundingpiecechain;


public class Route extends CoordinateDistanceHeight<
        Route,
        Route.Reading,
        Route.Writing,
        Route.Grouping,
        Route.Merging,
        double[],
        Route.ElementWriter,
        Route.AggregateWriter
    > {

    public Route(int blocksize, double precision) {
        super(blocksize, precision);
    }

    @Override
    protected Route.ElementWriter createElementWriter() {
        return new ElementWriter(blocksize);
    }

    @Override
    protected Route.AggregateWriter createAggregateWriter() {
        return new AggregateWriter(blocksize);
    }

    @Override
    protected Merging createMutableBounds() {
        return new MutableBounds();
    }

    @Override
    protected Writing createMutableVal() {
        return new MutableVal();
    }
    public Reading immutable(double lat, double lng, double distance, double height){
        Writing mutableVal = createMutableVal();
        mutableVal.setLat(lat);
        mutableVal.setLon(lng);
        mutableVal.setDistance(distance);
        mutableVal.setHeight(height);
        return mutableVal.read();
    }
    protected interface Reading extends CoordinateDistanceHeight.Reading<Reading> {
    }
    protected interface Writing extends CoordinateDistanceHeight.Writing<Reading,Writing>, CoordinateDistanceHeight.Reading<Reading>{
    }
    protected interface Grouping extends CoordinateDistanceHeight.Grouping<Reading,Grouping>{
    }
    protected interface Merging extends CoordinateDistanceHeight.Merging<Reading,Grouping,Merging>, CoordinateDistanceHeight.Grouping<Reading,Grouping>{
    }

    protected static class MutableBounds extends VaringAggregate<
            Reading, Grouping, Merging, MutableBounds
    > implements Merging{

    }
    protected static class MutableVal extends Varing<
            Reading, Writing, MutableVal
    > implements Writing{

    }

    protected static final class ElementWriter
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
        @Override public void setLon(double longitude) {
            buffer[actualIndex + 1] = longitude;
        }
        @Override public double getDistance() {
            return buffer[actualIndex + 2];
        }
        @Override public double getLat() {
            return buffer[actualIndex + 0];
        }
        @Override public double getLon() {
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
    protected static final class AggregateWriter extends BaseTree.Editor<Route.AggregateWriter, Grouping, Merging, double[]> implements Grouping, Merging {
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
