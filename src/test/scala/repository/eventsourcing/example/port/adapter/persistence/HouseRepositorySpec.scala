package repository.eventsourcing.example.port.adapter.persistence

import java.util.Optional
import eventstore.impl.InMemoryEventStore
import org.scalatest._
import repository.eventsourcing.example.domain.{House, HouseRepository}
import repository.eventsourcing.EventSourcingConflictException

class HouseRepositorySpec extends FlatSpec with Matchers {
  val awesomeHouse = House.build("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")

  "remove" should "remove object from repository" in {
    List(
      new EventSourcedHouseRepository(new InMemoryEventStore)
    ).foreach(removeHouseScenario)
  }

  "get" should "return previously put object" in {
    List(
      new EventSourcedHouseRepository(new InMemoryEventStore)
    ).foreach(successHouseScenario)
  }

  "conflict" should "be handled properly" in {
    List(
      new EventSourcedHouseRepository(new InMemoryEventStore)
    ).foreach(failedHouseScenario)
  }

  private def removeHouseScenario(houseRepository: HouseRepository): Unit = {
    houseRepository.contained(awesomeHouse.address) should be(right = false)
    houseRepository.save(awesomeHouse)
    houseRepository.remove(awesomeHouse.address) should be(right = true)
    houseRepository.get(awesomeHouse.address) should be(Optional.empty())
    houseRepository.contains(awesomeHouse.address) should be(right = false)
    houseRepository.contained(awesomeHouse.address) should be(right = true)
  }

  private def successHouseScenario(houseRepository: HouseRepository): Unit = {
    var house = awesomeHouse
    houseRepository.save(house)
    houseRepository.size should be(1)
    houseRepository.get(house.address).get should be(house)

    house = house.buy("Stepan Stepanov")
    houseRepository.save(house)
    houseRepository.get(house.address).get.owner should be("Stepan Stepanov")

    house = house.destroy()
    houseRepository.save(house)
    houseRepository.get(house.address).get.destroyed should be(right = true)
    houseRepository.get(house.address).get.owner should be("Stepan Stepanov")
  }
  
  private def failedHouseScenario(houseRepository: HouseRepository): Unit = {
    houseRepository.save(awesomeHouse.destroy())
    a [EventSourcingConflictException] shouldBe thrownBy {
      houseRepository.save(awesomeHouse.buy("Stepan Stepanov"))
    }
  }

}
