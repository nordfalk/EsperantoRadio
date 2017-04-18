package dk.dk.niclas.event.events;

import dk.dr.radio.data.Udsendelse;

/**
 * Created by Yoouughurt on 18-04-2017.
 */

public class StreamsParsedEvent extends AbstractEvent {

    private Udsendelse udsendelse;

   public StreamsParsedEvent(boolean fraCache, boolean uændret, Udsendelse udsendelse) {
        super(fraCache, uændret);
       this.udsendelse = udsendelse;
    }

    public Udsendelse getUdsendelse(){
        return udsendelse;
    }

}
