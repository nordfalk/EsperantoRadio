package dk.dk.niclas.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dk.dk.niclas.cast.mediaplayer.LocalPlayerActivity;
import dk.dk.niclas.event.events.MestSeteEvent;
import dk.dk.niclas.event.events.NetværksFejlEvent;
import dk.dk.niclas.event.events.StreamsParsedEvent;
import dk.dk.niclas.models.MestSete;
import dk.dk.niclas.utilities.CastVideoProvider;
import dk.dk.niclas.utilities.VerticalScrollRecyclerView;
import dk.dr.radio.akt.Basisfragment;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;


public class MestSeteFrag extends Basisfragment {

    private static boolean fetchingStreams = false;

    private static RecyclerViewAdapter mRecyclerViewAdapter;
    private VerticalScrollRecyclerViewAdapter mVerticalScrollRecyclerViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.niclas__mest_sete_frag, container, false);
        VerticalScrollRecyclerView verticalScrollRecyclerView = (VerticalScrollRecyclerView)
                root.findViewById(R.id.mestsete_verticalscrollrecyclerview);
        setupVerticalScrollRecyclerView(verticalScrollRecyclerView);
        return root;
    }

    private void setupVerticalScrollRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        mVerticalScrollRecyclerViewAdapter = new VerticalScrollRecyclerViewAdapter();
        recyclerView.setAdapter(mVerticalScrollRecyclerViewAdapter);
    }


    /**
     * The Adapter that holds the list of channels and their corresponding RecyclerView
     *  containing the most watched episodes for that channel.
     */
    private static class VerticalScrollRecyclerViewAdapter
            extends RecyclerView.Adapter<VerticalScrollRecyclerViewAdapter.ViewHolder> {

        public static class ViewHolder extends RecyclerView.ViewHolder {

            private final ImageView mImageView;
            private final RecyclerView mRecyclerView;

            public ViewHolder(View view) {
                super(view);
                mImageView = (ImageView) view.findViewById(R.id.mestsete_kanal_imageview);
                mRecyclerView = (RecyclerView) view.findViewById(R.id.mestsete_udsendelse_recyclerview);
            }
        }

        private VerticalScrollRecyclerViewAdapter() {
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.niclas_mest_sete_list, parent, false);
            return new VerticalScrollRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            initImageView(holder.mImageView, position);
            initRecyclerView(holder.mRecyclerView, position);
        }

        @Override
        public int getItemCount() {
            return App.backend[1].kanaler.size();
        }

        private void initImageView(ImageView imageView, int position){
            imageView.setImageResource(App.grunddata.kanaler.get(position).kanallogo_resid);
        }

        private void initRecyclerView(RecyclerView recyclerView, int position){
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            mRecyclerViewAdapter = new RecyclerViewAdapter(getKanalSlugFraPosition(position));
            recyclerView.setAdapter(mRecyclerViewAdapter);

            //Remove focus from the RecyclerView so we can intercept the vertical scrolling events
            recyclerView.setNestedScrollingEnabled(false);
        }

        private String getKanalSlugFraPosition(int position){
            return App.backend[1].kanaler.get(position).slug;
        }
    }

    /**
     * The RecyclerViewAdapter that holds the list of most watched episodes for a single channel
     */
    private static class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private MestSete mestSete = App.data.mestSete;
        private String kanalSlug;

        public static class ViewHolder extends RecyclerView.ViewHolder {

            private final ImageView mImageView;
            private final TextView mTextView;

            public ViewHolder(View view) {
                super(view);
                mImageView = (ImageView) view.findViewById(R.id.mestsete_udsendelse_imageview);
                mTextView = (TextView) view.findViewById(R.id.mestsete_udsendelse_description);
            }
        }

        private RecyclerViewAdapter(String kanalSlug) {
            this.kanalSlug = kanalSlug;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.niclas_mest_sete_udseendelse_list, parent, false);
            return new RecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            initImageView(holder.mImageView, position);
            initTextView(holder.mTextView, position);
        }

        @Override
        public int getItemCount() {
            if(mestSete.udsendelserFraKanalSlug.get(kanalSlug) != null) {
                return mestSete.udsendelserFraKanalSlug.get(kanalSlug).size();
            } else return 0;
        }

        private void initImageView(ImageView imageView, int position){
            if(mestSete.udsendelserFraKanalSlug.get(kanalSlug) != null) {
                Udsendelse udsendelse = mestSete.udsendelserFraKanalSlug.get(kanalSlug).get(position);

                if (udsendelse != null) {
                    Picasso.with(imageView.getContext()).load(udsendelse.billedeUrl).into(imageView);
                    setClickListener(imageView, udsendelse);
                }
            }
        }

        private void initTextView(TextView textView, int position){
            if(mestSete.udsendelserFraKanalSlug.get(kanalSlug) != null) {
                Udsendelse udsendelse = mestSete.udsendelserFraKanalSlug.get(kanalSlug).get(position);

                if (udsendelse != null) {
                    textView.setText(udsendelse.titel);
                }
            }
        }

        public void update(){
            mestSete = App.data.mestSete;
        }

        private void setClickListener(final ImageView imageView, final Udsendelse udsendelse){
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(fetchingStreams) {
                        return;
                    }

                    if(udsendelse.harStreams()){
                        MediaInfo mediaInfo = CastVideoProvider.buildMedia(udsendelse);
                        Activity activity = (Activity) imageView.getContext();
                        Intent intent = new Intent(activity, LocalPlayerActivity.class);
                        intent.putExtra("media", mediaInfo);
                        intent.putExtra("shouldStart", false);
                        activity.startActivity(intent);
                    } else {
                        fetchingStreams = true;
                        App.networkHelper.tv.startHentStreamsForUdsendelse(udsendelse);
                    }
                }
            });
        }
    }

    @Subscribe
        public void StreamsParsedEvent(StreamsParsedEvent event){
            if(event.isFraCache()){
                Log.d("Fra cache");;
            } else
            if(event.isUændret()){
                Log.d("Uændret");
                fetchingStreams = false;
                //Should not end up here
                startPlayerActivity(event.getUdsendelse());
            } else { //Data er opdateret.
                Log.d("Data opdateret");
                startPlayerActivity(event.getUdsendelse());
                fetchingStreams = false;
            }
    }

    public void startPlayerActivity(Udsendelse udsendelse){
        MediaInfo mediaInfo = CastVideoProvider.buildMedia(udsendelse);
        Activity activity = (Activity) getContext();
        Intent intent = new Intent(activity, LocalPlayerActivity.class);
        intent.putExtra("media", mediaInfo);
        intent.putExtra("shouldStart", false);
        activity.startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        updateData();
    }

    @Subscribe
    public void MestSeteEvent(MestSeteEvent event){
        if(event.isFraCache()){
            Log.d("Fra cache");
        } else
        if(event.isUændret()){
            //TODO stop spinner
            Log.d("Uændret");
            Log.d("size = " + App.data.mestSete.udsendelserFraKanalSlug.size());
            debugData();
            mRecyclerViewAdapter.update();
            mVerticalScrollRecyclerViewAdapter.notifyDataSetChanged();
        } else { //Data er opdateret.
            //TODO stop spinner
            Log.d("Data opdateret");
            debugData();
            mRecyclerViewAdapter.update();
            mVerticalScrollRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void netværksFejl(NetværksFejlEvent event){
        fetchingStreams = false;
        Toast.makeText(getActivity(), R.string.Netværksfejl_prøv_igen_senere,
                Toast.LENGTH_LONG).show();
    }

    private void updateData(){
        for(Kanal kanal : App.backend[1].kanaler)
        App.networkHelper.tv.startHentMestSete(kanal.slug, 0, this);
    }

    private void debugData(){
        HashMap<String, ArrayList<Udsendelse>> map = App.data.mestSete.udsendelserFraKanalSlug;

        for (Map.Entry<String, ArrayList<Udsendelse>> entry : map.entrySet()) {
            String key = entry.getKey();
            Log.d("Key = " + key);
            ArrayList<Udsendelse> value = entry.getValue();
            for(Udsendelse udsendelse: value){
                Log.d("Value navn = " + udsendelse.getNavn());
            }
        }
    }
}
