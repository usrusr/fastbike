package de.ulf_schreiber.fastbike.boundingpiecechain;


public class SimpleTree extends CoordinateDistance<
        SimpleTree,
        SimpleTree.Reading,
        SimpleTree.Writing,
        SimpleTree.Grouping,
        SimpleTree.Merging,
        double[],
        SimpleTree.ElementWriter,
        SimpleTree.AggregateWriter
    >{

    public SimpleTree(int blocksize, double precision) {
        super(blocksize, precision);
    }

    @Override
    protected SimpleTree.ElementWriter createElementWriter() {
        return new ElementWriter(blocksize);
    }

    @Override
    protected SimpleTree.AggregateWriter createAggregateWriter() {
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
    Reading immutable(double lat, double lng, double distance){
        Writing mutableVal = createMutableVal();
        mutableVal.setLat(lat);
        mutableVal.setLng(lng);
        mutableVal.setDistance(distance);
        return mutableVal.read();
    }

    protected interface Reading extends CoordinateDistance.Reading<Reading> {
    }
    protected interface Writing extends CoordinateDistance.Writing<Reading,Writing>, CoordinateDistance.Reading<Reading>{
    }
    protected interface Grouping extends CoordinateDistance.Grouping<Reading,Grouping>{
    }
    protected interface Merging extends CoordinateDistance.Merging<Reading,Grouping,Merging>, CoordinateDistance.Grouping<Reading,Grouping>{
    }

    static class MutableBounds extends CoordinateDistance.VaringAggregate<
            Reading, Grouping, Merging, MutableBounds
    > implements Merging{

    }
    static class MutableVal extends CoordinateDistance.Varing<
            Reading, Writing, MutableVal
    > implements Writing{

    }

    static final class ElementWriter
            extends BaseTree.Editor<ElementWriter,Reading,Writing,double[]>
            implements Reading, Writing
    {
        protected ElementWriter(int blocksize) {
            super(3, blocksize);
        }

        @Override
        public double[] createBuffer() {
            return new double[size * 3];
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
    static final class AggregateWriter extends BaseTree.Editor<SimpleTree.AggregateWriter, Grouping, Merging, double[]> implements Grouping, Merging {
        protected AggregateWriter(int blocksize) {
            super(5, blocksize);
        }

        @Override
        public double[] createBuffer() {
            return new double[size * 5];
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
