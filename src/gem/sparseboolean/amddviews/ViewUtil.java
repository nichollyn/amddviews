package gem.sparseboolean.amddviews;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

public class ViewUtil {
    public static boolean isViewContained(View view, float rawX, float rawY) {
        Log.i("ViewUtil", "isViewContained");
        if (view == null || view.getWidth() == 0) {
            return false;
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        Log.i("ViewUtil", "loc of view is: x:" + x + " y:" + y);
        int width = view.getWidth();
        int height = view.getHeight();

        if (rawX < x || rawX > x + width || rawY < y || rawY > y + height) {
            return false;
        } else {
            return true;
        }
    }

    public static Bitmap getBitmapFromView(View v, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }
}
