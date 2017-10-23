package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import java.io.IOException;

public abstract class Coordinate<
        R extends Coordinate.Reading<R> & Value.Reading<R>,
        W extends Coordinate.Writing<R,W> & Coordinate.Reading<R> & Value.Writing<R,W>,
        G extends Coordinate.Grouping<R,G> & Value.Grouping<R,G>,
        M extends Coordinate.Merging<R,G,M> & Coordinate.Grouping<R,G> & Value.Merging<R,G,M>
        > extends Value<R, W, G, M> {

    public Coordinate(double precision) {
        super(precision);
    }

    interface Reading<
            R extends Reading<R>
            > extends Value.Reading<R> {
        double getLat();
        double getLng();
    }

    interface Writing<
            R extends Reading<R>,
            W extends Writing<R, W>
            > extends Reading<R>, Value.Writing<R, W> {
        void setLat(double latitude);
        void setLng(double longitude);
    }

    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R, G>
            > extends Value.Grouping<R, G> {

        double getWest();
        double getNorth();
        double getEast();
        double getSouth();
    }

    interface Merging<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>
            > extends Grouping<R,G>, Value.Merging<R, G, M> {
        void setWest(double west);
        void setNorth(double north);
        void setEast(double east);
        void setSouth(double south);
    }

    public interface PublicRead extends Reading<PublicRead> {

    }

    public interface PublicGroup extends Grouping<PublicRead, PublicGroup> {

    }
    @Override
    public boolean sameAs(R one, R other) {
        return
                Math.abs(one.getLng() - other.getLng()) < precision
                        && Math.abs(one.getLat() - other.getLat()) < precision
                ;
    }

    @Override
    public void copy(R from, W to) {
        to.setLat(from.getLat());
        to.setLng(from.getLng());
        super.copy(from, to);
    }

    public boolean contains(Coordinate.Grouping<R, G> box, Coordinate.Reading<R> point) {
        double lat = point.getLat();
        if(lat < box.getSouth()-precision || lat > box.getNorth()+precision) return false;
        double lng = point.getLng();
        double east = box.getEast() + precision;
        double west = box.getWest() -precision;
        if(lng>=west && lng<= east) return true;

        double lngWrapEast = lng+360;
        if(lngWrapEast>=west && lngWrapEast<= east) return true;

        double lngWrapWest = lng-360;
        if(lngWrapWest>=west && lngWrapWest<= east) return true;

        return true;
    }

    @Override
    public void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);
        double lat = point.getLat();
        toExtend.setNorth(Math.max(toExtend.getNorth(), lat));
        toExtend.setSouth(Math.min(toExtend.getSouth(), lat));

        double lng = point.getLng();

        double myEast = toExtend.getEast();
        double myWest = toExtend.getWest();

        double myMid = (myEast+myWest)/2;

        double offset = 0;
        while(lng +offset - myMid < -180) offset+=360;
        while(lng +offset - myMid > 180) offset-=360;

        toExtend.setEast(Math.max(myEast, myEast+offset));
        toExtend.setWest(Math.min(myWest, myWest+offset));
    }

    @Override
    public void extendBy(M toExtend, G other) {
        super.extendBy(toExtend, other);
        double north = other.getNorth();
        double south = other.getSouth();
        toExtend.setNorth(Math.max(toExtend.getNorth(), north));
        toExtend.setSouth(Math.min(toExtend.getSouth(), south));


        double east = other.getEast();
        double west = other.getWest();


        double myEast = toExtend.getEast();
        double myWest = toExtend.getWest();

        double mid = (east+west)/2;
        double myMid = (myEast+myWest)/2;

        double offset = 0;
        while(mid+offset - myMid < -180) offset+=360;
        while(mid+offset - myMid > 180) offset-=360;

        toExtend.setEast(Math.max(myEast, east+offset));
        toExtend.setWest(Math.min(myWest, west+offset));
    }

    @Override
    void stringifyPoint(Appendable sw, R point){
        try {
            if(point==null) {
                sw.append("null");
            }else{
                sw.append('[').append(""+point.getLat()).append(':').append(""+point.getLng()).append(']');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    protected abstract class ElementLooking<L extends Looking<L> & Writing<R,W>> extends Looking<L> implements Writing<R,W>{
//        private final int skip;
//        protected ElementLooking(int subsize, int fieldLimit) {
//            super(subsize + 8, fieldLimit);
//            skip = weight - (subsize + 8);
//        }
//        @Override public double getLat() {
//            return buffer.getDouble(actualIndex);
//        }
//
//        @Override public double getLng() {
//            return buffer.getDouble(actualIndex + skip + 4);
//        }
//
//        @Override public void setLat(double latitude) {
//            buffer.putDouble(actualIndex, latitude);
//        }
//
//        @Override public void setLng(double longitude) {
//            buffer.putDouble(actualIndex + skip + 4, longitude);
//        }
//    }

    protected abstract static class Varing<V extends Varing<V>> extends Value.Varing<V> implements Writing<V,V> {
        private double lat;
        private double lng;

        @Override public double getLat() {
            return lat;
        }
        @Override public double getLng() {
            return lng;
        }
        @Override public void setLat(double latitude) {
            this.lat = latitude;
        }
        @Override public void setLng(double longitude) {
            this.lng = longitude;
        }

    }

    static class Var extends Varing<Var> {
        @Override
        public Var read() {
            return this;
        }

        @Override
        public Var write() {
            return this;
        }
    }

}
