package de.ulf_schreiber.fastbike;

import de.ulf_schreiber.fastbike.boundingpiecechain.BufferLooking;
import de.ulf_schreiber.fastbike.boundingpiecechain.ContentType;
import de.ulf_schreiber.fastbike.boundingpiecechain.DistanceBounds;
import de.ulf_schreiber.fastbike.boundingpiecechain.Point;
import de.ulf_schreiber.fastbike.boundingpiecechain.Step;


public class DoubleStepType implements ContentType<DoubleStepType, Step.Interface, DoubleStepType.Looker> {
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
    public DoubleStepType.Looker createPointLooker() {
        return new DoubleStepType.Looker();
    }

    public static class Looker extends Step.Base implements
            Step.Interface,
            BufferLooking<Looker, Step.Interface>,
            Step.Writers<Looker, Step.Interface> {


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
        public void copyFrom(Step.Interface from) {
            setDist(from.getDist());
            setLatLng(from.getLat(), from.getLng());
        }

        @Override
        public void setLatLng(double lat, double lng) {
            buffer[0] = lat;
            buffer[1] = lng;
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
        public void setDist(double dist) {
            buffer[2] = dist;
        }
    }
}