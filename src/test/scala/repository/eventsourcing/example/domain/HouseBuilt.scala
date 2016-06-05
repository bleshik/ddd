package repository.eventsourcing.example.domain

import repository.eventsourcing.InitialEvent

case class HouseBuilt(address: String, priceInCents: Long, owner: String) extends InitialEvent[House] {
  override def initializedObject(): House =
    House.build(address, priceInCents, owner)
}
