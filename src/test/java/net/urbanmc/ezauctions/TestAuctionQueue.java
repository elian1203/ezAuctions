package net.urbanmc.ezauctions;

import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionQueue;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestAuctionQueue {

    @Test
    public void testEmptyQueue() {
        AuctionQueue auctionQueue = new AuctionQueue(1);
        // Test on Empty Queue
        assertEquals(0, auctionQueue.size());
        assertTrue(auctionQueue.isEmpty());
        assertNull(auctionQueue.poll());
        assertNull(auctionQueue.get(5));
    }

    @Test
    public void testOneElementQueue() {
        AuctionQueue auctionQueue = new AuctionQueue(1);
        // Test queue with one item
        Auction auction = mock(Auction.class);
        auctionQueue.enqueue(auction);
        assertEquals(1, auctionQueue.size());
        assertFalse(auctionQueue.isEmpty());
        assertEquals(auction, auctionQueue.get(0));
        assertEquals(auction, auctionQueue.peek());
        assertEquals(auction, auctionQueue.poll());
    }

    private final int ELEMENTS_TO_ENQUEUE = 10;
    @Test
    public void testEnqueuingElements() {
        AuctionQueue auctionQueue = new AuctionQueue(ELEMENTS_TO_ENQUEUE);

        // Test Enqueing with a number of elements
        assertTrue(auctionQueue.isEmpty()); // Make sure auction queue is empty before we start
        for (int i = 0; i < ELEMENTS_TO_ENQUEUE; i++) {
            Auction auc = mock(Auction.class);
            auctionQueue.enqueue(auc);
            assertEquals(i + 1, auctionQueue.size());
            assertEquals(i, auctionQueue.indexOf(auc));
            assertEquals(auc, auctionQueue.get(i));

            // Test peek for first element
            if (i == 0)
                assertEquals(auc, auctionQueue.peek());
        }
    }

    private AuctionQueue generateQueue() {
        return generateQueue(ELEMENTS_TO_ENQUEUE);
    }

    private AuctionQueue generateQueue(int numElements) {
        AuctionQueue auctionQueue = new AuctionQueue(numElements);
        for (int i = 0; i < ELEMENTS_TO_ENQUEUE; i++) {
            Auction auc = mock(Auction.class);
            auctionQueue.enqueue(auc);
        }
        return auctionQueue;
    }

    private final int ELEMENTS_TO_POLL = 2;

    @Test
    public void testPollingElements() {
        AuctionQueue auctionQueue = generateQueue();
        // Poll some elements
        for (int i = 0; i < ELEMENTS_TO_POLL; ++i) {
            Auction aucPolled = auctionQueue.get(0);
            assertEquals(aucPolled, auctionQueue.peek());
            assertEquals(aucPolled, auctionQueue.poll());
        }

        assertEquals(ELEMENTS_TO_ENQUEUE - ELEMENTS_TO_POLL, auctionQueue.size());
    }

    @Test
    public void testRemovingFirstElement() {
        int auctionQueueSize = ELEMENTS_TO_ENQUEUE;
        AuctionQueue auctionQueue = generateQueue(auctionQueueSize);

        // Test the remove function
        // Test removing first elements
        Auction firstElement = auctionQueue.get(0);
        // Make sure element is not null
        assertNotNull(firstElement);
        // Make sure indexOf actually works
        assertEquals( 0, auctionQueue.indexOf(firstElement));
        // Make sure it's actually the first element
        assertEquals(firstElement, auctionQueue.peek());
        // Remove the element
        auctionQueue.remove(0);
        auctionQueueSize--;
        // Make sure that peek returns something different
        assertNotEquals(firstElement, auctionQueue.peek());
        // Check size
        assertEquals(auctionQueueSize, auctionQueue.size());
        // Make sure that indexOf does not return an index
        assertEquals(-1, auctionQueue.indexOf(firstElement));
    }

    @Test
    public void testRemovingLastElement() {
        int auctionQueueSize = ELEMENTS_TO_ENQUEUE;
        AuctionQueue auctionQueue = generateQueue(auctionQueueSize);

        // Test removing the last element
        int lastElementIndex = auctionQueue.size() - 1;
        Auction lastElement = auctionQueue.get(lastElementIndex);
        assertNotNull(lastElement);
        auctionQueue.remove(lastElementIndex);
        auctionQueueSize--;
        assertEquals(auctionQueueSize, auctionQueue.size());
        assertEquals(-1, auctionQueue.indexOf(lastElement));
    }

    @Test
    public void testRemovingRandomElement() {
        int auctionQueueSize = ELEMENTS_TO_ENQUEUE;
        AuctionQueue auctionQueue = generateQueue(auctionQueueSize);

        // Remove a random element.
        int indexToRemove = 2; // https://xkcd.com/221/
        assertTrue(auctionQueue.size() > indexToRemove); // Make sure auction queue can remove the index
        Auction randomElement = auctionQueue.get(indexToRemove);
        assertNotNull(randomElement);
        assertEquals(indexToRemove, auctionQueue.indexOf(randomElement));
        auctionQueue.remove(indexToRemove);
        auctionQueueSize--;
        assertEquals(auctionQueueSize, auctionQueue.size());
        assertEquals(-1, auctionQueue.indexOf(randomElement));
    }
}
