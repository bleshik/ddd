package repository.eventsourcing;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eventstore.api.Event;

public abstract class EventSourcedEntity<T extends EventSourcedEntity<T>> implements Cloneable {
    private static final String MUTATE_METHOD_NAME = "when";
    private List<Event> _mutatingChanges = new ArrayList<>();
    private long _version = 1;
    private long _updateDate = System.currentTimeMillis();
    private long _committedVersion = 0;
    private InitialEvent<T> initialEvent;

    protected EventSourcedEntity(InitialEvent<T> initialEvent) {
        this.initialEvent = initialEvent;
    }

    public T apply(Event event) throws Exception {
        EventSourcedEntity mutatedEntity = mutate(event);
        mutatedEntity._mutatingChanges = new ArrayList<Event>() {{ addAll(_mutatingChanges); add(event); }};
        mutatedEntity._version = this._version + 1;
        mutatedEntity._committedVersion = this._committedVersion;
        mutatedEntity._updateDate = System.currentTimeMillis();
        return (T) mutatedEntity;
    }

    private T mutate(Event event) throws Exception {
        if (event == null) {
            return (T) this;
        }
        Method when = findMutatingMethod(event.getClass());
        if (when == null) {
            return (T) this;
        }
        try {
            return (T) when.invoke(this, event);
        } catch(IllegalAccessException e) {
            throw new AssertionError();
        } catch(InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw ((Exception) cause);
            } else {
                throw ((Error) cause);
            }
        }
    }

    public long getMutatedVersion() { return _version; }

    public long getUnmutatedVersion() { return _version - getChanges().size(); }

    public long getUpdateDate() { return _updateDate; }

    public List<Event> getChanges() {
        if ((_version - _mutatingChanges.size() == 1) && _committedVersion == 0) {
            return Collections.unmodifiableList(new ArrayList<Event>() {{ add(initialEvent); addAll(_mutatingChanges); }});
        } else {
            return Collections.unmodifiableList(_mutatingChanges);
        }
    }

    public T commitChanges() {
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
            return this.getClass().getMethod(MUTATE_METHOD_NAME, eventClass);
        } catch(NoSuchMethodException e) {
            return null;
        }
    }
}
