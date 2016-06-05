package eventstore.impl

class InMemoryEventStoreSpec extends AbstractEventStoreSpec {
  it should "be thread safe" in {
    stressTest(new InMemoryEventStore)
  }
}
