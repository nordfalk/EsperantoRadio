package dk.dk.niclas.models;

import java.util.ArrayList;
import java.util.HashMap;

import dk.dr.radio.data.Udsendelse;

/**
 * Created by Yoouughurt on 10-03-2017.
 */

public class MestSete {

    public HashMap<String, ArrayList<Udsendelse>> udsendelser;

    public MestSete(){
        udsendelser = new HashMap<>();
    }

}
