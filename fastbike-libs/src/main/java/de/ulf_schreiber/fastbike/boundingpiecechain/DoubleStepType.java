//package de.ulf_schreiber.fastbike.boundingpiecechain;
//
//
//public class DoubleStepType implements ContentType<DoubleStepType, DoubleStepType.Looker> {
//    public static DoubleStepType instance = new DoubleStepType();
//
//    @Override
//    public int size() {
//        return 4;
//    }
//
//    @Override
//    public int bytesize() {
//        return 32;
//    }
//
//    @Override
//    public Object createArray(int len) {
//        return new double[len];
//    }
//
//    @Override
//    public Looker createPointLooker() {
//        return new Looker();
//    }
//
//
//    protected class Looker implements Step<Looker>, BufferLooking<DoubleStepType>, PointWriter{
//
//
//        double[] buffer;
//        int index;
//
//        @Override
//        public void at(Object array, int index, int offset) {
//            buffer = (double[]) array;
//            this.index = offset + 4 * index;
//        }
//
//        @Override
//        public void move(int direction) {
//            index += direction * 4;
//        }
//
//        @Override
//        public DoubleStepType type() {
//            return DoubleStepType.instance;
//        }
//
//
//        @Override
//        public double getLat() {
//            return buffer[0];
//        }
//
//        @Override
//        public double getLng() {
//            return buffer[1];
//        }
//
//        @Override
//        public double getDist() {
//            return buffer[2];
//        }
//
//        @Override
//        public boolean sameAs(Looker b, double precision) {
//            return
//                    Math.abs(getLat() - b.getLat()) < precision &&
//                            Math.abs(getLng() - b.getLng()) < precision &&
//                            Math.abs(getLat() - b.getLat()) < precision &&
//                            Math.abs(getDist() - b.getDist()) < precision
//                    ;
//        }
//
//        @Override
//        public void setLat(double lat) {
//
//        }
//
//        @Override
//        public void setLng(double lng) {
//
//        }
//    }
//}