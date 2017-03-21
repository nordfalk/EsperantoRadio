package dk.emda;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import dk.dr.radio.v3.R;

import java.util.ArrayList;

/**
 * Created by User on 2/28/2017.
 */

public class EmdahTab1_Frag extends Fragment implements EmdahAdapter.ItemClickCallback {
    private static final String TAG = "Tab1Fragment";

    private Button btnTEST;
    private RecyclerView recyclerView;
    private EmdahAdapter adapter;
    private ArrayList listData;

    private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
    private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
    private static final String EXTRA_ATTR = "EXTRA_ATTR";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emdah_tab1_frag,container,false);
        //btnTEST = (Button) view.findViewById(R.id.button1);


        listData = (ArrayList) EmdahDerpData.getListData();

        recyclerView = (RecyclerView) view.findViewById(R.id.rec_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.HORIZONTAL, false));

        adapter = new EmdahAdapter(EmdahDerpData.getListData(),getActivity());
        recyclerView.setAdapter(adapter);
        adapter.setItemClickCallback(this);





        return view;
    }

    public void onItemClick(int p) {
        ListItem item = (ListItem) listData.get(p);

        Intent i = new Intent(getActivity(), EmdahDetailActivity.class);

        Bundle extras = new Bundle();
        extras.putString(EXTRA_QUOTE, item.getTitle());
        extras.putString(EXTRA_ATTR, item.getSubTitle());
        i.putExtra(BUNDLE_EXTRAS, extras);

        startActivity(i);
    }

    public void onSecondaryIconClick(int p) {

    }
}