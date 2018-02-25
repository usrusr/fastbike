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
        > extends BaseTree<V,R,W,G,M,B,L,A> {

    public Coordinate(int blocksize, double precision) {
        super(blocksize, precision);
    }

    double projectedDistance(R a, R b){
        double dLat = a.getLat() - b.getLat();
        double dLon = a.getLon() - b.getLon();
        return Math.sqrt(dLat*dLat+dLon*dLon);
    }

    protected interface Reading<
            R extends Reading<R>
            > extends BaseTree.Reading<R> {
        double getLat();
        double getLon();
    }

    protected interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, BaseTree.Writing<R,W> {
        void setLat(double latitude);
        void setLon(double longitude);
    }

    protected interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends BaseTree.Grouping<R,G> {

        double getWest();
        double getNorth();
        double getEast();
        double getSouth();
    }

    protected interface Merging<
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, BaseTree.Merging<R,G,M> {
        void setWest(double west);
        void setNorth(double north);
        void setEast(double east);
        void setSouth(double south);
    }
    @Override boolean sameAs(R one, R other) {
        return
                Math.abs(one.getLon() - other.getLon()) < precision
                        && Math.abs(one.getLat() - other.getLat()) < precision
                ;
    }

    @Override W clearWrite(W toClear) {
        toClear.setLat(Double.NaN);
        toClear.setLon(Double.NaN);
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
        to.setLon(from.getLon());
        super.copy(from, to);
    }

//    public boolean contains(Coordinate.Grouping<R,G> box, Coordinate.Reading<R> point) {
//        double lat = point.getLat();
//        if(Double.isNaN(lat)) return false;
//        double south = box.getSouth();
//        if(Double.isNaN(south)) return false;
//        if(lat < south -precision || lat > box.getNorth()+precision) return false;
//        double lng = point.getLon();
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

        double lng = point.getLon();

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
            stringifyDouble(sw, point.getLon());
            sw.append(']');
        }
    }
    @Override
    protected W interpolate(R from, R to, double fraction, W result) {
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
        @Override final public double getLon() {
            return lng;
        }
        @Override final public void setLat(double latitude) {
            this.lat = latitude;
        }
        @Override final public void setLon(double longitude) {
            this.lng = longitude;
        }

    }


    protected abstract static class VaringAggregate<
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>,
            A extends VaringAggregate<R,G,M,A> & Merging<R,G,M>
           > extends BaseTree.VaringAggregate<R,G,M,A> implements Merging<R,G,M> {
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

    @Override
    protected boolean overlaps(G one, G other) {
        // no super call, because super call is better safe than sorry, always true
        if(one.getNorth() < other.getSouth()) return false;
        if(one.getSouth() > other.getNorth()) return false;

        /*
        w10e20 w15e25 t
        w10e20 w25e35 f

        w170e180 w-175e-165 f?
        w170e180 w-185e-175 t?
        */
        double east1 = one.getEast();
        double west1 = one.getWest();
        
        double west2 = other.getWest();
        double east2 = other.getEast();

        while(west1-west2 < -180) {
            east1+=360;
            west1+=360;
        }
        while(west2-west1<-180) {
            east2 += 360;
            west2 += 360;
        }

        if(east1 < west2) return false;
        if(west1 > east2) return false;

        if(east1 < west2) return false;
        if(west1 > east2) return false;

        return true;
    }

    protected boolean contains(G one, R other) {
        double lat = other.getLat();
        if(one.getNorth() < lat) return false;
        if(one.getSouth() > lat) return false;

        double east1 = one.getEast();
        double west1 = one.getWest();

        double west2 = other.getLon();

        while(west1-west2 < -180) {
            east1+=360;
            west1+=360;
        }
        while(west2-west1<-180) {
            west2 += 360;
        }


        if(east1 < west2) return false;
        if(west1 > west2) return false;

        return true;
    }
}
