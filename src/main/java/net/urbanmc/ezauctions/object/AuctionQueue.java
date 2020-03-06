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

    public int indexOf(UUID player) {
        for (int i = 0; i < queue.length; ++i) {
            Auction auction = queue[i];

            if (auction != null && auction.getAuctioneer().getUniqueId().equals(player)) {
                return distanceAwayFromHead(i);
            }
        }

        return -1;
    }

    /**
     * Remove a specific index from the queue
     *
     * @param index A modified index representing the elements position away from the head
     */
    public void remove(int index) {
        if (isEmpty() || index < 0 || index >= queue.length)
            return;

        // Shift index appropriately
        index = head + index;

        if (index >= queue.length)
            index -= queue.length;

        // Handle end cases
        if (index == head) {
            queue[head++] = null;
            return;
        }


        if (index != tail) {
            int shiftTowardsIndex = index;

            // Check if the index is between head and end of the array
            if (index > head) {
                // Shift elements from the index
                System.arraycopy(queue, index + 1, queue, index, queue.length - 1 - index);
                shiftTowardsIndex = 0;
            }

            // Check if the queue is not flattened
            if (tail < head) {
                // Cycle first element to the last element
                queue[queue.length - 1] = queue[0];

                // Copy elements from 0 to the tail.
                System.arraycopy(queue, shiftTowardsIndex + 1, queue, shiftTowardsIndex, tail - shiftTowardsIndex);
            }
        }

        queue[--tail] = null;
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
