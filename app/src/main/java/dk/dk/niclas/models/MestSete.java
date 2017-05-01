package dk.dk.niclas.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.data.Udsendelse;

/**
 * Created by Yoouughurt on 10-03-2017.
 */

public class MestSete {
    public HashMap<String, ArrayList<Udsendelse>> udsendelserFraKanalSlug = new HashMap<>();

    public ArrayList<Runnable> observatører = new ArrayList<>();
}
