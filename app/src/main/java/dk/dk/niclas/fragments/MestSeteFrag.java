package dk.dk.niclas.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import dk.dk.niclas.event.events.MestSeteEvent;
import dk.dk.niclas.models.MestSete;
import dk.dk.niclas.utilities.VerticalScrollRecyclerView;
import dk.dr.radio.akt.Basisfragment;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;


public class MestSeteFrag extends Basisfragment {

    private static RecyclerViewAdapter mRecyclerViewAdapter;

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
        recyclerView.setAdapter(new VerticalScrollRecyclerViewAdapter());
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
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            mRecyclerViewAdapter = new RecyclerViewAdapter();
            recyclerView.setAdapter(mRecyclerViewAdapter);

            //Remove focus from the RecyclerView so we can intercept the vertical scrolling events
            recyclerView.setNestedScrollingEnabled(false);
        }
    }

    /**
     * The RecyclerViewAdapter that holds the list of most watched episodes for a single channel
     */
    private static class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private MestSete mestSete = App.data.mestSete;

        public static class ViewHolder extends RecyclerView.ViewHolder {

            private final ImageView mImageView;
            private final TextView mTextView;

            public ViewHolder(View view) {
                super(view);
                mImageView = (ImageView) view.findViewById(R.id.mestsete_udsendelse_imageview);
                mTextView = (TextView) view.findViewById(R.id.mestsete_udsendelse_description);
            }
        }

        private RecyclerViewAdapter() {
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
            return mestSete.udsendelser.size();
        }

        private void initImageView(ImageView imageView, int position){
            Udsendelse udsendelse = mestSete.udsendelser.get(position);

            if(udsendelse != null){
                Picasso.with(imageView.getContext()).load(udsendelse.billedeUrl).into(imageView);
            }
        }

        private void initTextView(TextView textView, int position){
            Udsendelse udsendelse = mestSete.udsendelser.get(position);

            if(udsendelse != null){
                textView.setText(udsendelse.beskrivelse);
            }
        }

        public void update(){
            mestSete = App.data.mestSete;
        }
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
            Log.d("size = " + App.data.mestSete.udsendelser.size());
            debugData();
            mRecyclerViewAdapter.update();
            mRecyclerViewAdapter.notifyDataSetChanged();
        } else { //Data er opdateret.
            //TODO stop spinner
            Log.d("Data opdateret");
            debugData();
            mRecyclerViewAdapter.update();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void updateData(){
        for(Kanal kanal : App.backend[1].kanaler)
        App.networkHelper.tv.getMestSete(kanal.kode, 0, this);
    }

    private void debugData(){
        for(Udsendelse udsendelse : App.data.mestSete.udsendelser)
            Log.d(udsendelse.billedeUrl + " description = " + udsendelse.beskrivelse);
    }
}
