package net.urbanmc.ezauctions.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class mimics a PriorityQueue however the tail element is the bid with the most value.
 * Any elements before the tail are out of order, but are guaranteed to be bids below the tail element amount.
 * Furthermore, this class offers an update method for bids to be updated.
 *
 * The way this list prioritizes is through a single swap to determine highest bid value on elemental add.
 *
 * This class also has a mostly complete implementation of a normal list interface.
 */

public class BidList implements List<Bidder> {

    // Default growth factor
    protected static int GROWTH = 5;

    protected Bidder[] bidders;
    private int size = 0;

    public BidList() {
        bidders = new Bidder[5]; // Initial Capacity is 5
    }

    @Override
    public Bidder get(int index) {
        if (index < 0 || index >= size)
            return null;

        return bidders[index];
    }

    public Bidder get(AuctionsPlayer auctionPlayer) {
        for (int i = 0; i < size; ++i) {
            if (bidders[i].getBidder() == auctionPlayer)
                return bidders[i];
        }

        return null;
    }

    @Override
    public Bidder set(int index, Bidder element) {
        if (index < 0 || index >= size)
            return null;

        Bidder oldValue = bidders[index];

        Bidder topBid = bidders[size - 1];

        if (element.getAmount() > topBid.getAmount()) {
            bidders[index] = topBid;
            bidders[size - 1] = element;
        }
        else {
            bidders[index] = element;
        }

        return oldValue;
    }

    /**
     * Add an object to the bid list.
     * This method does not guarantee that the bidder object will be the last element in the list.
     * If the bidder amount is less than the top bidder amount, then it will be the second to last.
     *
     * @param bidder Bidder object to be added
     * @return whether the object was successfully added to the the list
     */
    @Override
    public boolean add(Bidder bidder) {
        if (bidders.length == size)
            grow(GROWTH);

        Bidder lastBid;

        if (size > 0 && (lastBid = bidders[size - 1]).getAmount() > bidder.getAmount()) {
            bidders[size - 1] = bidder;
            bidders[size++] = lastBid;
        }
        else {
            bidders[size++] = bidder;
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Bidder> c) {
        if (bidders.length < (size + c.size())) {
            grow(c.size());
        }

        for (Bidder bidder : c)
            add(bidder);

        return true;
    }

    @Override
    public void add(int index, Bidder element) {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public boolean addAll(int index, Collection<? extends Bidder> c) {
        throw new UnsupportedOperationException("Not supported!");
    }

    private void grow(int growthSize) {
        Bidder[] a = new Bidder[bidders.length + growthSize];

        System.arraycopy(bidders, 0, a, 0, bidders.length);

        bidders = a;
    }

    public void updateBid(int index) {
        if (index < 0 || index >= (size - 1))
            return;

        Bidder updatedBid = bidders[index];
        Bidder lastBid = bidders[size - 1];

        // Check if the updated bid is now greater than the last bid
        if (updatedBid.getAmount() > lastBid.getAmount()) {
            bidders[size - 1] = updatedBid;
            bidders[index] = lastBid;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void clear() {
        size = 0;
        bidders = new Bidder[5];
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Bidder))
            return false;

        Bidder bidder = (Bidder) o;

        if (size == 0)
            return false;

        if (bidder != getLastBid()) {
            int index = indexOf(bidder);

            if (index == -1)
                return false;

            shiftTowards(index);
        }

        bidders[--size] = null;
        return true;
    }

    @Override
    public Bidder remove(int index) {
        if (index < 0 || index >= size)
            return null;

        Bidder bidder = bidders[index];

        if (index != (size - 1))
            shiftTowards(index);

        bidders[--size] = null;
        return bidder;
    }

    private void shiftTowards(int index) {
        System.arraycopy(bidders, index + 1, bidders, index, size - 1 - index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported!");
    }


    @Override
    public ListIterator<Bidder> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Bidder> listIterator(int index) {
        return new ListIterator<Bidder>() {

            private int cursor = index;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public Bidder next() {
                return bidders[cursor++];
            }

            @Override
            public boolean hasPrevious() {
                return cursor >= 0;
            }

            @Override
            public Bidder previous() {
                return bidders[cursor--];
            }

            @Override
            public int nextIndex() {
                return cursor + 1;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                BidList.this.remove(cursor);
            }

            @Override
            public void set(Bidder bidder) {
                BidList.this.set(cursor, bidder);
            }

            @Override
            public void add(Bidder bidder) {
                BidList.this.add(bidder);
            }
        };
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public Bidder[] toArray() {
        Bidder[] b = new Bidder[size];
        System.arraycopy(bidders, 0, b, 0, size);
        return b;
    }

    @Override
    public Iterator<Bidder> iterator() {
        return new Iterator<Bidder>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return (cursor < size);
            }

            @Override
            public Bidder next() {
                return bidders[cursor++];
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Bidder))
            return false;

        Bidder object = (Bidder) o;

        for (int i = 0; i < size; ++i) {
            Bidder bidder = bidders[i];

            if (bidder == object || (bidder.getBidder() == object.getBidder() && bidder.getAmount() == object.getAmount()))
                return true;
        }

        return false;
    }

    public boolean contains(UUID id) {
        for (int i = 0; i < size; ++i) {
            if (bidders[i].getBidder().getUniqueId().equals(id))
                return true;
        }

        return false;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Bidder))
            return -1;

        Bidder object = (Bidder) o;

        for (int i = size - 1; i >= 0; --i) {
            Bidder bidder = bidders[i];

            if (bidder == object || (bidder.getBidder() == object.getBidder() && bidder.getAmount() == object.getAmount()))
                return i;
        }

        return -1;
    }

    @Override
    public int indexOf(Object o) {
        if (o instanceof Bidder) {
            return indexOf((Bidder) o);
        }

        return -1;
    }

    public int indexOf(Bidder object) {
        for (int i = 0; i < size; ++i) {
            Bidder bidder = bidders[i];

            if (bidder == object || (bidder.getBidder() == object.getBidder() && bidder.getAmount() == object.getAmount()))
                return i;
        }

        return -1;
    }

    public int indexOf(AuctionsPlayer player) {
        for (int i = 0; i < size; ++i) {
            if (bidders[i].getBidder() == player)
                return i;
        }

        return -1;
    }

    public int indexOf(UUID id) {
        for (int i = 0; i < size; ++i) {
            if (bidders[i].getBidder().getUniqueId().equals(id))
                return i;
        }

        return -1;
    }

    public Bidder getTopBid() {
        return getLastBid();
    }

    public Bidder getLastBid() {
        if (size > 0)
            return bidders[size - 1];

        return null;
    }

    @Override
    public List<Bidder> subList(int fromIndex, int toIndex) {
        return toArrayList(fromIndex, toIndex);
    }

    public List<Bidder> toArrayList() {
        return toArrayList(0, size);
    }

    public List<Bidder> toArrayList(int startingPos, int endingPos) {
        if (startingPos < 0)
            startingPos = 0;

        if (endingPos > size)
            endingPos = size;

        List<Bidder> bidList = new ArrayList<>(endingPos - startingPos);

        for (int i = startingPos; i < endingPos; ++i) {
            bidList.add(bidders[i]);
        }

        return bidList;
    }

    @Override
    public void forEach(Consumer<? super Bidder> consumer) {
        forEach(consumer, 0, size);
    }

    public void forEach(Consumer<? super Bidder> consumer, int startingPos, int endingPos) {
        if (startingPos < 0)
            startingPos = 0;

        if (endingPos > size)
            endingPos = size;

        for (int i = startingPos; i < endingPos; ++i) {
            consumer.accept(bidders[i]);
        }
    }
}
