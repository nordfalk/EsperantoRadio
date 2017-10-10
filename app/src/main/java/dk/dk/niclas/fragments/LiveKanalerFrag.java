package dk.dk.niclas.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.MediaInfo;
import com.squareup.picasso.Picasso;

import dk.dk.niclas.cast.mediaplayer.LocalPlayerActivity;
import dk.dk.niclas.cast.utils.Utils;
import dk.dk.niclas.utilities.CastVideoProvider;
import dk.dr.radio.akt.Basisfragment;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;
import dk.faelles.model.NetsvarBehander;


public class LiveKanalerFrag extends Fragment {

  private static boolean fetchingSchedule = false;
  private KanalRecyclerViewAdapter recyclerViewAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    fetchingSchedule = true;
    String url = "http://www.dr.dk/mu-online/api/1.3/schedule/nownext-for-all-active-dr-tv-channels";

    App.netkald.kald(this, url, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
          return;
        }
        if (s.uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
          //TODO Håndter hvis det er uændret
        }

        if (s.json != null) {
          App.networkHelper.tv.backend.parseNowNextAlleKanaler(s.json, App.grunddata);
          Log.d("NowNext parsed for alle kanaler");//Data opdateret
          fetchingSchedule = false;
          recyclerViewAdapter.notifyDataSetChanged();
        } else {
          App.langToast(R.string.Netværksfejl_prøv_igen_senere);
        }
      }
    });

    RecyclerView recyclerView = (RecyclerView) inflater.inflate(
            R.layout.niclas_livekanaler_frag, container, false);
    setupRecyclerView(recyclerView);

    debugData();
    return recyclerView;
  }

  private void setupRecyclerView(RecyclerView recyclerView) {
    recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
    recyclerViewAdapter = new KanalRecyclerViewAdapter();
    recyclerView.setAdapter(recyclerViewAdapter);
  }

  private static class KanalRecyclerViewAdapter
          extends RecyclerView.Adapter<KanalRecyclerViewAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {

      private final ImageView kanalLogoImageView;
      private final ImageView udsendelseImageView;
      private final ImageView playButtonImageView;
      private final TextView udsendelseTextView;

      public ViewHolder(View view) {
        super(view);
        kanalLogoImageView = (ImageView) view.findViewById(R.id.list_livekanaler_kanallogo);
        udsendelseImageView = (ImageView) view.findViewById(R.id.list_livekanaler_udsendelseslogo);
        udsendelseImageView.setImageDrawable(null); // vis ikke eksempelbilledet
        playButtonImageView = (ImageView) view.findViewById(R.id.list_livekanaler_playbutton);
        udsendelseTextView = (TextView) view.findViewById(R.id.list_livekanaler_udsendelsestitel);
      }
    }

    private KanalRecyclerViewAdapter() {
    }

    @Override
    public KanalRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
              .inflate(R.layout.niclas_livekanaler_list, parent, false);
      return new KanalRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final KanalRecyclerViewAdapter.ViewHolder holder, int position) {
      Picasso.with(holder.kanalLogoImageView.getContext())
              .load(App.backend[1].kanaler.get(position).kanallogo_url)
              .into(holder.kanalLogoImageView);

      if (!fetchingSchedule) {
        Point displaySize = Utils.getDisplaySize(holder.kanalLogoImageView.getContext());
        String billedeUrl = App.backend[1].kanaler.get(position).getUdsendelse().billedeUrl;
        billedeUrl = Basisfragment.skalérBillede(App.backend[1].kanaler.get(position).getUdsendelse(), displaySize.x, displaySize.x * 9 / 16);
        //Log.d("billedeUrl = "+billedeUrl);
        Picasso.with(holder.udsendelseImageView.getContext())
                .load(billedeUrl).placeholder(null)
                .resize(displaySize.x, displaySize.x * 9 / 16)
                .into(holder.udsendelseImageView);


        holder.udsendelseTextView.setText(App.backend[1].kanaler.get(position).getUdsendelse().titel);
      }

      holder.playButtonImageView.setImageResource(R.drawable.afspiller_spil_normal);
      setClickListener(holder.playButtonImageView, position);

    }

    @Override
    public int getItemCount() {
      return App.backend[1].kanaler.size();
    }

    private void setClickListener(ImageView imageView, final int position) {
      imageView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          debugData();
          if (!fetchingSchedule) {
            if (App.backend[1].kanaler.get(position).getUdsendelse() != null) {
              startPlayerActivity(App.backend[1].kanaler.get(position), v.getContext());
            }
          }
        }
      });
    }
  }

  public static void startPlayerActivity(Kanal kanal, Context context) {
    MediaInfo mediaInfo = CastVideoProvider.buildMedia(kanal.getUdsendelse(), kanal);
    Activity activity = (Activity) context;
    Intent intent = new Intent(activity, LocalPlayerActivity.class);
    intent.putExtra("media", mediaInfo);
    intent.putExtra("shouldStart", false);
    activity.startActivity(intent);
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  private static void debugData() {
    for (Kanal kanal : App.backend[1].kanaler) {
      Log.d("Kanal streams size = " + kanal.streams.size());
      Log.d("Kanal udsendelser size = " + kanal.udsendelser.size());
      Log.d("Kanal slug =" + kanal.slug + "  " + kanal.kode + "   " + kanal.navn);
    }
  }
}
