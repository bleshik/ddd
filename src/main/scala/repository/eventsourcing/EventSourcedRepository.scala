package repository.eventsourcing

import java.util.Optional
import java.lang.Math.min
import java.lang.reflect.{Constructor, ParameterizedType}
import java.util.ConcurrentModificationException

import com.typesafe.scalalogging.LazyLogging
import eventstore.api.{Event, EventStore}
import repository.IdentifiedEntity
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.compat.java8.StreamConverters._

abstract class EventSourcedRepository[T <: EventSourcedEntity[T] with IdentifiedEntity[K], K](val eventStore: EventStore)
  extends TemporalPersistenceOrientedRepository[T, K] with LazyLogging {
  override def get(id: K): Optional[T] = {
    get(id, -1)
  }

  private def get(id: K, version: Long): Optional[T] = {
    getByStreamName(streamName(id), version, snapshot(id, version)).asJava
  }

  private def getByStreamName(streamName: String, version: Long, snapshot: Option[T] = None): Option[T] = {
    eventStore.streamSince(streamName, snapshot.map(e => e.getUnmutatedVersion()).getOrElse(-1)).asScala.map(_.toScala[Stream]).flatMap { events =>
      if (events.isEmpty || events.lastOption.exists(_.isInstanceOf[RemovedEvent[K]])) {
        return snapshot
      }
      var entity = snapshot.getOrElse(init(events.head))
      var eventsRemaining = if (snapshot.isEmpty) events.tail else events
      while (entity.getMutatedVersion() != version && eventsRemaining.nonEmpty) {
        entity = entity.apply(eventsRemaining.head)
        eventsRemaining = eventsRemaining.tail
      }
      Some(entity.commitChanges())
    }
  }

  protected def saveSnapshot(entity: T): Unit = {}

  protected def removeSnapshot(id: K): Boolean = { false }

  protected def snapshot(id: K, before: Long): Option[T] = { None }

  private def init(initEvent: Event): T = {
    initEvent.asInstanceOf[InitialEvent[T]].initializedObject()
  }

  private def getAndApply(id: K, after: Long, changes: List[Event]): Option[T] = {
    if (after <= 0) {
      getAndApply(id, 1, changes.tail)
    } else {
      get(id, after).asScala match {
        case Some(entity) =>
          var mutatedEntity = entity.asInstanceOf[T]
          eventStore.streamSince(streamName(id), after).asScala.map(_.toScala[Stream]).flatMap { events =>
            var i: Int = 0
            while (min(changes.length, events.length) > i && changes(i).equals(events(i))) {
              mutatedEntity = mutatedEntity.apply(events(i))
              i += 1
            }
            events.takeRight(events.length - i).foreach { e => mutatedEntity = mutatedEntity.apply(e) }
            mutatedEntity = mutatedEntity.commitChanges()
            changes.takeRight(changes.length - i).foreach { e => mutatedEntity = mutatedEntity.apply(e) }
            Some(mutatedEntity)
          }
        case None => None
      }
    }
  }

  override def save(entity: T): T = {
    if (!entity.getChanges().iterator().hasNext()) {
      entity
    } else {
      try {
        eventStore.append(streamName(entity.getId()), entity.getUnmutatedVersion(), entity.getChanges())
        try {
          saveSnapshot(entity)
        } catch {
          case e: Exception => logger.error("Couldn't save snapshot", e)
        }
      } catch {
        case e: ConcurrentModificationException =>
          val freshEntity = getAndApply(entity.getId(), entity.getUnmutatedVersion(), entity.getChanges().asScala.toList).get
          if (freshEntity.getUnmutatedVersion() == entity.getUnmutatedVersion()) {
            throw new IllegalStateException(s"Couldn't resolve the conflict, the saved entity ${entity} (${entity.getUnmutatedVersion()}, ${entity.getChanges()}), but got fresh ${freshEntity} (${eventStore.version(streamName(entity.getId()))}).", e)
          }
          save(freshEntity)
      }
      entity
    }
  }

  override def size: Long = {
    eventStore.size
  }

  override def all : java.util.Set[T] = {
    eventStore.streamNames.asScala.flatMap(s => getByStreamName(s, -1)).toSet.asJava
  }

  protected def entityClass: Class[T] = {
    this.getClass
      .getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments()(0)
      .asInstanceOf[Class[T]]
  }

  override def remove(id: K): Boolean = {
    if (!contains(id)) {
      return false
    }
    try {
      eventStore.append(streamName(id), new RemovedEvent[K](id))
      removeSnapshot(id)
      true
    } catch {
      case e: ConcurrentModificationException => remove(id)
    }
  }

  /**
   * Whether the repository had the given entity.
   * @param id id of the entity.
   * @return true, if it contained the entity.
   */
  override def contained(id: K): Boolean = eventStore.contains(streamName(id))

  private def constructor(eventClass: Class[_ <: Event]): Constructor[T] = {
    val c =  entityClass
      .getConstructor(eventClass)
      .asInstanceOf[Constructor[T]]
    c.setAccessible(true)
    c
  }

  protected def streamName(id: K): String = {
     streamPrefix + id
  }

  private def streamPrefix: String = {
    this.entityClass.getSimpleName
  }
}
