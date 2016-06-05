package repository.eventsourcing.example.domain

import repository.eventsourcing.IdentifiedEventSourcedEntity

case class House private (id: String, address: String, priceInCents: Long, owner: String, destroyed: Boolean)
  extends IdentifiedEventSourcedEntity[House, String](id, new HouseBuilt(address, priceInCents, owner)) {

  def buy(newOwner: String): House = {
    apply(HouseBought(newOwner))
  }

  def destroy(): House = {
    apply(HouseDestroyed())
  }

  protected def when(event: HouseBought): House = {
    if (destroyed) {
      throw new IllegalStateException("You cannot buy a destroyed house")
    }
    copy(owner = event.newOwner)
  }

  protected def when(event: HouseDestroyed): House = {
    copy(destroyed = true)
  }
}

object House {
  def build(address: String, priceInCents: Long, owner: String): House =
    new House(address, address, priceInCents, owner, false)
}
