package dk.emda;

/**
 * Created by Emdadollah on 08-03-2017.
 */

public class ListItem {

    private int imageResId;
    private String subTitle;
    private String title;
    private boolean favourite = false;





    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle (){return subTitle;}

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

}


