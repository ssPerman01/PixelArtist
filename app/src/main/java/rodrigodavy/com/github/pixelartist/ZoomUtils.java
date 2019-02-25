package rodrigodavy.com.github.pixelartist;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ZoomUtils {

    String TAG = "ZoomUtils";

    int currZoom = 0;
    int oneStepZoom = 15;   // pixel


    public void zoomByDirection(LinearLayout paper, int directionZoom) {
        int tempWidth = -1;
        for (int i = 0; i < paper.getChildCount(); i++) {
            LinearLayout l = (LinearLayout) paper.getChildAt(i);

            for (int j = 0; j < l.getChildCount(); j++) {
                View pixel = l.getChildAt(j);

                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) pixel.getLayoutParams();
                if (tempWidth < 0) {
                    tempWidth = layoutParams.width + directionZoom * oneStepZoom;
                }
                layoutParams.width = tempWidth;
                layoutParams.height = tempWidth;

//                layoutParams.width = layoutParams.width + directionZoom * oneStepZoom;
//                layoutParams.height = layoutParams.height + directionZoom * oneStepZoom;

//                Log.i(TAG, "zoomByDirection: width="+layoutParams.width+" currZoom="+currZoom);
                pixel.setLayoutParams(layoutParams);
            }
        }
        currZoom = tempWidth;
        Log.i(TAG, "zoomByDirection: width= currZoom="+currZoom);
//        currZoom += directionZoom;
    }

}
