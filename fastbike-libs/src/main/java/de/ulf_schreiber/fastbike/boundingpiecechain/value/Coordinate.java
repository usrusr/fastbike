package de.ulf_schreiber.fastbike.boundingpiecechain.value;

public abstract class Coordinate<
        R extends Coordinate.Reading<R> & Value.Reading<R> & Value.CT,
        W extends Coordinate.Writing<R,W> & Coordinate.Reading<R> & Value.Writing<R,W> & Value.CT,
        G extends Coordinate.Grouping<R,G> & Value.Grouping<R,G> & Value.CT,
        M extends Coordinate.Merging<R,G,M> & Coordinate.Grouping<R,G> & Value.Merging<R,G,M> & Value.CT
        > extends Value<R, W, G, M> {

    public Coordinate(double precision) {
        super(precision);
    }

    interface Reading<
            R extends Reading<R> & CT
            > extends Value.Reading<R> {
        double getLat();
        double getLng();
    }

    interface Writing<
            R extends Reading<R> & CT,
            W extends Writing<R, W> & CT
            > extends Reading<R>, Value.Writing<R, W> {
        double setLat(double latitude);
        double setLng(double longitude);
    }

    interface Grouping<
            R extends Reading<R> & CT,
            G extends Grouping<R, G> & CT
            > extends Value.Grouping<R, G> {

        double getWest();
        double getNorth();
        double getEast();
        double getSouth();
    }

    interface Merging<
            R extends Reading<R> & CT,
            G extends Grouping<R, G> & CT,
            M extends Merging<R, G, M> & CT
            > extends Grouping<R,G>, Value.Merging<R, G, M> {
        double setWest(double west);
        double setNorth(double north);
        double setEast(double east);
        double setSouth(double south);
    }

    public interface PublicRead extends Reading<PublicRead>, CT {

    }

    public interface PublicGroup extends Grouping<PublicRead, PublicGroup>, CT {

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
}
