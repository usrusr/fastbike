package de.ulf_schreiber.fastbike.boundingpiecechain;


import de.ulf_schreiber.fastbike.boundingpiecechain.value.Value;

/**
 * lng wraps around when extending over 180 or under-180
 */
public class Bounds {
    public interface Getters<A extends Getters<A, P>, P extends Point.Getters<P> & Value.CT> {
        double getEast();
        double getNorth();
        double getWest();
        double getSouth();
        boolean contains(Point.Getters<?> point); // should be available for any point, not just P
    }



    public interface Aggregate<A extends Aggregate<A,G,P> & Getters<A,P>, G extends Getters<G,P> & Value.CT, P extends Point.Getters<P> & Value.CT> {
        void extendBy(P point);
        void extendBy(G aggregate);
    }

    /** closes the selftype hierarchy */
    public interface Interface extends Getters<Interface, Point.Interface>, Value.CT {}

    public abstract static class Base<G extends Getters<G, P> , P extends Point.Getters<P> & Value.CT> implements Getters<G,P>  {
        @Override
        public boolean contains(Point.Getters<?> point) {
            double lat = point.getLat();
            if(lat< getSouth() || lat> getNorth()) return false;
            double lng = point.getLng();
            double east = getEast();
            double west = getWest();
            if(lng>=west && lng<= east) return true;

            double lngWrapEast = lng+360;
            if(lngWrapEast>=west && lngWrapEast<= east) return true;

            double lngWrapWest = lng-360;
            if(lngWrapWest>=west && lngWrapWest<= east) return true;

            return false;
        }
    }
    public abstract static class BaseWrite<
            A extends BaseWrite<A,G,P>,
            G extends Getters<G,P> & Value.CT,
            P extends Point.Getters<P> & Value.CT
        > extends Base<A,P> implements Aggregate<A,G,P> {
        abstract void setEast (double east);
        abstract void setNorth(double north);
        abstract void setWest (double west);
        abstract void setSouth(double south);
        @Override
        public void extendBy(P point) {
            double lat = point.getLat();
            setNorth(Math.max(getNorth(), lat));
            setSouth(Math.min(getSouth(), lat));

            double lng = point.getLng();

            double myEast = getEast();
            double myWest = getWest();

            double myMid = (myEast+myWest)/2;

            double offset = 0;
            while(lng +offset - myMid < -180) offset+=360;
            while(lng +offset - myMid > 180) offset-=360;

            setEast(Math.max(myEast, myEast+offset));
            setWest(Math.min(myWest, myWest+offset));
        }

        @Override
        public void extendBy(G other) {
            double north = other.getNorth();
            double south = other.getSouth();
            setNorth(Math.max(getNorth(), north));
            setSouth(Math.min(getSouth(), south));


            double east = other.getEast();
            double west = other.getWest();


            double myEast = getEast();
            double myWest = getWest();

            double mid = (east+west)/2;
            double myMid = (myEast+myWest)/2;

            double offset = 0;
            while(mid+offset - myMid < -180) offset+=360;
            while(mid+offset - myMid > 180) offset-=360;

            setEast(Math.max(myEast, east+offset));
            setWest(Math.min(myWest, west+offset));
        }
    }

}