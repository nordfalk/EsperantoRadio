package dk.emda.emdah_akt;

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

import java.util.ArrayList;

import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.v3.R;
import dk.emda.EmdahAdapter;

/**
 * Created by User on 2/28/2017.
 */

public class EmdahTab1MestSete_Frag extends Fragment implements EmdahAdapter.ItemClickCallback, Runnable {
  private static final String TAG = "Tab1Fragment";

  private Button btnTEST;
  private RecyclerView recyclerView;
  private EmdahAdapter adapter;
  private ArrayList<Udsendelse> listData;

  private static final String BUNDLE_EXTRAS = "BUNDLE_EXTRAS";
  private static final String EXTRA_QUOTE = "EXTRA_QUOTE";
  private static final String EXTRA_ATTR = "EXTRA_ATTR";

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.emdah_tab1_frag, container, false);
    //btnTEST = (Button) view.findViewById(R.id.button1);


    recyclerView = (RecyclerView) view.findViewById(R.id.rec_list);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

    App.networkHelper.tv.startHentMestSete(null, 0, this); // null henter for alle kanaler
    App.data.mestSete.observatører.add(this);
    run();
    return view;
  }

  @Override
  public void run() {
    listData = App.data.mestSete.udsendelserFraKanalSlug.get(null); // null vil sige alle kanaler
    if (listData == null) listData = new ArrayList<>();
    adapter = new EmdahAdapter(listData, getActivity());
    recyclerView.setAdapter(adapter);
    adapter.setItemClickCallback(this);
  }

  @Override
  public void onDestroyView() {
    App.data.mestSete.observatører.remove(this);
    super.onDestroyView();
  }

  public void onItemClick(int p) {
    Udsendelse item = (Udsendelse) listData.get(p);

    Intent i = new Intent(getActivity(), EmdahDetailActivity.class);

    Bundle extras = new Bundle();
    extras.putString(EXTRA_QUOTE, item.titel);
    extras.putString(EXTRA_ATTR, item.beskrivelse);
    i.putExtra(BUNDLE_EXTRAS, extras);

    startActivity(i);
  }

  public void onSecondaryIconClick(int p) {

  }
}