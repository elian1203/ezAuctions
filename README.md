# ezAuctions
Auction plugin based off of floAuction & Auctions <br>
Requires Vault & Uses the Fanciful library

### Developers
Elian, Silverwolfg11

### Website / Server
http://urbanmc.net/

## API
#### Events: <br>
AuctionBidEvent : Cancellable <br>
AuctionCancelEvent : Cancellable <br>
AuctionEndEvent : Not cancellable <br>
AuctionImpoundEvent : Cancellable <br>
AuctionStartEvent : Cancellable <br> <br>

#### To get the current auction: <br>
EzAuctions.getAuctionManager().getCurrentAuction() <br>
<br>

#### To cancel/impound the current auction <br>
EzAuctions.getAuctionManager().getCurrentRunnable().
cancelAuction()
or .impoundAuction() respectively.