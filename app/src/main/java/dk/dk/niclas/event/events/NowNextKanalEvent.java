package dk.dk.niclas.event.events;

/**
 * Created by Yoouughurt on 20-04-2017.
 */

public class NowNextKanalEvent extends AbstractEvent {

    private String kanalSlug;

    public NowNextKanalEvent(boolean fraCache, boolean uændret, String kanalSlug){
        super(fraCache, uændret);
        this.kanalSlug = kanalSlug;
    }

    public String getKanalSlug() {
        return kanalSlug;
    }
}
