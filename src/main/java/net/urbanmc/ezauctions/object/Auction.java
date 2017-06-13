package net.urbanmc.ezauctions.object;


import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class Auction {

    private ItemStack item;
    private int amount;
    private int starting;
    private double increase;
    private double lastBid;
    private Player lastBidder;

    public Auction(ItemStack item, int amount, int starting, double increase) {
        this.item = item;
        this.amount = amount;
        this.starting = starting;
        this.increase = increase;
        startAuction();
    }

    private void startAuction() {

    }


    public void setLastBid(double bid) {
        lastBid = bid;
    }

    public double getLastBid() {
        return lastBid;
    }

    public void setLastBidder(Player p) {
        lastBidder = p;
    }

    public Player getLastBidder() {
        return lastBidder;
    }


}
