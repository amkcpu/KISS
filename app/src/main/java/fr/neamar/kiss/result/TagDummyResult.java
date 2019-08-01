package fr.neamar.kiss.result;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.utils.FuzzyScore;

public class TagDummyResult extends Result {
    private static final String TAG = TagDummyResult.class.getSimpleName();
    private static Drawable gBackground = null;

    TagDummyResult(@NonNull TagDummyPojo pojo) {
        super(pojo);
    }

    private Drawable getShape(Context context, SharedPreferences sharedPreferences) {
        boolean largeSearchBar = sharedPreferences.getBoolean("large-search-bar", false);
        int barSize = context.getResources().getDimensionPixelSize(largeSearchBar ? R.dimen.large_bar_height : R.dimen.bar_height);

        if ( gBackground == null || gBackground.getIntrinsicWidth() != barSize || gBackground.getIntrinsicHeight() != barSize ) {
            int inset = (int)(3.f * context.getResources().getDisplayMetrics().density);   // 3dp to px
            barSize -= 2 * inset;   // shrink size with the inset to keep the overall size

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setSize(barSize, barSize);
            float rad = barSize / 2.3f;
            shape.setCornerRadii(new float[]{rad, rad, rad, rad, rad, rad, rad, rad});
            shape.setColor(0xFFffffff);

            gBackground = new InsetDrawable(shape, inset);
        }

        return gBackground;
    }

    private Bitmap generateBitmap(Context context, SharedPreferences sharedPreferences) {
        boolean largeSearchBar = sharedPreferences.getBoolean("large-search-bar", false);
        int barSize = context.getResources().getDimensionPixelSize(largeSearchBar ? R.dimen.large_bar_height : R.dimen.bar_height);

        int width, height = width = barSize;

        // create a canvas from a bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // use StaticLayout to draw the text centered
        TextPaint paint = new TextPaint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(.6f * height);

        RectF rectF = new RectF(0, 0, width, height);
        rectF.inset(1.f, 1.f);

        // draw a white rounded background
        paint.setColor(0xFFffffff);
        canvas.drawRoundRect(rectF, width / 2.4f, height / 2.4f, paint);

        int codepoint = pojo.getName().codePointAt(0);
        String glyph = new String(Character.toChars(codepoint));
        // If the codepoint glyph is an image we can't use SRC_IN to draw it.
        boolean drawAsHole = true;
        Character.UnicodeBlock block = null;
        try {
            block = Character.UnicodeBlock.of(codepoint);
        } catch (IllegalArgumentException ignored) {
        }
        if (block == null)
            drawAsHole = false;
        else if ("DINGBATS".equals(block.toString()))
            drawAsHole = false;
        else if ("EMOTICONS".equals(block.toString()))
            drawAsHole = false;
        else if ("MISCELLANEOUS_SYMBOLS".equals(block.toString()))
            drawAsHole = false;
        else if ("MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS".equals(block.toString()))
            drawAsHole = false;
        else if ("SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS".equals(block.toString()))
            drawAsHole = false;
        else if ("TRANSPORT_AND_MAP_SYMBOLS".equals(block.toString()))
            drawAsHole = false;
        else if (!"BASIC_LATIN".equals(block.toString())) {
            // log untested glyphs
            Log.d(TAG, "Codepoint " + codepoint + " with glyph " + glyph + " is in block " + block);
        }
        // we can't draw images (emoticons and symbols) using SRC_IN with transparent color, the result is a square
        if (drawAsHole) {
            // write text with "transparent" (create a hole in the background)
            paint.setColor(0);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        } else {
            paint.setColor(0xFFffffff);
        }

        // draw the letter in the center
        Rect b = new Rect();
        paint.getTextBounds(glyph, 0, glyph.length(), b);
        canvas.drawText(glyph, 0, glyph.length(), width / 2.f - b.centerX(), height / 2.f - b.centerY(), paint);

        rectF.set(b);
        rectF.offset(width / 2.f - rectF.centerX(), height / 2.f - rectF.centerY());
        // pad the rectF so we don't touch the letter
        rectF.inset(rectF.width() * -.3f, rectF.height() * -.4f);

        // stroke a rect with the bounding of the letter
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.f * context.getResources().getDisplayMetrics().density);
        canvas.drawRoundRect(rectF, rectF.width() / 2.4f, rectF.height() / 2.4f, paint);
        return bitmap;
    }

    @NonNull
    @Override
    public View display(Context context, int position, View v, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_search, parent);

        ImageView image = v.findViewById(R.id.item_search_icon);
        TextView searchText = v.findViewById(R.id.item_search_text);

        image.setImageDrawable(getDrawable(context));
        searchText.setText(pojo.getName());

        image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return v;
    }

    @NonNull
    @Override
    public View inflateFavorite(@NonNull Context context, @Nullable View favoriteView, @NonNull ViewGroup parent) {
        if (favoriteView == null)
            favoriteView = LayoutInflater.from(context).inflate(R.layout.favorite_tag, parent, false);
        ImageView favoriteIcon = favoriteView.findViewById(android.R.id.background);
        TextView favoriteText = favoriteView.findViewById(android.R.id.text1);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("pref-fav-tags-drawable", false)) {
            favoriteText.setVisibility(View.GONE);

            Drawable drawable = new BitmapDrawable(generateBitmap(context, sharedPreferences));
            favoriteIcon.setImageDrawable(drawable);
        } else {
            int codepoint = pojo.getName().codePointAt(0);
            String glyph = new String(Character.toChars(codepoint));

            Drawable drawable = getShape(context, sharedPreferences);
            favoriteIcon.setImageDrawable(drawable);
            favoriteIcon.invalidateDrawable(drawable);

            favoriteText.setVisibility(View.VISIBLE);
            favoriteText.setText(glyph);
            favoriteText.setTextSize(TypedValue.COMPLEX_UNIT_PX, drawable.getIntrinsicHeight() / 2.f);
        }

        favoriteView.setContentDescription(pojo.getName());
        return favoriteView;
    }

    @Override
    protected void doLaunch(Context context, View v) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).showMatchingTags(pojo.getName());
        }
    }
}
