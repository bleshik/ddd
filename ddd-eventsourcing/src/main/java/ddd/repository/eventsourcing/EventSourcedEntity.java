package ddd.repository.eventsourcing;

import eventstore.Event;
import eventstore.PayloadEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

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
    static final ClassValue<Map<Class, Method>> mutatingMethods =
        new ClassValue<Map<Class, Method>>() {
        @Override
        protected Map<Class, Method> computeValue(Class<?> type) {
            Stream<Method> whenMethods = Stream.empty();
            Class<?> curClass = type;
            while(curClass != Object.class) {
                whenMethods = Stream.concat(
                    whenMethods,
                    Arrays.asList(
                        curClass.getDeclaredMethods()
                    ).stream().filter(m ->
                        m.getName().equals("when") && (m.getParameterTypes().length == 1 || m.getParameterTypes().length == 2)
                    ).map(m -> {
                        m.setAccessible(true);
                        return m;
                    })
                );
                curClass = curClass.getSuperclass();
            }
            return whenMethods.collect(
                groupingBy((m) -> (Class) m.getParameterTypes()[0],
                    collectingAndThen(toList(), (list) -> list.get(0))
                )
            );
        }
    };

    static final ClassValue<Map<Class, Constructor>> constructors =
        new ClassValue<Map<Class, Constructor>>() {
        @Override
        protected Map<Class, Constructor> computeValue(Class<?> type) {
            Stream<Constructor> constructors = Stream.empty();
            Class<?> curClass = type;
            while(curClass != Object.class) {
                constructors = Stream.concat(
                    constructors,
                    Arrays.asList(curClass.getConstructors())
                    .stream()
                    .filter(c -> c.getParameterTypes().length == 1)
                    .map(c -> { c.setAccessible(true); return c; })
                );
                curClass = curClass.getSuperclass();
            }
            return constructors.collect(
                groupingBy((m) -> (Class) m.getParameterTypes()[0], collectingAndThen(toList(), (list) -> list.get(0)))
            );
        }
    };

    /**
     * Adds the initial event to the list of changes, checking that there is the corresponding constructor, so that the
     * entity could be initialized by the given event.
     * @param initialEvent event to be used for the initialization
     */
    protected EventSourcedEntity(Event initialEvent) {
        Map<Class, Constructor> constructors = EventSourcedEntity.constructors.get(this.getClass());
        if (constructors.containsKey(initialEvent.getClass())) {
            _mutatingChanges.add(initialEvent);
        } else if (initialEvent instanceof PayloadEvent) {
            Object payload = ((PayloadEvent) initialEvent).payload;
            if (constructors.containsKey(payload.getClass())) {
                _mutatingChanges.add(initialEvent);
            } else {
                throw new IllegalArgumentException("The entity does not have a constructor for the payload " + payload);
            }
        } else {
            throw new IllegalArgumentException("The entity does not have a constructor for the event " + initialEvent);
        }
    }

    /**
     * Wraps the given POJO into an event object, adds it to the list of changes, checking that there is the
     * corresponding constructor, so that the entity could be initialized by the given POJO.
     * @param initialEvent POJO to be used for the initialization
     */
    protected EventSourcedEntity(Object initialEvent) {
        this(new PayloadEvent(initialEvent));
    }

    /**
     * Applies the event changes. This will add the event into the list of changes, and mutate the entity.
     * @return the entity with the applied changes
     */
    public T apply(Event event) {
        return getMutatingMethod(event.getClass())
            .map((e) -> {
                Event occurredEvent = event.occurred(getMutatedVersion() + 1);
                return appendEvent(mutate(occurredEvent), occurredEvent);
            })
            .orElseGet(() -> {
                if (event instanceof PayloadEvent) {
                    PayloadEvent occurredEvent = (PayloadEvent) event.occurred(getMutatedVersion() + 1);
                    return appendEvent(mutate(occurredEvent.payload), occurredEvent);
                } else {
                    return (T) this;
                }
            });
    }

    /**
     * Applies the POJO event changes. This will add the event into the list of changes, and mutate the entity.
     * @return the entity with the applied changes
     */
    public T apply(Object event) {
        return apply(new PayloadEvent(event));
    }

    private T appendEvent(EventSourcedEntity mutatedEntity, Event event) {
        mutatedEntity._mutatingChanges  = new ArrayList<Event>(_mutatingChanges.size() + 1) {{ addAll(_mutatingChanges); add(event); }};
        mutatedEntity._version          = event.getStreamVersion();
        mutatedEntity._committedVersion = this._committedVersion;
        mutatedEntity._updateDate       = System.currentTimeMillis();
        return (T) mutatedEntity;
    }

    /**
     * @return version including the unsaved changes
     */
    public long getMutatedVersion() { return _version; }

    /**
     * @return version NOT including the unsaved changes, i.e. version of the saved entity
     */
    public long getUnmutatedVersion() { return _committedVersion; }

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

    private T mutate(Object event) {
        if (event == null) {
            return (T) this;
        }
        return getMutatingMethod(event.getClass()).map((when) -> doMutate(when, event)).orElse((T) this);
    }

    private Optional<Method> getMutatingMethod(Class eventClass) {
        return Optional.ofNullable(mutatingMethods.get(this.getClass()).get(eventClass));
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

    private T doMutate(Method when, Object event) {
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
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new EventSourcingException(String.format("Exception occurred while applying the event %s on the entity %s.", event, this), e.getCause());
        }
    }
}
