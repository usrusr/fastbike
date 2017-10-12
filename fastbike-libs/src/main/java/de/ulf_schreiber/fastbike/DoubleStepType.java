package de.ulf_schreiber.fastbike;

import de.ulf_schreiber.fastbike.boundingpiecechain.BufferLooking;
import de.ulf_schreiber.fastbike.boundingpiecechain.ContentType;
import de.ulf_schreiber.fastbike.boundingpiecechain.Step;


public class DoubleStepType implements ContentType<DoubleStepType, DoubleStepType.Looker> {
    public static DoubleStepType instance = new DoubleStepType();

    @Override
    public int size() {
        return 4;
    }

    @Override
    public int bytesize() {
        return 32;
    }

    @Override
    public Object createArray(int len) {
        return new double[len];
    }

    @Override
    public Looker createPointLooker() {
        return new Looker();
    }


    public class Looker extends Step.Implementation<Looker> implements BufferLooking<DoubleStepType> {


        double[] buffer;
        int index;
        int size;

        @Override
        public void at(Object array, int index, int offset) {
            buffer = (double[]) array;
            this.index = offset + 4 * index;
            this.size = (buffer.length - offset) / 4;
        }

        @Override
        public void move(int direction) {
            index += direction * 4;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public DoubleStepType type() {
            return DoubleStepType.instance;
        }


        @Override
        public double getLat() {
            return buffer[0];
        }

        @Override
        public double getLng() {
            return buffer[1];
        }

        @Override
        public double getDist() {
            return buffer[2];
        }

        @Override
        public boolean sameAs(Looker b, double precision) {
            return
                    Math.abs(getLat() - b.getLat()) < precision &&
                            Math.abs(getLng() - b.getLng()) < precision &&
                            Math.abs(getLat() - b.getLat()) < precision &&
                            Math.abs(getDist() - b.getDist()) < precision
                    ;
        }


        @Override
        protected void setLenImpl(double len) {
            buffer[2]=len;
        }

        @Override
        protected void setLatLngImpl(double lat, double lng) {
            buffer[0]=lat;
            buffer[1]=lng;
        }
    }
}