package ddd.repository.eventsourcing;

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import ddd.eventstore.Event;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.collectingAndThen;

/**
 * Entity storing mutating events. The main declared method here is {@link #apply(Event)}. Every mutating method
 * should create an event and invoke the apply method, declaring a handling <b>when</b> method.
 * For example:
 * <pre>
 * <code>
 *  class House extends EventSourcedEntity<House, String> {
 *      //...
 *      House buy(String newOwner) {
 *          return apply(new HouseBought(newOwner));
 *      }
 *
 *      protected House when(HouseBought event) {
 *          this.owner = event.newOwner;
 *          return this;
 *      }
 *
 *      // or you can change the cloned "this"
 *      protected void when(HouseBought event, House clonedHouse) {
 *          clonedHouse.owner = event.newOwner;          
 *      }
 *      //...
 *  }
 * </code>
 * </pre>
 * The <b>when</b> method returns instance of the same class: either a new one, if you like immutability, or this, if
 * you don't. Alternatively, you can use the 2-paramater <b>when</b> method, where the second parameter is simply a
 * cloned "this", where you can apply all the required changes. Latter option ingores the returned value.
 * Note, that all events should extend the base {@link Event} class.
 * @see Event
 */
@SuppressWarnings("unchecked")
public abstract class EventSourcedEntity<T extends EventSourcedEntity<T>> implements Cloneable {
    public static final String MUTATE_METHOD_NAME = "when";
    private List<Event> _mutatingChanges = new ArrayList<>();
    private long _version = 1;
    private long _updateDate = System.currentTimeMillis();
    private long _committedVersion = 0;
    private static ConcurrentMap<Class<? extends EventSourcedEntity>, Map<Class<? extends Event>, Method>> mutatingMethods =
        new ConcurrentHashMap<>();

    protected EventSourcedEntity(InitialEvent<T> initialEvent) {
        _mutatingChanges.add(initialEvent);
        // cache the when methods
        mutatingMethods.computeIfAbsent(this.getClass(), (c) -> {
            Stream<Method> whenMethods = Stream.empty();
            Class<?> curClass = c;
            while(curClass != Object.class) {
                whenMethods = Stream.concat(
                    whenMethods,
                    Arrays.asList(
                        curClass.getDeclaredMethods()
                    ).stream().filter(m ->
                        m.getName().equals("when") && (m.getParameterTypes().length == 1 || m.getParameterTypes().length == 2)
                    )
                );
                curClass = curClass.getSuperclass();
            }
            return whenMethods.collect(
                groupingBy((m) -> (Class<? extends Event>) m.getParameterTypes()[0],
                    collectingAndThen(toList(), (list) -> list.get(0))
                )
            );
        });
    }

    /**
     * Applies the event changes.
     * @return the entity with the applied changes
     */
    public T apply(Event event) {
        EventSourcedEntity mutatedEntity = mutate(event);
        mutatedEntity._mutatingChanges = new ArrayList<Event>(_mutatingChanges.size() + 1) {{ addAll(_mutatingChanges); add(event); }};
        mutatedEntity._version = this._version + 1;
        mutatedEntity._committedVersion = this._committedVersion;
        mutatedEntity._updateDate = System.currentTimeMillis();
        return (T) mutatedEntity;
    }

    /**
     * @return version including the unsaved changes
     */
    public long getMutatedVersion() { return _version; }

    /**
     * @return version NOT including the unsaved changes, i.e. version of the saved entity
     */
    public long getUnmutatedVersion() { return _version - getChanges().size(); }

    /**
     * @return timestamp of the last change of the entity
     */
    public long getUpdateDate() { return _updateDate; }

    /**
     * @return unsaved changes
     */
    public List<Event> getChanges() {
        return Collections.unmodifiableList(_mutatingChanges);
    }

    private T mutate(Event event) {
        if (event == null) {
            return (T) this;
        }
        Method when = mutatingMethods.get(this.getClass()).get(event.getClass());
        if (when == null) {
            return (T) this;
        }
        when.setAccessible(true);
        return doMutate(when, event);
    }

    T commitChanges() {
        try {
            EventSourcedEntity entity = (EventSourcedEntity) this.clone();
            entity._mutatingChanges = new ArrayList<>();
            entity._version = this._version;
            entity._committedVersion = this._version;
            return (T) entity;
        } catch(CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private T doMutate(Method when, Event event) {
        try {
            if (when.getParameterTypes().length == 1) {
                return (T) when.invoke(this, event);
            } else {
                T cloned = (T) this.clone();
                when.invoke(this, event, cloned);
                return cloned;
            }
        } catch(IllegalAccessException|CloneNotSupportedException e) {
            throw new AssertionError("This shouldn't happen");
        } catch(InvocationTargetException e) {
            throw new EventSourcingException(String.format("Exception occurred while applying the event %s on the entity %s.", event, this), e.getCause());
        }
    }
}
