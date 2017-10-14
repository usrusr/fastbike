//package de.ulf_schreiber.fastbike.boundingpiecechain;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.ListIterator;
//
//
///**
// * <nl>
// *     <li>somewhat memory-efficient</li>
// *     <li>piece table</li>
// *     <li>append-friendly</li>
// *     <li>somewhat fast "point -&gt; closest points along track within radius" function</li>
// *     <li>distance is independent from geometric distance</li>
// *     <li>nodes can have extra data like height, cost,...</li>
// * </nl>
// *
// * @param <R>
// * @param <T>
// */
//public class BoundingTree<R extends Step.Getters<R> & Point.Sameable<R>, W extends Step.S, T extends ContentType<T, R, ?>> {
//    int blocksize = 16;
//    double resolution = 0.0000001;
//
//    T type;
//
//    void stringifyPoint(Appendable sw, R point){
//        try {
//         if(point==null) {
//                 sw.append("null");
//         }else{
//             sw.append('[').append(""+point.getLat()).append(':').append(""+point.getLng()).append(']');
//         }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    boolean samePoint(R a, R b){
//        if(a==null&&b==null) return true;
//        if(a==null||b==null) return false;
//        return a.sameAs(b, resolution);
//    }
//
//    static abstract class Node<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?>, N extends Node<R,T,N>> extends MutableBoundingBox{
//        GroupNode parent;
//        N previous;
//        N next;
//
//        double distance;
//
//        void growBoundingBox(R include){
//            double lng = include.getLng();
//            double lat = include.getLng();
//            getWest = Math.min(getWest, lng);
//            getEast = Math.max(getEast, lng);
//            getSouth = Math.min(getSouth, lat);
//            getNorth = Math.max(getNorth, lat);
//
//            if(parent!=null) growBoundingBox(include);
//        }
//        void build(BoundingBuilder<R, T> builder) {
//            if(builder.toSkip<=0 && builder.toVisit>= distance){
//                builder.toVisit-= distance;
//                builder.extendBoundsFor(this);
//            }
//        }
//    }
//    static class LeafNode<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?> > extends Node<R,T,LeafNode<R,T>> {
//        Object array;
//        /** in elements */
//        int size;
//        double distance;
//
//
//        LeafNode(BoundingTree<R,T> tree, Iterator<Step> it, R looker) {
//            array = tree.type.createArray(tree.blocksize*tree.type.size());
//            int i=0;
//            looker.at(array,0,0);
//            while(it.hasNext() && i<tree.blocksize){
//                Step next = it.next();
//
//                looker.copyFrom(next);
//                extendBoundsFor(next.getLat(), next.getLng());
//                distance+=next.getDist();
//
//                looker.move(1);
//            }
//        }
//
//        @Override
//        public void build(BoundingBuilder<R, T> builder) {
//            super.build(builder);
//
//            R looker = builder.looker;
//            looker.at(array, 0, 0);
//            final double resolution = builder.tree.resolution;
//
//            for(int i=0;i<size;i++){
//                double dist = looker.getDist();
//
//                double restToSkip = builder.toSkip-dist;
//                if(restToSkip> resolution){
//                    builder.toSkip = restToSkip;
//                }else{
//                    builder.toSkip = 0;
//                    builder.extend(looker.getLat(), looker.getLng());
//                    double restToVisit = builder.toVisit-dist;
//                    if(restToSkip< resolution){
//                        builder.toVisit=0;
//                        return;
//                    }else{
//                        builder.toVisit = restToVisit;
//                    }
//                }
//                looker.move(1);
//            }
//        }
//    }
//    static class GroupNode<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?> > extends Node<R,T,GroupNode<R,T>> {
//        Node<R,T,?>[] children;
//        int len;
//        boolean containsLeaves = false;
//
//        GroupNode(BoundingTree<R,T> tree) {
//            children = new Node[tree.blocksize*tree.type.size()];
//        }
//
//        @Override
//        public void build(BoundingBuilder<R, T> builder) {
//            super.build(builder);
//            if(builder.toVisit<builder.tree.resolution) return;
//
//            for(Node<R,T,?> n : children) {
//                if(n.distance < builder.toSkip){
//                    builder.toSkip-=n.distance;
//                }else{
//                    n.build(builder);
//                    if(builder.toVisit<builder.tree.resolution) return;
//                }
//            }
//        }
//
//        /** @return newChild if already full */
//        public Node<R, T, ?> add(Node<R, T, ?> newChild) {
//            if(len>=children.length){
//                return newChild;
//            }
//            distance+=newChild.distance;
//            extendBoundsFor(newChild);
//            children[len]=newChild;
//            len++;
//            return null;
//        }
//    }
//
//    static class Piece<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?> >{
//        final Node<R,T,?> root;
//        final double offset; // into node, e.g. 0 when the node was freshly created
//        final double distance;
//        final double getWest;
//        final double getNorth;
//        final double getEast;
//        final double getSouth;
//        Piece(Node<R,T,?> root, double offset, double distance, BoundingTree<R,T> tree) {
//            this.root = root;
//            this.offset = offset;
//            this.distance = distance;
//
//            BoundingBuilder<R, T> boundingBuilder = new BoundingBuilder<R,T>(offset, distance, tree);
//            root.build(boundingBuilder);
//            getWest = boundingBuilder.getWest;
//            getNorth = boundingBuilder.getNorth;
//            getEast = boundingBuilder.getEast;
//            getSouth = boundingBuilder.getSouth;
//        }
//    }
//    static class Undo<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?>> {
//        int at;
//        int removed; // first n pieces are removed, rest added
//        Piece<R, T>[] pieces;
//
//        final void apply(BoundingTree<R, T> tree) {
//            ListIterator<Piece> iterator = tree.pieces.listIterator(at);
//            int i = 0;
//            for (; i < removed; i++) iterator.remove();
//            int length = pieces.length;
//            for (; i < length; i++) iterator.add(pieces[i]);
//        }
//
//        final void unapply(BoundingTree<R, T> tree) {
//            ListIterator<Piece> iterator = tree.pieces.listIterator(at);
//            int i = removed;
//            int length = pieces.length;
//            for (; i < length; i++) iterator.remove();
//
//            for (i = 0; i < removed; i++) iterator.add(pieces[i]);
//        }
//    }
//
//    public static class BoundingBuilder<R extends Step.Getters<R> & Point.Sameable<R> & BufferLooking<T>, T extends ContentType<T, R, ?>> extends MutableBoundingBox {
//        private final BoundingTree<R, T> tree;
//
//        R looker;
//        double toSkip;
//        double toVisit;
//
//        public BoundingBuilder(double skipDistance, double distance, BoundingTree<R, T> tree) {
//            this.toSkip = skipDistance;
//            this.toVisit = distance;
//            this.tree = tree;
//            looker = tree.type.createPointLooker();
//        }
//
//        public void extend(double lat, double lng) {
//
//        }
//    }
//    private abstract static class MutableBoundingBox {
//        double getWest = 1;
//        double getNorth = -1;
//        double getEast = -1;
//        double getSouth = 1;
//
//        void extendBoundsFor(double lat, double lng){
//            getWest = Math.min(getWest, lng);
//            getEast = Math.max(getEast, lng);
//            getSouth = Math.min(getSouth, lat);
//            getNorth = Math.max(getNorth, lat);
//        }
//        void extendBoundsFor(MutableBoundingBox other){
//            getWest = Math.min(getWest, other.getWest);
//            getEast = Math.max(getEast, other.getEast);
//            getSouth = Math.min(getSouth, other.getSouth);
//            getNorth = Math.max(getNorth, other.getNorth);
//        }
//    }
//
//    private final LinkedList<Piece> pieces = new LinkedList<>();
//    private final LinkedList<Undo> undos = new LinkedList<>();
//    public void append(Iterable<R> points){
//        replace(Double.MAX_VALUE, 0, points);
//    }
//    public void insert(double at, Iterable<R> points){
//        replace(at, 0, points);
//    }
//    public void delete(double at, double distance){
//        replace(at, distance, Collections.<R>emptyList());
//    }
//    public void replace(double at, double distance, Iterable<R> points){
//
//    }
//
//
//    Node<R,T,?> makeTree(Iterable<Step> iterable){
//        R looker = type.createPointLooker();
//        Iterator<Step> it = iterable.iterator();
//        LeafNode<R, T> first = new LeafNode<R,T>(this, it, looker);
//
//        while(it.hasNext()){
//
//
//        }
//
//
//
//        Node<R, T, ?> ret = first;
//        return ret;
//    }
//
//    /** @return new root or self (will always return self if called with parent) */
//    Node<R,T,?> extendTree(GroupNode<R,T> group, GroupNode<R,T> parent, Iterator<Step> iterator, R looker){
//        GroupNode<R, T> cur = group;
//        while(iterator.hasNext()) {
//            if (cur.len == this.blocksize) {
//                if(parent==null){
//                    GroupNode<R, T> newParent = new GroupNode<>(this);
//                    newParent.add(cur);
//                    return extendTree(newParent, null, iterator, looker);
//                }else {
//                    // continue in parent call
//                    return group;
//                }
//            }
//            if(cur.containsLeaves){
//                LeafNode<R, T> newChild = new LeafNode<>(this, iterator, looker);
//
//                cur.add(newChild);
//            }else{
//                GroupNode<R, T> newChild = new GroupNode<>(this);
//                extendTree(newChild, cur, iterator, looker);
//                cur.add(newChild);
//            }
//        }
//        return group;
//    }
//
//}
