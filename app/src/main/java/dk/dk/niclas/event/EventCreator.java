package dk.dk.niclas.event;

import org.greenrobot.eventbus.EventBus;
import dk.dk.niclas.event.events.MestSeteEvent;
import dk.dk.niclas.event.events.NetværksFejlEvent;


public class EventCreator {
    private EventBus bus = EventBus.getDefault();

    public void mestSete(boolean fraCache, boolean uændret) {
        MestSeteEvent event = new MestSeteEvent(fraCache, uændret);
        bus.post(event);
    }

    public void netværksFejl(){
        NetværksFejlEvent event = new NetværksFejlEvent();
        bus.post(event);
    }
}
