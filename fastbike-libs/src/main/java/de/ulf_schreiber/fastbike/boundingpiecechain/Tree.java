package de.ulf_schreiber.fastbike.boundingpiecechain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public final class Tree<
        V extends CoordinateDistance<R,W,G,M,B,L,A>,
        R extends CoordinateDistance.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
        G extends CoordinateDistance.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>,
        T extends Tree<V,R,W,G,M,B,L,A,T>
    > implements Iterable<R>{
    final V meta;

    public Tree(V meta) {
        this.meta = meta;
    }

    int blocksize = 16;

    void stringifyPoint(Appendable sw, R point){
        meta.stringifyPoint(sw,point);
    }

    boolean samePoint(R a, R b){
        return meta.sameAs(a,b);
    }


    abstract class Node<N extends Node<N>> {

        B array;
        int elements = 0;
        abstract void build(BoundingBuilder builder);
        abstract int depth();

        protected abstract boolean calculateNext(NodeIterator state, double skip);
    }
    class LeafNode extends Node<LeafNode> {
        LeafNode(Iterator<? extends R> it, L elemLooker, M mutableBoundsReturn) {
            array = elemLooker.createBuffer(blocksize);
            elemLooker.wrap(array, blocksize,0);
            while(it.hasNext() && elements < blocksize){
                R next = it.next();
                meta.copy(next, elemLooker.write());
                meta.extendBy(mutableBoundsReturn, next);
                elemLooker.moveRelative(1);
                elements++;
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(BoundingBuilder builder) {
            V meta = builder.tree().meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            L looker = builder.valueLooker;
            looker.wrap(array, builder.tree().blocksize, 0);

            for(int i=0;i<elements;i++){
                double dist = looker.read().getDistance();

                double restToSkip = builder.toSkip-dist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    meta.extendBy(builder.result, looker.read());
                    double restToVisit = builder.toVisit-dist;
                    if(restToSkip< precision){
                        builder.toVisit=0;
                        return;
                    }else{
                        builder.toVisit = restToVisit;
                    }
                }
                looker.moveRelative(1);
            }
        }

        @Override
        protected boolean calculateNext(NodeIterator state, double skip) {
            state.leaf=this; // for next iteration
            L looker = state.valueLooker;
            if(state.doneInCur < elements) {
                looker.wrap(array, elements, 0).moveAbsolute(state.doneInCur);

                while (skip > meta.precision) {
                    state.doneInCur++;
                    skip -= looker.moveRelative(1).read().getDistance();
                }
            }
            if(state.doneInCur>=elements){
                int stackDepth = state.grpstack.size();
                int idx = stackDepth -1;
                while(idx>=0){ // zip all deeper levels to first element
                    GroupNode groupNode = state.grpstack.get(idx);
                    int cur = state.idxstack.get(idx);
                    cur++;
                    if (cur < groupNode.elements) {
                        state.idxstack.set(idx, cur);
                        Node<?> child = groupNode.children[cur];
                        idx++;
                        while(idx < stackDepth){
                            GroupNode newParent = GroupNode.class.cast(child);
                            state.grpstack.set(idx, newParent);
                            state.idxstack.set(idx, 0);
                            child = newParent.children[0];
                        }
                        state.doneInCur = 0;
                        LeafNode cast = LeafNode.class.cast(child);
                        cast.calculateNext(state, skip);
                        return false;
                    }
                    idx--;
                }
                return false;
            }
            meta.copy(looker.read(), state.next);
            if(looker.hasNext()) looker.moveRelative(1);
            state.doneInCur++;
            return true;
        }
    }

    class GroupNode extends Node<GroupNode> {


        Node<?>[] children;

        int depth;

        @Override
        int depth() {
            return depth;
        }

        GroupNode(Iterator<? extends R> it, L elemLooker, A groupLooker, M mutableBoundsReturn, Node<?> firstChild, int depth){
            children = new Node[blocksize];
            array = groupLooker.createBuffer(blocksize);
            this.depth = depth;

            groupLooker.wrap(array, 0, 0);

            M groupLookerWrite = groupLooker.write();
            G mutableBoundsReturnRead = mutableBoundsReturn.read();

            if(firstChild!=null) { // creating a new root
                children[0]=firstChild;
                meta.clearMerge(groupLookerWrite);
                meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
            }

            while(it.hasNext() && elements<blocksize){
                Node<?> newNode;

                if(depth==1){
                    groupLooker.moveAbsolute(elements);
                    newNode = new LeafNode(it, elemLooker, groupLookerWrite);
                } else {
                    meta.clearMerge(mutableBoundsReturn);
                    newNode = new GroupNode(it, elemLooker, groupLooker, mutableBoundsReturn, null, depth-1);
                    groupLooker.wrap(array, elements, 0);
                    meta.clearMerge(groupLookerWrite);
                    meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
                }
                children[elements] = newNode;
                elements++;
            }

            meta.clearMerge(mutableBoundsReturn);
            groupLooker.wrap(array, 0, 0);
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                meta.extendBy(mutableBoundsReturn, groupLooker.read());
            }
        }

        @Override
        public void build(BoundingBuilder builder) {
            V meta = builder.tree().meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            A groupLooker = builder.groupLooker;
            meta.clearMerge(groupLooker.write());
            groupLooker.wrap(array, 0, 0);
            G read = groupLooker.read();
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                double childDist = read.getDistance();

                double restToSkip = builder.toSkip-childDist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    double restToVisit = builder.toVisit-childDist;
                    if(restToVisit > -precision){
                        meta.extendBy(builder.result, read);
                        builder.toVisit=restToVisit;
                    }else{
                        children[i].build(builder);
                        return;
                    }
                }
            }
        }

        @Override
        protected boolean calculateNext(NodeIterator state, double skip) {
            A looker = state.groupLooker;
            looker.wrap(array, elements, 0);
            for(int i=0;i<elements;i++){
                double childLen = looker.read().getDistance();
                if(childLen +meta.precision > skip){
                    Node<?> child = children[i];
                    state.doneInCur=0;
                    state.grpstack.add(this);

                    state.idxstack.add(i);

                    return child.calculateNext(state, skip);
                }
                skip -= childLen;
                looker.moveRelative(1);
            }
            return false;
        }
    }

    protected T self(){
        return (T) this;
    }

    /**
     * wraps a node, either whole or a subset (this way, multiple pieces can use the same immutable tree, for when a piece is cut)
     */
    class Piece {
        final G bounds;
        final Node<?> root;
        final double offset; // length is contained in bounds
        Piece(Node<?> root, double offset, double distance) {
            this.root = root;
            this.offset = offset;

            BoundingBuilder boundingBuilder = new BoundingBuilder(offset, distance);
            root.build(boundingBuilder);
            bounds = boundingBuilder.result.read();
        }

        Piece(Iterable<? extends R> iterable){
            L elemLooker = meta.createElementWriter();
            A groupLooker = meta.createAggregateWriter();
            Iterator<? extends R> it = iterable.iterator();
            M mutableBounds = meta.createMutableBounds();
            LeafNode first = new LeafNode(it, elemLooker, mutableBounds);
            Node tmpRoot = first;
            int depth = 0;
            while(it.hasNext()){
                depth++;
                tmpRoot = new GroupNode(it, elemLooker, groupLooker, mutableBounds, tmpRoot, depth);
            }
            root = tmpRoot;
            offset = 0;
            bounds = mutableBounds.read();
        }

    }
    class Undo{
        final int skip;
        final int removed; // first n pieces are removed, rest added
        final List<Piece> pieces;

        Undo(int skip, List<Piece> toRemoveList, List<Piece> toAddList) {
            this.skip = skip;
            pieces = new ArrayList<>(toRemoveList.size()+toAddList.size());
            this.removed = toRemoveList.size();
            pieces.addAll(toRemoveList);
            pieces.addAll(toAddList);
        }

        final void apply() {
            ListIterator<Piece> iterator = Tree.this.pieces.listIterator(skip);
            int i = 0;
            for (; i < removed; i++) iterator.remove();
            int length = pieces.size();
            for (; i < length; i++) iterator.add(pieces.get(i));
        }

        final void unapply() {
            ListIterator<Piece> iterator = Tree.this.pieces.listIterator(skip);
            int i = removed;
            int length = pieces.size();
            for (; i < length; i++) iterator.remove();

            for (i = 0; i < removed; i++) iterator.add(pieces.get(i));
        }
    }

    public class BoundingBuilder {
        final M result;
        final L valueLooker;
        final A groupLooker;

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            result = meta.createMutableBounds();
            valueLooker = meta.<L,B>createElementWriter();
            groupLooker = meta.<A,B>createAggregateWriter();
        }

        public T tree() {
            return (T) Tree.this;
        }
    }


    private final LinkedList<Piece> pieces = new LinkedList<>();
    private final LinkedList<Undo> undos = new LinkedList<>();
    private final LinkedList<Undo> redos = new LinkedList<>();
    public void append(Iterable<? extends R> points){
        replace(Double.MAX_VALUE, 0, points);
    }
    public void insert(double from, Iterable<? extends R> points){
        replace(from, 0, points);
    }
    public void delete(double wrap, double distance){
        replace(wrap, distance, Collections.<R>emptyList());
    }
    public void replace(double from, double distance, Iterable<? extends R> points){
        List<Piece> toRemoveList = new ArrayList<>();
        List<Piece> toAddList = new ArrayList<>();

        int undoSkip = 0;
        if(from<Double.MAX_VALUE){
            double toSkip = from;
            double toRemove = distance;
            ListIterator<Piece> pit = pieces.listIterator();
            while(pit.hasNext()){
                Piece cur = pit.next();
                double plen = cur.bounds.getDistance();
                if(plen < toSkip  + meta.precision){
                    undoSkip++;
                    toSkip-=plen;
                }else{
                    pit.previous(); // add cur to toRemoveList in the next while loop
                    if(toSkip > meta.precision){
                        // add partial copy of first removed
                        toAddList.add(new Piece(cur.root, 0, toSkip));
                    }
                    break;
                }
            }
            toAddList.add(new Piece(points));
            while(pit.hasNext() && toRemove > meta.precision){
                Piece cur = pit.next();
                double plen = cur.bounds.getDistance();
                toRemove -= plen - toSkip;
                toSkip = 0;
                toRemoveList.add(cur);
                if(toRemove < -meta.precision){
                    double restToAdd = -toRemove;
                    toAddList.add(new Piece(cur.root, plen-restToAdd, restToAdd));
                }
            }
        }else{
            undoSkip = pieces.size();
            toAddList.add(new Piece(points));
        }

        Undo undo = new Undo(undoSkip, toRemoveList, toAddList);
        undo.apply();
        undos.add(undo);
        redos.clear();
    }


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

    @Override
    public Iterator<R> iterator() {
        return new Iterator<R>() {
            Iterator<Piece> pieceIt = pieces.iterator();
            Piece currentPiece = null;
            double currentPieceDistLeft = 0;

            NodeIterator nodeIt = new NodeIterator();
            double curNodeDone = 0;

            final W next = Tree.this.meta.createMutableVal();
            final W cur = Tree.this.meta.createMutableVal();
            boolean hasNext = calcNext();

            protected boolean calcNext(){
                while(pieceIt.hasNext() && ! nodeIt.hasNext()){
                    currentPiece=pieceIt.next();
                    currentPieceDistLeft=currentPiece.bounds.getDistance();
                    nodeIt.reset(currentPiece.root, currentPiece.offset, currentPiece.bounds.getDistance());
                }
                if( ! nodeIt.hasNext()) return false;
                meta.copy(nodeIt.next(), next);
                return true;
            }
            @Override public boolean hasNext() {
                return hasNext;
            }

            @Override public R next() {
                meta.copy(next.read(), cur);
                hasNext = calcNext();
                return cur.read();
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    class NodeIterator implements Iterator<R>{
        protected ArrayList<GroupNode> grpstack = new ArrayList<>();
        protected ArrayList<Integer> idxstack = new ArrayList<>();
        protected LeafNode leaf;
        protected double remaining;
        final private W next = meta.createMutableVal();
        final private W cur = meta.createMutableVal();
        L valueLooker = meta.<L,B>createElementWriter();
        A groupLooker = meta.<A,B>createAggregateWriter();
        protected int doneInCur = 0;
        private boolean hasNext = false;

        void reset(Node root, double skip, double total){
            grpstack.ensureCapacity(root.depth());
            idxstack.ensureCapacity(root.depth());
            grpstack.clear();
            idxstack.clear();
            remaining = total;
            hasNext = root.calculateNext(this, skip);
        }

        @Override
        public boolean hasNext() {
            return hasNext && remaining>-meta.precision;
        }

        @Override
        public R next() {
            meta.copy(next.read(), cur);
            remaining-=cur.getDistance();
            hasNext = leaf.calculateNext(this, 0);
            return cur.read();
        }
        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        join(sb, "\n");
        return sb.toString();
    }
    public void join(Appendable sb, String separator){
        Iterator<R> it = iterator();
        String sep = "";
        while(it.hasNext()){
            try {
                sb.append(sep);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sep=separator;
            stringifyPoint(sb, it.next());
        }
    }

    /**
     * @return false if undo stack was empty
     */
    public boolean undo(){
        Undo undo = undos.pollLast();
        if(undo==null) return false;

        undo.unapply();
        redos.push(undo);
        return true;
    }
    public boolean canUndo(){
        return ! undos.isEmpty();
    }
    public boolean canRedo(){
        return ! redos.isEmpty();
    }
    /**
     * @return false if undo stack was empty
     */
    public boolean redo(){
        Undo redo = redos.poll();
        if(redo==null) return false;

        redo.apply();
        undos.add(redo);
        return true;
    }
}
