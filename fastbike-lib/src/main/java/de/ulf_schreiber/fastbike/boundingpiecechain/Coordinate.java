package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;

/** pointless class, fold into coordinateDistance */
public abstract class Coordinate<
        V extends Coordinate<V,R,W,G,M,B,L,A>,
        R extends Coordinate.Reading<R> & BaseTree.Reading<R>,
        W extends Coordinate.Writing<R,W> & Coordinate.Reading<R> & BaseTree.Writing<R,W>,
        G extends Coordinate.Grouping<R,G> & BaseTree.Grouping<R,G>,
        M extends Coordinate.Merging<R,G,M> & Coordinate.Grouping<R,G> & BaseTree.Merging<R,G,M>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
        > extends BaseTree<V, R, W, G, M, B, L, A> {

    public Coordinate(int blocksize, double precision) {
        super(blocksize, precision);
    }

    interface Reading<
            R extends Reading<R>
            > extends BaseTree.Reading<R> , Read {
//        double getLat();
//        double getLng();
    }

    interface Writing<
            R extends Reading<R>,
            W extends Writing<R, W>
            > extends Reading<R>, BaseTree.Writing<R, W> {
        void setLat(double latitude);
        void setLng(double longitude);
    }

    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R, G>
            > extends BaseTree.Grouping<R, G>, Group {

//        double getWest();
//        double getNorth();
//        double getEast();
//        double getSouth();
    }

    interface Merging<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>
            > extends Grouping<R,G>, BaseTree.Merging<R, G, M> {
        void setWest(double west);
        void setNorth(double north);
        void setEast(double east);
        void setSouth(double south);
    }

    public interface Read {
        double getLat();
        double getLng();

        class Immutable implements Read {
            final private double lat;
            final private double lng;
            public static Immutable of(double lat, double lng){
                return new Immutable(lat, lng);
            }
            public Immutable(double lat, double lng) {
                this.lat = lat;
                this.lng = lng;
            }
            @Override public double getLat() {
                return lat;
            }
            @Override public double getLng() {
                return lng;
            }
        }
    }

    public interface Group {
        double getWest();
        double getNorth();
        double getEast();
        double getSouth();
    }
    @Override boolean sameAs(R one, R other) {
        return
                Math.abs(one.getLng() - other.getLng()) < precision
                        && Math.abs(one.getLat() - other.getLat()) < precision
                ;
    }

    @Override W clearWrite(W toClear) {
        toClear.setLat(Double.NaN);
        toClear.setLng(Double.NaN);
        return super.clearWrite(toClear);
    }

    @Override M clearMerge(M toClear) {
        toClear.setEast(Double.NaN);
        toClear.setWest(Double.NaN);
        toClear.setNorth(Double.NaN);
        toClear.setSouth(Double.NaN);

        return super.clearMerge(toClear);
    }

    @Override void copy(R from, W to) {
        to.setLat(from.getLat());
        to.setLng(from.getLng());
        super.copy(from, to);
    }

//    public boolean contains(Coordinate.Grouping<R, G> box, Coordinate.Reading<R> point) {
//        double lat = point.getLat();
//        if(Double.isNaN(lat)) return false;
//        double south = box.getSouth();
//        if(Double.isNaN(south)) return false;
//        if(lat < south -precision || lat > box.getNorth()+precision) return false;
//        double lng = point.getLng();
//        double east = box.getEast() + precision;
//        double west = box.getWest() -precision;
//        if(lng>=west && lng<= east) return true;
//
//        double lngWrapEast = lng+360;
//        if(lngWrapEast>=west && lngWrapEast<= east) return true;
//
//        double lngWrapWest = lng-360;
//        if(lngWrapWest>=west && lngWrapWest<= east) return true;
//
//        return true;
//    }

    @Override void extendBy(M toExtend, R point) {
        super.extendBy(toExtend, point);
        double lat = point.getLat();
        if(Double.isNaN(lat)) return;
        toExtend.setNorth(max(toExtend.getNorth(), lat));
        toExtend.setSouth(min(toExtend.getSouth(), lat));

        double lng = point.getLng();

        double myEast = toExtend.getEast();
        double myWest = toExtend.getWest();

        double myMid = (myEast+myWest)/2;

        double offset = 0;
        while(lng +offset - myMid < -180) offset+=360;
        while(lng +offset - myMid > 180) offset-=360;

        toExtend.setEast(max(myEast, lng+offset));
        toExtend.setWest(min(myWest, lng+offset));
    }


    @Override void extendBy(M toExtend, G other) {
        super.extendBy(toExtend, other);
        double north = other.getNorth();
        double south = other.getSouth();
        toExtend.setNorth(max(toExtend.getNorth(), north));
        toExtend.setSouth(min(toExtend.getSouth(), south));


        double east = other.getEast();
        double west = other.getWest();


        double myEast = toExtend.getEast();
        double myWest = toExtend.getWest();

        double mid = (east+west)/2;
        double myMid = (myEast+myWest)/2;

        double offset = 0;
        while(mid+offset - myMid < -180) offset+=360;
        while(mid+offset - myMid > 180) offset-=360;

        toExtend.setEast(max(myEast, east+offset));
        toExtend.setWest(min(myWest, west+offset));
    }

    @Override
    void stringifyPoint(Appendable sw, R point) throws IOException {
        if(point==null) {
            sw.append("null");
        }else{
            sw.append('[');
            stringifyDouble(sw, point.getLat());
            sw.append(':');
            stringifyDouble(sw, point.getLng());
            sw.append(']');
        }
    }
    @Override
    protected void interpolate(R from, R to, double fraction, W result) {
        {
            double fVal = from.getLat();
            result.setLat(fVal + (to.getLat() - fVal) * fraction);
        }
        {
            double fVal = from.getLng();
            result.setLng(fVal + (to.getLng() - fVal) * fraction);
        }
        super.interpolate(from, to, fraction, result);
    }


    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R,W>,
            V extends Varing<R,W,V>
        > extends BaseTree.Varing<R,W,V> implements Writing<R,W> {
        private double lat = Double.NaN;
        private double lng = Double.NaN;

        @Override final public double getLat() {
            return lat;
        }
        @Override final public double getLng() {
            return lng;
        }
        @Override final public void setLat(double latitude) {
            this.lat = latitude;
        }
        @Override final public void setLng(double longitude) {
            this.lng = longitude;
        }

    }


    protected abstract static class VaringAggregate<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>,
            A extends VaringAggregate<R, G, M, A> & Merging<R,G,M>
           > extends BaseTree.VaringAggregate<R, G, M, A> implements Merging<R,G,M> {
        private double west = Double.NaN;
        private double north = Double.NaN;
        private double east = Double.NaN;
        private double south = Double.NaN;

        @Override final public double getWest() {
            return west;
        }
        @Override final public double getNorth() {
            return north;
        }
        @Override final public double getEast() {
            return east;
        }
        @Override final public double getSouth() {
            return south;
        }
        @Override final public void setWest(double west) {
            this.west = west;
        }
        @Override final public void setNorth(double north) {
            this.north = north;
        }
        @Override final public void setEast(double east) {
            this.east = east;
        }
        @Override final public void setSouth(double south) {
            this.south = south;
        }
    }

}