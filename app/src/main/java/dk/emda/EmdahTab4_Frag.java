package dk.emda;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

/**
 * Created by User on 2/28/2017.
 */

public class EmdahTab4_Frag extends Fragment {
    private static final String TAG = "Tab4Fragment";

    private Button btnTEST;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emdah_tab4_frag,container,false);
        btnTEST = (Button) view.findViewById(R.id.button4);

        btnTEST.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "TESTING BUTTON CLICK 4", Toast.LENGTH_SHORT).show();
                final Kanal kanal = App.grunddata.kanaler.get(2); // P3

                final Date dato = new Date();
                final String datoStr = Datoformater.apiDatoFormat.format(dato);
                final String url = App.backend.getUdsendelserPåKanalUrl(kanal, datoStr);

                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        try {
                            String data = Diverse.hentUrlSomStreng(url);
                            ArrayList<Udsendelse> liste = App.backend.parseUdsendelserForKanal(data, kanal, dato, App.data);
                            kanal.setUdsendelserForDag(liste, datoStr);

                            System.out.println("Kanal "+kanal);
                            System.out.println("Udsendelser "+liste);
                            return liste;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        ArrayList<Udsendelse> liste = (ArrayList<Udsendelse>) o;
                        if (liste!=null) btnTEST.setText(liste.get(0).titel);
                    }
                }.execute();
            }
        });

        return view;
    }
}