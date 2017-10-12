package dk.dk.niclas.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.cast.MediaInfo;

import org.json.JSONObject;

import dk.dk.niclas.cast.mediaplayer.LocalPlayerActivity;
import dk.dk.niclas.utilities.CastVideoProvider;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;
import dk.faelles.model.NetsvarBehander;

public class KanalerFrag extends Fragment {


  private static boolean fetchingSchedule = false;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    RecyclerView recyclerView = (RecyclerView) inflater.inflate(
            R.layout.niclas_kanaler_frag, container, false);
    setupRecyclerView(recyclerView);
    debugData();
    return recyclerView;
  }

  private void setupRecyclerView(RecyclerView recyclerView) {
    recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
    recyclerView.setAdapter(new KanalRecyclerViewAdapter());
  }

  private class KanalRecyclerViewAdapter
          extends RecyclerView.Adapter<KanalRecyclerViewAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

      private final ImageView mImageView1;
      private final ImageView mImageView2;
      private final ImageView mImageView3;

      private int numberOfImageViews;
      private int modulo;

      public ViewHolder(View view) {
        super(view);
        mImageView1 = (ImageView) view.findViewById(R.id.list_kanaler_image_1);
        mImageView2 = (ImageView) view.findViewById(R.id.list_kanaler_image_2);
        mImageView3 = (ImageView) view.findViewById(R.id.list_kanaler_image_3);
        numberOfImageViews = 3;
        modulo = App.backend[1].kanaler.size() % 3;

      }
    }

    private KanalRecyclerViewAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
              .inflate(R.layout.niclas_kanaler_list, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

      if (position != 0) {
        position = position * holder.numberOfImageViews;
      }

      if (getItemCount() != position) {
        initImageView(holder.mImageView1, position);
        initImageView(holder.mImageView2, position + 1);
        initImageView(holder.mImageView3, position + 2);
      } else {
        initImageView(holder.mImageView1, position);
        if (holder.modulo != 0) {
          initImageView(holder.mImageView2, position + 1);
          if (holder.modulo == 2) {
            initImageView(holder.mImageView2, position + 2);
          }
        }
      }
    }

    @Override
    public int getItemCount() {
      int size = App.backend[1].kanaler.size();

      if (size % 3 == 0) {
        return size / 3;
      }
      return size / 3 + 1;
    }

    private void initImageView(ImageView imageView, int position) {
      imageView.setImageResource(App.grunddata.kanaler.get(position).kanallogo_resid);
      setClickListener(imageView, position);
    }

    private void setClickListener(ImageView imageView, final int position) {
      imageView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          debugData();
          if (!fetchingSchedule) {
            if (App.backend[1].kanaler.get(position).getUdsendelse() != null) {
              startPlayerActivity(App.backend[1].kanaler.get(position));
            } else {
              fetchingSchedule = true;
              final Kanal kanal = App.backend[1].kanaler.get(position);
              App.netkald.kald(this, "http://www.dr.dk/mu-online/api/1.3/schedule/nownext/" + kanal.slug, new NetsvarBehander() {
                @Override
                public void fikSvar(Netsvar s) throws Exception {
                  if (s.fraCache) { // Første kald vil have fraCache = true hvis der er noget i cache.
                    return;
                  }
                  if (kanal.getUdsendelse() != null && s.uændret) { // Andet kald vil have uændret = true hvis dataen er uændret i forhold til cache.
                    return;
                  }

                  if (s.json != null) {
                    JSONObject jsonObject = new JSONObject(s.json);
                    App.networkHelper.tv.backend.parseNowNextKanal(jsonObject, kanal);
                    Log.d("NowNext parsed for kanal = " + kanal.slug);//Data opdateret
                    fetchingSchedule = false;
                    //Should not end up here
                    startPlayerActivity(App.grunddata.kanalFraSlug.get(kanal));
                  } else {
                    App.langToast(R.string.Netværksfejl_prøv_igen_senere);
                  }
                }
              });
            }
          }
        }
      });
    }
  }

  public void startPlayerActivity(Kanal kanal) {
    MediaInfo mediaInfo = CastVideoProvider.buildMedia(kanal.getUdsendelse(), kanal);
    Activity activity = getActivity();
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
