package dk.dk.niclas.event.events;

/**
 * This class defines the needed attributes for events to describe the responses from Volley.
 * Should be subclassed by all events that occurs from data retrieval with Volley.
 */

abstract class AbstractEvent {

    private boolean fraCache;
    private boolean uændret;

    AbstractEvent(boolean fraCache, boolean uændret){
        this.fraCache = fraCache;
        this.uændret = uændret;
    }

    public boolean isFraCache() {
        return fraCache;
    }

    public boolean isUændret() {
        return uændret;
    }
}
