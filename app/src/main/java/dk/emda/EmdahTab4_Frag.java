package dk.emda;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
            }
        });

        return view;
    }
}