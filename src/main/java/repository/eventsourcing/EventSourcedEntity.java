package repository.eventsourcing;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import eventstore.api.Event;

public abstract class EventSourcedEntity<T extends EventSourcedEntity<T>> implements Cloneable {
    private static final String MUTATE_METHOD_NAME = "when";
    private List<Event> _mutatingChanges = new ArrayList<>();
    private long _version = 1;
    private long _updateDate = System.currentTimeMillis();
    private long _committedVersion = 0;
    private InitialEvent<T> _initialEvent;

    protected EventSourcedEntity(InitialEvent<T> initialEvent) {
        this._initialEvent = initialEvent;
    }

    public T apply(Event event) {
        EventSourcedEntity mutatedEntity = mutate(event);
        mutatedEntity._mutatingChanges = new ArrayList<Event>() {{ addAll(_mutatingChanges); add(event); }};
        mutatedEntity._version = this._version + 1;
        mutatedEntity._committedVersion = this._committedVersion;
        mutatedEntity._updateDate = System.currentTimeMillis();
        mutatedEntity._initialEvent = this._initialEvent;
        return (T) mutatedEntity;
    }

    public long getMutatedVersion() { return _version; }

    public long getUnmutatedVersion() { return _version - getChanges().size(); }

    public long getUpdateDate() { return _updateDate; }

    public List<Event> getChanges() {
        if ((_version - _mutatingChanges.size() == 1) && _committedVersion == 0) {
            return Collections.unmodifiableList(new ArrayList<Event>() {{ add(_initialEvent); addAll(_mutatingChanges); }});
        } else {
            return Collections.unmodifiableList(_mutatingChanges);
        }
    }

    private T mutate(Event event) {
        if (event == null) {
            return (T) this;
        }
        Method when = findMutatingMethod(event.getClass());
        if (when == null) {
            return (T) this;
        }
        when.setAccessible(true);
        try {
            return (T) when.invoke(this, event);
        } catch(IllegalAccessException e) {
            throw new AssertionError();
        } catch(InvocationTargetException e) {
            throw new EventSourcingException(String.format("Exception occurred while applying the event %s on the entity %s.", event, this), e.getCause());
        }
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

    private Method findMutatingMethod(Class<? extends Event> eventClass) {
        //TODO: cache it
        try {
            return this.getClass().getDeclaredMethod(MUTATE_METHOD_NAME, eventClass);
        } catch(NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
