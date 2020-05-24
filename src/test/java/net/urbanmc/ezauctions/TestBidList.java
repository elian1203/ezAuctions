package net.urbanmc.ezauctions;

import net.urbanmc.ezauctions.object.BidList;
import net.urbanmc.ezauctions.object.Bidder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestBidList {

    @Test
    public void testEmptyList() {
        BidList list = new BidList();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertNull(list.get(0));
        assertNull(list.getLastBid());
    }

    @Test
    public void testSingleElementList() {
        BidList list = new BidList();
        Bidder bidder = mock(Bidder.class);

        // Test insertion
        list.add(bidder);

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(bidder, list.get(0));
        assertEquals(bidder, list.getTopBid());
        assertTrue(list.contains(bidder)); // This may not work for mocking
        assertEquals(0, list.indexOf(bidder));
        assertEquals(0, list.lastIndexOf(bidder));

        // Test removal
        assertEquals(bidder, list.remove(0));
        assertFalse(list.contains(bidder));
        assertNull(list.get(0));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals(-1, list.indexOf(bidder));
        assertEquals(-1, list.lastIndexOf(bidder));
        assertNull(list.getLastBid());
    }

    private final int ELEMENTS_TO_ADD = 30;

    @Test
    public void testAddingElements() {
        BidList list = new BidList();

        Bidder maxBid = null;
        for (int i = 0; i < ELEMENTS_TO_ADD; ++i) {
            Bidder bidder = mock(Bidder.class);
            when(bidder.getAmount()).thenReturn(ThreadLocalRandom.current().nextDouble(1, 10000));

            if (maxBid == null || bidder.getAmount() > maxBid.getAmount())
                maxBid = bidder;

            list.add(bidder);

            assertEquals(i + 1, list.size());
            assertNotEquals(-1, list.indexOf(bidder));
            assertTrue(list.contains(bidder));
            assertEquals(maxBid, list.getLastBid());
        }
    }

    private BidList generateList(int numElements, double topBid) {
        BidList list = new BidList();

        // Add top bid
        {
            Bidder bidder = mock(Bidder.class);
            when(bidder.getAmount()).thenReturn(topBid);
        }

        for (int i = 0; i < numElements; ++i) {
            Bidder bidder = mock(Bidder.class);
            when(bidder.getAmount()).thenReturn(ThreadLocalRandom.current().nextDouble(1, topBid));
            list.add(bidder);
        }

        return list;
    }

    @Test
    public void testUpdateBid() {
        BidList list = generateList(15, 100);

        Bidder bidderToUpdate = mock(Bidder.class);
        when(bidderToUpdate.getAmount()).thenReturn(50.0); // Not going to be the top bid

        list.add(bidderToUpdate);

        assertNotEquals(bidderToUpdate, list.getLastBid());

        int index = list.indexOf(bidderToUpdate);
        assertNotEquals(-1, index);

        // Now update the bid
        when(bidderToUpdate.getAmount()).thenReturn(110.0); // Going to be the top bid
        list.updateBid(index);
        assertEquals(bidderToUpdate, list.getLastBid());
    }
}
