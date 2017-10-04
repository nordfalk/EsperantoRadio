package dk.emda.emdah_data;

import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;

/**
 * Created by Emdadollah on 08-03-2017.
 */

public class EmdahMestSeteData {


  final Kanal kanal = App.grunddata.kanaler.get(2); // P3

  final Date dato = new Date();
  final String datoStr = Datoformater.apiDatoFormat.format(dato);
  final String url = kanal.getBackend().getUdsendelserPåKanalUrl(kanal, datoStr);


  private static final String[] titles = {"Nothingness cannot be defined",
          "Time is like a river made up of the events which happen, and a violent stream; " +
                  "for as soon as a thing has been seen, it is carried away, and another comes" +
                  " in its place, and this will be carried away too,",
          "But when I know that the glass is already broken, every minute with it is precious.",
          "For me, it is far better to grasp the Universe as it really is than to persist in" +
                  " delusion, however satisfying and reassuring.",
          "The seeker after the truth is not one who studies the writings of the ancients and," +
                  " following his natural disposition, puts his trust in them, but rather the" +
                  " one who suspects his faith in them and questions what he gathers from them," +
                  " the one who submits to argument and demonstration, and not to the " +
                  "sayings of a human being whose nature is fraught with all kinds " +
                  "of imperfection and deficiency.",
          "You must take personal responsibility. You cannot change the circumstances, the" +
                  " seasons, or the wind, but you can change yourself. That is something you" +
                  " have charge of."
  };


  private static final String[] subTitles = {"Bruce Lee",
          "Marcus Aurelius",
          "Meng Tzu",
          "Ajahn Chah",
          "Carl Sagan",
          "Alhazen",
          "Jim Rohn"

  };

  private static final int[] icons = {android.R.drawable.ic_popup_reminder, android.R.drawable.ic_menu_add,
          android.R.drawable.ic_menu_delete};
/*
    public static List<ListItem> getMestSeteListData() {
        List<ListItem> data = new ArrayList<>();

        //Repeat process 4 times, so that we have enough data to demonstrate a scrollable
        //RecyclerView
        for (int x = 0; x < 4; x++) {
            //create ListItem with dummy data, then add them to our List
            for (int i = 0; i < titles.length && i < icons.length; i++) {
                ListItem item = new ListItem();
                item.setImageResId(icons[i]);
                item.setTitle(titles[i]);
                item.setSubTitle(subTitles[i]);
                data.add(item);
            }
        }
        return data;
    }

*/
}
