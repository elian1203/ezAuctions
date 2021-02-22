package net.urbanmc.ezauctions.object;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Minimal implementation of a cylindrical Queue backed by an array.
 *
 * Mimics a list in some ways by allowing elemental retrieval from a modified index.
 *
 * The polling operation of this queue does not shift any elements hence is very quick.
 */
public class AuctionQueue {
    protected Auction[] queue;

    private int head = 0,
                tail = 0;

    public AuctionQueue(int initialCapacity) {
        queue = new Auction[initialCapacity];
    }

    /**
     * Retrieve the auction from the queue at a certain position.
     * @param index Modified index that is n elements away from the head.
     * @return Auction at modified index.
     */
    public Auction get(int index) {
        // Check out of bounds
        if (index < 0 || index >= queue.length)
            return null;

        // If index is e.g. 2, that means 2 elements away from the head.
        int shiftedIndex = head + index;

        if (shiftedIndex >= queue.length)
            shiftedIndex -= queue.length;

        return queue[shiftedIndex];
    }

    /**
     *
     * @param index Pure index (position in the array).
     * @return Auction at pure index.
     */
    public Auction unsafeGet(int index) {
        return queue[index];
    }

    /**
     * Add an auction to the tail of the queue.
     * If the queue has insufficient capacity, it will grow to add the item.
     * If the head has reached a size constraint, then the elements will shift to the beginning of the array.
     *
     * @param auction Auction object
     */
    public void enqueue(Auction auction) {
        queue[tail++] = auction;

        if (tail == queue.length)
            tail = 0;

        if (tail == head)
            expand();
    }

    private void expand() {
        Auction[] newQueue = new Auction[queue.length + 5];

        System.arraycopy(queue, head, newQueue, 0, queue.length - head);

        if (head != 0) {
            System.arraycopy(queue, 0, newQueue, queue.length - head, head);
        }

        head = 0;
        tail = queue.length; // Insertion index

        queue = newQueue;
    }

    /**
     * Retrieve and remove the head element of the queue.
     * Does not shift any elements.
     * @return Auction at head of queue
     */
    public Auction poll() {
        if (isEmpty())
            return null;

        Auction auc = queue[head];

        queue[head++] = null;

        if (head == queue.length)
            head = 0;

        return auc;
    }

    public Auction dequeue() {
        return poll();
    }

    /**
     * @return Auction at head of queue.
     */
    public Auction peek() {
        if (isEmpty())
            return null;

        return queue[head];
    }

    public int getHeadIndex() {
        return head;
    }

    private int distanceAwayFromHead(int index) {
        int length = index - head;
        return length >= 0 ? length : queue.length + length;
    }

    public int size() {
        return distanceAwayFromHead(tail);
    }

    public boolean isEmpty() {
        return head == tail;
    }

    public void clear() {
        head = 0;
        tail = 0;
        queue = new Auction[5];
    }

    // All indexOf functions return a modified index notating the position of the element away from the head

    public int indexOf(Auction auc) {
        for (int i = 0; i < queue.length; ++i) {
            if (queue[i] == auc) {
                return distanceAwayFromHead(i);
            }
        }

        return -1;
    }

    public int indexOf(AuctionsPlayer player) {
        for (int i = 0; i < queue.length; ++i) {
            Auction auction = queue[i];

            if (auction != null && auction.getAuctioneer() == player) {
                return distanceAwayFromHead(i);
            }
        }

        return -1;
    }

    public int indexOfReverse(AuctionsPlayer player) {
        for (int i = queue.length - 1; i >= 0; i--) {
            Auction auction = queue[i];

            if (auction != null && auction.getAuctioneer() == player) {
                return distanceAwayFromHead(i);
            }
        }

        return -1;
    }

    public int indexOf(UUID player) {
        for (int i = 0; i < queue.length; ++i) {
            Auction auction = queue[i];

            if (auction != null && auction.getAuctioneer().getUniqueId().equals(player)) {
                return distanceAwayFromHead(i);
            }
        }

        return -1;
    }

    public int getNumberInQueue(AuctionsPlayer player) {
        int numberInQueue = 0;

        for (int i = 0; i < queue.length; ++i) {
            Auction auction = queue[i];

            if (auction != null && auction.getAuctioneer() == player)
                numberInQueue++;
        }

        return numberInQueue;
    }

    /**
     * Remove a specific index from the queue
     *
     * @param modifiedIndex A modified index representing the elements position away from the head
     */
    // This remove function is smart.
    // It will calculate the distance between the head and the remove index
    // and the tail and the remove index. Then depending on which one is
    // smaller, it will shift the elements accordingly.
    // Thus the worst case run time of this method is O(n / 2).
    public void remove(int modifiedIndex) {
        // Check if index is valid
        if (isEmpty() || modifiedIndex < 0 || modifiedIndex >= queue.length)
            return;

        // Determine whether the head or tail is closer to the index

        // Since a modified index is inherently the # of elements
        // away from the head, we do not have to get that value again.

        // Get how far away this index is from the tail.
        int distAwayFromTail = distanceAwayFromHead(tail) - modifiedIndex;

        // Decide whether head or tail is closer
        boolean propagateHead = distAwayFromTail > modifiedIndex;

        // Find the actual index of the value to remove.
        int anchorIndex = head + modifiedIndex;
        if (anchorIndex >= queue.length)
            anchorIndex -= queue.length;

        int distanceFromIndex = propagateHead ? modifiedIndex : distAwayFromTail;
        int directionFactor = propagateHead ? -1 : 1;

        // Shift elements towards the anchor index
        // This mean if head is closer, shift the elements to the right.
        // If tail is closer, shift the elements to the left.
        for (int i = 0; i < distanceFromIndex; i++) {
            int currIndex = anchorIndex + (directionFactor * i);
            int propagationIndex = currIndex + directionFactor;

            // Handle cylindrical aspect
            if (currIndex < 0) {
                currIndex += queue.length;
                propagationIndex += queue.length;
            }
            else if (currIndex >= queue.length) {
                currIndex -= queue.length;
                propagationIndex -= queue.length;
            }
            else if (propagationIndex < 0)
                propagationIndex += queue.length;
            else if (propagationIndex >= queue.length)
                propagationIndex -= queue.length;

            queue[currIndex] = queue[propagationIndex];
        }

        if (propagateHead)
            dequeue();
        else {
            // Tail is always empty
            tail--;
            // Make sure tail is valid
            if (tail < 0)
                tail += queue.length;

            queue[tail] = null;
        }
    }

    public void forEach(Consumer<Auction> consumer) {
        for (int i = 0; i < queue.length; ++i) {
            Auction  auction = queue[i];

            if (auction != null)
                consumer.accept(queue[i]);
        }
    }

    public Iterator<Auction> iterator() {
        return new Iterator<Auction>() {
            int cursor = head;

            @Override
            public boolean hasNext() {
                return cursor != tail;
            }

            @Override
            public Auction next() {
                Auction auc =  queue[cursor++];

                if (cursor == queue.length)
                    cursor = 0;

                return auc;
            }
        };
    }
}
