package dk.emda;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.v3.R;

/**
 * Created by Emdadollah on 08-03-2017.
 */

public class EmdahAdapter extends RecyclerView.Adapter<EmdahAdapter.DerpHolder> {

  private List<Udsendelse> listData;
  private LayoutInflater inflater;

  private ItemClickCallback itemClickCallback;

  public interface ItemClickCallback {
    void onItemClick(int p);

    void onSecondaryIconClick(int p);
  }

  public void setItemClickCallback(final ItemClickCallback itemClickCallback) {
    this.itemClickCallback = itemClickCallback;
  }

  public EmdahAdapter(List<Udsendelse> listData, Context c) {
    inflater = LayoutInflater.from(c);
    this.listData = listData;
  }

  @Override
  public EmdahAdapter.DerpHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = inflater.inflate(R.layout.emdah_card_item, parent, false);
    return new DerpHolder(view);
  }

  @Override
  public void onBindViewHolder(DerpHolder holder, int position) {
    Udsendelse item = listData.get(position);
    holder.title.setText(item.titel);
    holder.subTitle.setText(item.beskrivelse);
       /* if (item.isFavourite()){
            holder.secondaryIcon.setImageResource(R.drawable.ic_home_black_24dp);
        } else {
            holder.secondaryIcon.setImageResource(R.drawable.ic_home_black_24dp);
        }
        */
  }

  @Override
  public int getItemCount() {
    return listData.size();
  }

  class DerpHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    ImageView thumbnail;
    ImageView secondaryIcon;
    TextView title;
    TextView subTitle;
    View container;

    public DerpHolder(View itemView) {
      super(itemView);
      thumbnail = (ImageView) itemView.findViewById(R.id.im_item_icon);
      //secondaryIcon = (ImageView)itemView.findViewById(R.id.im_item_icon_secondary);
      //secondaryIcon.setOnClickListener(this);
      subTitle = (TextView) itemView.findViewById(R.id.lbl_item_sub_title);
      title = (TextView) itemView.findViewById(R.id.lbl_item_text);
      container = (View) itemView.findViewById(R.id.cont_item_root);
      container.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      if (v.getId() == R.id.cont_item_root) {
        itemClickCallback.onItemClick(getAdapterPosition());
      } else {
        itemClickCallback.onSecondaryIconClick(getAdapterPosition());
      }
    }
  }
}