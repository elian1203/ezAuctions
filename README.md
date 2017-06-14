# ezAuctions
Auction plugin based off of floAuction & Auctions

# Developers
Elian, Silverwolfg11

# Website / Server
http://urbanmc.net/

# API
<b>Events:</b> <br>
AuctionBidEvent : Cancellable <br>
AuctionCancelEvent : Cancellable <br>
AuctionEndEvent : Not cancellable <br>
AuctionImpoundEvent : Cancellable <br>
AuctionStartEvent : Cancellable <br> <br>

<b>To get the current auction:</b> <br>
EzAuctions.getAuctionManager().getCurrentAuction() <br>
<br>

<b>To cancel/impound the current auction</b> <br>
EzAuctions.getAuctionManager().getCurrentRunnable().
cancelAuction()
or .impoundAuction() respectively.