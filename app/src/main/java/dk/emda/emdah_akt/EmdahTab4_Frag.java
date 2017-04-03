package dk.emda.emdah_akt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

import static dk.dr.radio.v3.R.id.beskrivelse;

/**
 * Created by User on 2/28/2017.
 */

public class EmdahTab4_Frag extends Fragment {
    private static final String TAG = "Tab4Fragment";
    private  ImageView imageView;
    private  ImageView kanallogo;
    private TextView beskrivelse;
    private TextView titel;
    //private Button btnTEST;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emdah_card_item,container,false);
       // btnTEST = (Button) view.findViewById(R.id.hør);
        imageView = (ImageView) view.findViewById(R.id.im_item_icon);
        beskrivelse= (TextView) view.findViewById(R.id.lbl_item_text);
        titel = (TextView) view.findViewById(R.id.lbl_item_sub_title);


                final Kanal kanal = App.grunddata.kanaler.get(2); // P3

                final Date dato = new Date();
                final String datoStr = Datoformater.apiDatoFormat.format(dato);
                final String url = kanal.getBackend().getUdsendelserPåKanalUrl(kanal, datoStr);
                final String dr ="http://www.dr.dk/";


                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        try {
                            String data = Diverse.hentUrlSomStreng(url);
                            ArrayList<Udsendelse> liste = kanal.getBackend().parseUdsendelserForKanal(data, kanal, dato, App.data);
                            kanal.setUdsendelserForDag(liste, datoStr);

                            System.out.println("Kanal "+kanal);
                            System.out.println("Udsendelser "+liste);
                            System.out.println("Kanalnavn"+ liste.get(2).getNavn().toString());
                            return liste;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        ArrayList<Udsendelse> liste = (ArrayList<Udsendelse>) o;
                        //if (liste!=null) btnTEST.setText(liste.get(0).titel);

                        Picasso.with(getContext()).load(dr+liste.get(2).billedeUrl).into(imageView);


                        titel.setText(liste.get(2).getNavn());

                        beskrivelse.setText(liste.get(2).beskrivelse);


                    }
                }.execute();






        return view;
    }
}