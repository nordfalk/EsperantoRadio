package dk.dk.niclas.event;

import org.greenrobot.eventbus.EventBus;
import dk.dk.niclas.event.events.MestSeteEvent;
import dk.dk.niclas.event.events.NetværksFejlEvent;
import dk.dk.niclas.event.events.NowNextAlleKanalerEvent;
import dk.dk.niclas.event.events.NowNextKanalEvent;
import dk.dk.niclas.event.events.StreamsParsedEvent;
import dk.dr.radio.data.Udsendelse;


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

    public void streamsParsedEvent(boolean fraCache, boolean uændret, Udsendelse udsendelse){
        StreamsParsedEvent event = new StreamsParsedEvent(fraCache, uændret, udsendelse);
        bus.post(event);
    }

    public void nowNextKanalEvent(boolean fraCache, boolean uændret, String kanalSlug) {
        NowNextKanalEvent event = new NowNextKanalEvent(fraCache, uændret, kanalSlug);
        bus.post(event);
    }

    public void nowNextAlleKanalerEvent(boolean fraCache, boolean uændret) {
        NowNextAlleKanalerEvent event = new NowNextAlleKanalerEvent(fraCache, uændret);
        bus.post(event);
    }
}
