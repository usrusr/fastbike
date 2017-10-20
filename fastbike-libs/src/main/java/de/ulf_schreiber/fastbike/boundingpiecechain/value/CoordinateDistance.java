package de.ulf_schreiber.fastbike.boundingpiecechain.value;

abstract public class CoordinateDistance<
        R extends CoordinateDistance.Reading<R> & Coordinate.Reading<R> & Value.CT,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R> & Coordinate.Writing<R,W> & Value.CT,
        G extends CoordinateDistance.Grouping<R,G> & Coordinate.Grouping<R,G> & Value.CT,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G> & Coordinate.Merging<R,G,M> & Value.CT
    > extends Coordinate<R,W,G,M>{

    public CoordinateDistance(double precision) {
        super(precision);
    }

    interface Reading <
            R extends Reading<R> & CT
            > extends Coordinate.Reading<R> {
        double getDistance();
    }
    interface Writing<
            R extends Reading<R> & CT,
            W extends Writing<R,W> & CT
            > extends Reading<R>, Coordinate.Writing<R,W> {
        void setDistance(double distance);
    }
    interface Grouping<
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT
            > extends Coordinate.Grouping<R,G> {
        double getDistance();
    }
    interface Merging <
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT,
            M extends Merging<R,G,M>  & CT
            > extends Grouping<R,G>, Coordinate.Merging<R,G,M> {
        void setDistance(double distance);
    }
    public interface PublicRead extends Reading<PublicRead>, CT {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup>, CT {

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


    @Override
    protected Looker createElementWriter(){
        return new Looker(0,0);
    }

    class Looker extends Value<R,W,G,M>.Looking<Looker> implements Writing<R,W> , CT{
        protected Looker(int size, int fieldLimit) {
            super(size + layerlen, fieldLimit);
        }

        @Override
        public double getDistance() {
            return buffer.getDouble(actualIndex + Coordinate.Looking.layerlen);
        }

        @Override
        public void setDistance(double distance) {
            buffer.putDouble(actualIndex + Coordinate.Looking.layerlen, distance);
        }

        @Override
        public double getLat() {
            return 0;
        }

        @Override
        public double getLng() {
            return 0;
        }

        @Override
        public void setLat(double latitude) {

        }

        @Override
        public void setLng(double longitude) {

        }
    }


    class Var implements Writing<PublicRead,Var>, PublicRead {
        private double lat;
        private double lng;


        @Override
        public void setDistance(double distance) {

        }

        @Override
        public void setLat(double latitude) {

        }

        @Override
        public void setLng(double longitude) {

        }

        @Override
        public double getDistance() {
            return 0;
        }

        @Override
        public double getLat() {
            return 0;
        }

        @Override
        public double getLng() {
            return 0;
        }
    }

}
