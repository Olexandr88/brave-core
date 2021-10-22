package org.chromium.chrome.browser.brave_news;

import static org.chromium.ui.base.ViewUtils.dpToPx;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.widget.NestedScrollView;

import org.chromium.brave_news.mojom.Article;
import org.chromium.brave_news.mojom.BraveNewsController;
import org.chromium.brave_news.mojom.CardType;
import org.chromium.brave_news.mojom.Deal;
import org.chromium.brave_news.mojom.DisplayAd;
import org.chromium.brave_news.mojom.FeedItem;
import org.chromium.brave_news.mojom.FeedItemMetadata;
import org.chromium.brave_news.mojom.Image;
import org.chromium.brave_news.mojom.PromotedArticle;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.app.BraveActivity;
import org.chromium.chrome.browser.brave_news.models.FeedItemCard;
import org.chromium.chrome.browser.brave_news.models.FeedItemsCard;
import org.chromium.chrome.browser.brave_news.models.NewsItem;
import org.chromium.chrome.browser.preferences.BravePreferenceKeys;
import org.chromium.chrome.browser.preferences.SharedPreferencesManager;
import org.chromium.chrome.browser.util.ConfigurationUtils;
import org.chromium.chrome.browser.util.TabUtils;
import org.chromium.ui.base.DeviceFormFactor;
import org.chromium.url.mojom.Url;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.List;

public class CardBuilderFeedCard {
    // private final int HEADLINE = 0;
    // private final int HEADLINEPAIR = 1;
    // private final int THREE_ROWS_PHOTO = 2;
    // private final int TOP_NEWS = 3;
    // private final int THREE_ROWS_HEADLINES = 4;
    // private final int DISPLAY_AD = 5;
    // private final int DEALS = 6;
    // private final int WELCOME = 7;

    private final int CARD_LAYOUT = 8;
    private final int BUTTON_LAYOUT = 9;


    private final int TITLE = 10;
    private final int DESC = 11;
    private final int URL = 12;
    private final int PUBLISHER = 13;
    private final int TIME = 14;
    private final int CATEGORY = 15;
    private final int DEALS = 16;


    private LinearLayout linearLayout;
    private Activity mActivity;
    private View view;
    // private FeedItemCard mNewsItem;
    private FeedItemsCard mNewsItem;
    private int mPosition;
    private int mType;
    private int horizontalMargin;
    private boolean isTablet;
    private int mDeviceWidth;
    private boolean isLandscape;

    private ItemClickListener mClickListener;
    private String TAG = "BN";
    private BraveNewsController mBraveNewsController;
    private boolean isPromo;
    private String creativeInstanceId;
    private String offersCategory;
    private final int MARGIN_VERTICAL = 10;

    public CardBuilderFeedCard(BraveNewsController braveNewsController, LinearLayout layout,
            Activity activity, int position, FeedItemsCard newsItem, int type) {
        Log.d("bn", "createfeed cardbuilder:"+type);
        Log.d("bn", "createfeed cardbuilder position:"+position);
        linearLayout = layout;
        mActivity = activity;
        mPosition = position;
        mType = type;
        mNewsItem = newsItem;
        mBraveNewsController = braveNewsController;

        mDeviceWidth = ConfigurationUtils.getDisplayMetrics(activity).get("width");

        isPromo = false;
        creativeInstanceId = "";
        offersCategory = "";

        isTablet = ConfigurationUtils.isTablet(
                activity); // DeviceFormFactor.isNonMultiDisplayContextOnTablet(activity);
        isLandscape = ConfigurationUtils.isLandscape(activity);

        horizontalMargin = isTablet
                ? isLandscape ? (int) (0.20 * mDeviceWidth) : (int) (0.10 * mDeviceWidth)
                : 40;
        try {
            createCard(mType, mPosition);
        } catch (Exception e) {
            Log.d("bn", "crashinvestigation createCard:" + e);
        }
    }

    public void initItems() {}

    public void removeCard(LinearLayout layout) {
        layout.removeAllViews();
        layout.setVisibility(View.GONE);
        layout.invalidate();
    }

    private Url getImage(FeedItemMetadata itemMetaData) {
        Url imageUrlTemp = null;
        switch(itemMetaData.image.which()){
            
            case Image.Tag.PaddedImageUrl:
                imageUrlTemp = itemMetaData.image.getPaddedImageUrl();
                if (imageUrlTemp != null){
                    // Log.d("bn", "createfeed feed pages item image padded: "+imageUrlTemp.url);
                }
                break;
            case Image.Tag.ImageUrl:
                imageUrlTemp = itemMetaData.image.getImageUrl();
                if (imageUrlTemp != null){
                    // Log.d("bn", "createfeed feed pages item image: "+imageUrlTemp.url);
                }
                break;
        }

        final Url finalImageUrl = imageUrlTemp;

        return finalImageUrl;
    }

    private GradientDrawable roundedBackground() {
        GradientDrawable shape =  new GradientDrawable();
        shape.setCornerRadius(15);
        shape.setColor(mActivity.getResources().getColor(R.color.card_background));

        return shape;
    }

    public LinearLayout createCard(int type, int position) {
      TableLayout tableLayoutTopNews = new TableLayout(mActivity);

        TableLayout.LayoutParams tableParamsTopNews = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TableLayout.LayoutParams tableParamsRow1 = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        TableLayout.LayoutParams rowTableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);        
        //        RecyclerView.LayoutParams linearLayoutParams = new
        //        RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
        //        RecyclerView.LayoutParams.MATCH_PARENT);
        int displayHeight = mActivity.getResources().getDisplayMetrics().heightPixels;
        TableRow rowTop = new TableRow(mActivity);
        TextView topText = new TextView(mActivity);

        Log.d("BN", "display height:" + displayHeight);
        TableRow row1 = new TableRow(mActivity);
        TableRow row2 = new TableRow(mActivity);
        TableRow row3 = new TableRow(mActivity);

        LinearLayout layout1 = new LinearLayout(mActivity);
        LinearLayout layout2 = new LinearLayout(mActivity);
        LinearLayout layout3 = new LinearLayout(mActivity);

        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //        linearLayoutParams.setMargins(0, 40, 0, 40);
        linearLayoutParams.setMargins(horizontalMargin, 0, horizontalMargin, 40);


        // Log.d("BN", "createfeed CardBuilderFeed type:" + type);
        // Log.d("BN", "createfeed CardBuilderFeed mNewsItem:" + mNewsItem);
        Log.d("BN", "createfeed CardBuilderFeed type:" + type);
        Log.d("BN", "createfeed CardBuilderFeed position:" + position);

        try {
            switch (type) {
                // case WELCOME:
                //     NestedScrollView.LayoutParams welcomeParams =
                //             new
                //             NestedScrollView.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                //                     LinearLayout.LayoutParams.WRAP_CONTENT);
                //     welcomeParams.setMargins(
                //             horizontalMargin, displayHeight - 570, horizontalMargin, 500);
                //     linearLayout.setOrientation(LinearLayout.VERTICAL);
                //     linearLayout.setLayoutParams(welcomeParams);
                //     linearLayout.setBackgroundColor(
                //             mActivity.getResources().getColor(R.color.card_background));

                //     ImageView closeImage = new ImageView(mActivity);
                //     LinearLayout.LayoutParams closeImageParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     //                titleImageParams.height = 80;
                //     closeImageParams.gravity = Gravity.RIGHT;
                //     closeImageParams.setMargins(0, 10, 30, 0);
                //     closeImage.setLayoutParams(closeImageParams);
                //     closeImage.setImageResource(R.drawable.ic_close);
                //     closeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //     linearLayout.addView(closeImage);

                //     ImageView titleImage = new ImageView(mActivity);
                //     LinearLayout.LayoutParams titleImageParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     //                titleImageParams.height = 80;
                //     titleImageParams.gravity = Gravity.CENTER;
                //     titleImageParams.setMargins(30, 2, 30, 50);
                //     titleImage.setLayoutParams(titleImageParams);
                //     titleImage.setImageResource(R.drawable.sparkling_newsreader);
                //     titleImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //     linearLayout.addView(titleImage);

                //     TextView titleWelcome = new TextView(mActivity);
                //     LinearLayout.LayoutParams titleWelcomeParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     titleWelcome.setTextSize(18);
                //     titleWelcome.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                //     titleWelcome.setTextColor(
                //             mActivity.getResources().getColor(R.color.news_text_color));
                //     titleWelcome.setText(
                //             "Today’s top stories in a completely private feed, just for you.");
                //     titleWelcome.setTypeface(null, Typeface.BOLD);
                //     titleWelcomeParams.gravity = Gravity.CENTER;
                //     titleWelcome.setGravity(Gravity.CENTER_HORIZONTAL);
                //     titleWelcomeParams.setMargins(40, 30, 40, 30);
                //     titleWelcome.setLayoutParams(titleWelcomeParams);
                //     linearLayout.addView(titleWelcome);

                //     TextView subtitleWelcome = new TextView(mActivity);
                //     LinearLayout.LayoutParams subtitleWelcomeParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     subtitleWelcome.setTextSize(14);
                //     subtitleWelcome.setTextColor(
                //             mActivity.getResources().getColor(R.color.news_text_color));
                //     subtitleWelcome.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                //     subtitleWelcome.setText(
                //             "Brave Today matches your interests on your device so your personal
                //             information never leaves your browser. New content updated throughout
                //             the day.");
                //     subtitleWelcomeParams.gravity = Gravity.CENTER;
                //     subtitleWelcome.setGravity(Gravity.CENTER_HORIZONTAL);
                //     subtitleWelcomeParams.setMargins(40, 30, 40, 30);
                //     subtitleWelcome.setLayoutParams(subtitleWelcomeParams);
                //     linearLayout.addView(subtitleWelcome);

                //     Button settingsButWelcome = new Button(mActivity);
                //     LinearLayout.LayoutParams settingsButWelcomeParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     settingsButWelcome.setTextSize(16);
                //     settingsButWelcome.setAllCaps(false);
                //     settingsButWelcome.setText("Turn on Brave News");
                //     settingsButWelcomeParams.gravity = Gravity.CENTER;
                //     settingsButWelcome.setTextColor(
                //             mActivity.getResources().getColor(R.color.news_text_color));
                //     settingsButWelcome.setGravity(Gravity.CENTER);
                //     settingsButWelcome.setPadding(40, 20, 40, 20);
                //     settingsButWelcomeParams.setMargins(30, 30, 30, 30);
                //     settingsButWelcome.setBackground(makeRound(BUTTON_LAYOUT));
                //     settingsButWelcome.setLayoutParams(settingsButWelcomeParams);
                //     settingsButWelcome.setOnClickListener(new View.OnClickListener() {
                //         @Override
                //         public void onClick(View v) {
                //             Log.d("Test", "settingsButWelcome");
                //             if (mClickListener != null) mClickListener.onOptInClick(view);
                //         }
                //     });
                //     linearLayout.addView(settingsButWelcome);

                //     TextView learnMoreWelcome = new TextView(mActivity);
                //     LinearLayout.LayoutParams learnMoreWelcomeParams =
                //             new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                //                     LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                //     learnMoreWelcome.setTextSize(16);
                //     learnMoreWelcome.setTextColor(
                //             mActivity.getResources().getColor(R.color.news_text_color));
                //     learnMoreWelcome.setText("Learn more about your data");
                //     learnMoreWelcomeParams.gravity = Gravity.CENTER;
                //     learnMoreWelcome.setGravity(Gravity.CENTER_HORIZONTAL);
                //     learnMoreWelcome.setTypeface(null, Typeface.BOLD);
                //     learnMoreWelcomeParams.setMargins(0, 20, 0, 80);
                //     learnMoreWelcome.setLayoutParams(learnMoreWelcomeParams);
                //     learnMoreWelcome.setOnClickListener(new View.OnClickListener() {
                //         @Override
                //         public void onClick(View v) {
                //             if (mClickListener != null) mClickListener.onOptInClick(view);
                //         }
                //     });
                //     linearLayout.addView(learnMoreWelcome);

                //     break;
                case CardType.HEADLINE:
                case CardType.DISPLAY_AD:
                case CardType.PROMOTED_ARTICLE:
                    addElementsToSingleLayout(linearLayout, 0, type);
                    // linearLayout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));

                    linearLayout.setBackground(roundedBackground());
                    // Log.d("bn", "crashinvestigation HEADLINE after setlayout");
                    // linearLayout.setBackgroundColor(
                    //         mActivity.getResources().getColor(R.color.card_background));
                    break;
                case CardType.DEALS:
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    // linearLayout.setBackgroundColor(
                    //         mActivity.getResources().getColor(R.color.card_background));
                    linearLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    linearLayoutParams.setMargins(horizontalMargin, 0, horizontalMargin, 20);
                    linearLayout.setLayoutParams(linearLayoutParams);

                    tableLayoutTopNews.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));

                    tableParamsTopNews.setMargins(50, 50, 50, 50);
                    tableParamsTopNews.weight = 1;
                    tableParamsTopNews.height = TableLayout.LayoutParams.WRAP_CONTENT;

                    row1.setPadding(50, 0, 50, 0);

                    linearLayout.addView(tableLayoutTopNews);
                    tableLayoutTopNews.addView(rowTop);

                    rowTop.addView(topText);

                    tableLayoutTopNews.addView(row1);

                    row1.addView(layout1);
                    row1.addView(layout2);
                    row1.addView(layout3);

                    rowTop.setLayoutParams(tableParamsTopNews);
                    // topText.setText("DEALS OF THE DAY");
                    setTextFromFeed(topText, DEALS, 0);
                    topText.setTextSize(17);
                    topText.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    topText.setTypeface(null, Typeface.BOLD);

                    layout1.setOrientation(LinearLayout.VERTICAL);
                    layout2.setOrientation(LinearLayout.VERTICAL);
                    layout3.setOrientation(LinearLayout.VERTICAL);

                    addElementsToSingleLayout(layout1, 0, type);
                    addElementsToSingleLayout(layout2, 1, type);
                    addElementsToSingleLayout(layout3, 2, type);

                    linearLayout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));

                    break;
                case CardType.PUBLISHER_GROUP: // THREE_ROWS_HEADLINES:
                    Log.d(TAG, "creating HREE_ROWS_HEADLINES");

                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    tableLayoutTopNews.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT));
                    tableParamsTopNews.setMargins(30, 40, 30, 40);
                    linearLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    linearLayoutParams.setMargins(horizontalMargin, 0, horizontalMargin, 20);
                    linearLayout.setLayoutParams(linearLayoutParams);

                    linearLayout.addView(tableLayoutTopNews);
                    tableLayoutTopNews.addView(rowTop);
                    rowTop.addView(topText);

                    tableLayoutTopNews.addView(row1);
                    tableLayoutTopNews.addView(row2);
                    tableLayoutTopNews.addView(row3);
                    rowTop.setPadding(0,0,30,0);

                    rowTop.setLayoutParams(tableParamsTopNews);

                    // @TODO group title
                    setTextFromFeed(topText, CATEGORY, 0);
                    topText.setTextSize(20);
                    topText.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    topText.setTypeface(null, Typeface.BOLD);
                    //
                    addElementsToSingleLayout(row1, 0, type);

                    row2.setPadding(5, 5, 5, 5);
                    addElementsToSingleLayout(row2, 1, type);


                    TableRow.LayoutParams row3Params = new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
                    row3Params.bottomMargin = 2 * MARGIN_VERTICAL;
                    row3.setPadding(5, 5, 5, 5);
                    addElementsToSingleLayout(row3, 2, type);

                    linearLayout.setPadding(30, 10, 40, 10);
                    linearLayout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));
                    break;
                case CardType.CATEGORY_GROUP: // TOP_NEWS:
                    /*3 rows

                                   TOP NEWS

                                   Title           ---------
                                   Description     |       |
                                                   |       |
                                                   ---------

                                   Title           ---------
                                   Description     |       |
                                                   |       |
                                                   --------


                                   Title           ---------
                                   Description     |       |
                                                   |       |
                                                   --------

                                                   tablet landscape credit position good, first card
                       too low with int topCardMargin = ConfigurationUtils.isLandscape(mActivity) ?
                       (int) (dpHeight - 50) : (int) (dpHeight + 200); tablet portrait credit
                       position too high, first card too low

                                    */

                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // linearLayout.setBackgroundColor(
                    //         mActivity.getResources().getColor(R.color.card_background));

                    // DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    float dpHeight =
                            ConfigurationUtils.getDpDisplayMetrics(mActivity).get("height");

                    LinearLayout.LayoutParams params =
                            new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                                    TableLayout.LayoutParams.WRAP_CONTENT);
                    Log.d("bn", "firstcard dpheight:" + dpHeight);
                    Log.d("bn", "firstcard dpheight px:" + dpToPx(mActivity, dpHeight));
                    Log.d("bn", "firstcard dpheight+200:" + (int) (dpHeight + 200));
                    Log.d("bn", "firstcard dptopx 35:" + dpToPx(mActivity, 35));

                    params.setMargins(horizontalMargin, 0, horizontalMargin, 40);
                    int height = mActivity.getResources().getDisplayMetrics().heightPixels;
                    linearLayout.setLayoutParams(params);
                    Log.d("bn",
                            "newlayout display height:"
                                    + ConfigurationUtils.getDisplayMetrics(mActivity).get(
                                            "height"));
                    Log.d("bn", "newlayout linearLayout.getBottom:" + linearLayout.getBottom());
                    linearLayout.addView(tableLayoutTopNews);
                    tableLayoutTopNews.addView(rowTop);
                    rowTop.addView(topText);

                    tableLayoutTopNews.addView(row1);
                    tableLayoutTopNews.addView(row2);
                    tableLayoutTopNews.addView(row3);

                    linearLayout.setPadding(30, 30, 30, 10);
                    tableParamsTopNews.setMargins(30, 2 * MARGIN_VERTICAL, 30, 4 * MARGIN_VERTICAL);
                    tableParamsTopNews.weight = 3;
                    rowTop.setLayoutParams(tableParamsTopNews);
                    topText.setText(
                            mActivity.getResources().getString(R.string.brave_news_top_news_title));
                    topText.setTextSize(26);
                    topText.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    topText.setTypeface(null, Typeface.BOLD);

                    rowTableParams.bottomMargin = 2 * MARGIN_VERTICAL;
                    row1.setLayoutParams(rowTableParams);
                    row2.setLayoutParams(rowTableParams);
                    row3.setLayoutParams(rowTableParams);

                    addElementsToSingleLayout(row1, 0, type);
                    addElementsToSingleLayout(row2, 1, type);
                    addElementsToSingleLayout(row3, 2, type);
                    linearLayout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));

                    break;
                case CardType.HEADLINE_PAIRED: // HEADLINEPAIR:
                    /*headlinepair

                                 Image      Image
                               ---------    ---------
                               |       |    |       |
                               |       |    |       |
                               ---------    ---------
                                 Title        Title
                              Description    Description

                            */
                    Log.d("bn", "HEADLINE_PAIRED card position:" + position + " type: " + type);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    // linearLayout.setBackgroundColor(Color.TRANSPARENT);
                    linearLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    linearLayoutParams.setMargins(horizontalMargin - 20, 0, horizontalMargin, 20);
                    linearLayout.setLayoutParams(linearLayoutParams);

                    LinearLayout.LayoutParams cellParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    cellParams.setMargins(0, 0, 20, 0);

                    cellParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                    LinearLayout layoutLeft = new LinearLayout(mActivity);
                    LinearLayout layoutRight = new LinearLayout(mActivity);
                    linearLayout.addView(layoutLeft);
                    layoutLeft.setLayoutParams(cellParams);
                    layoutLeft.setOrientation(LinearLayout.VERTICAL);
                    //                layoutLeft.setPadding(10, 10, 10, 10);
                    //                layoutLeft.setBackgroundColor(mActivity.getResources().getColor(R.color.card_background));
                    addElementsToSingleLayout(layoutLeft, 0, type);

                    //                LinearLayout layoutRight = new LinearLayout(mActivity);
                    linearLayout.addView(layoutRight);
                    cellParams.setMargins(20, 0, 0, 0);

                    cellParams.height =  LinearLayout.LayoutParams.MATCH_PARENT;
                    layoutRight.setLayoutParams(cellParams);
                    layoutRight.setOrientation(LinearLayout.VERTICAL);
                    //                layoutLeft.setPadding(10, 10, 10, 10);
                    //                layoutRight.setBackgroundColor(mActivity.getResources().getColor(R.color.card_background));

                    addElementsToSingleLayout(layoutRight, 1, type);

                    layoutLeft.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    layoutRight.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                    // Log.d("bn", "HEADLINE_PAIRED height left:"+ layoutLeft.getHeight()+ " right:"+layoutRight.getMeasuredHeight());
                    // Log.d("bn", "HEADLINE_PAIRED height left height:"+ layoutLeft.getMeasuredHeight()+ " width:"+layoutLeft.getMeasuredWidth());
                    // Log.d("bn", "HEADLINE_PAIRED height left height:"+ layoutRight.getMeasuredHeight()+ " width:"+layoutRight.getMeasuredWidth());
                    int maxHeight =  Math.max(layoutLeft.getMeasuredHeight(),layoutRight.getMeasuredHeight());
                    Log.d("bn", "HEADLINE_PAIRED height max height:" + maxHeight);

                    if (maxHeight > layoutLeft.getMeasuredHeight()) {
                        cellParams.height = maxHeight;
                        layoutLeft.setLayoutParams(cellParams);
                    } else if (maxHeight > layoutRight.getMeasuredHeight()) {
                        cellParams.height = maxHeight;
                        layoutRight.setLayoutParams(cellParams);
                    }
                    Log.d("bn", "HEADLINE_PAIRED after measure height left:"+ layoutLeft.getMeasuredHeight()+ " right:"+layoutRight.getMeasuredHeight());
                    break;
            }
        } catch (Exception e) {
            Log.d("bn", "crashinvestigation exception:" + e);
        }

        // linearLayout.setBackground(makeRound(CARD_LAYOUT));
        Log.d("bn", "crashinvestigation after card creating");

        return linearLayout;
    }

    private void addElementsToSingleLayout(ViewGroup view, int index, int itemType) {
        ImageView image = new ImageView(mActivity);
        ImageView logo = new ImageView(mActivity);
        TextView title = new TextView(mActivity);
        TextView publisher = new TextView(mActivity);
        TextView source = new TextView(mActivity);
        TextView desc = new TextView(mActivity);

        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams publisherParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);

        TableLayout.LayoutParams tableParamsTopNews = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        TableRow.LayoutParams linearLayoutRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow.LayoutParams linearLayoutRowParams1 = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow.LayoutParams sourceRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow.LayoutParams titleRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow.LayoutParams descRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        TableRow.LayoutParams imageRowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

        LinearLayout layou1 = new LinearLayout(mActivity);
        publisher.setLetterSpacing(0.03f);

        try {
            LinearLayout layout = (LinearLayout) view;
            switch (itemType) {

                case CardType.DEALS:
                    LinearLayout layoutDeals = (LinearLayout) view;
                    layoutDeals.setOrientation(LinearLayout.VERTICAL);
                    linearLayoutRowParams1.width = 0;
                    linearLayoutRowParams1.weight = 1;
                    // linearLayoutRowParams1.height = 500;
                    linearLayoutRowParams1.setMargins(0, 0, 30, 0);
                    layoutDeals.setLayoutParams(linearLayoutRowParams1);

                    // image.setImageResource(R.drawable.img1);
                    setImage(image, "image", index);
                    imageParams.height = 230;
                    // imageParams.weight = 4;
                    // imageParams.bottomMargin = 20;
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setLayoutParams(imageParams);
                    layoutDeals.addView(image);

                    titleParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // titleParams.weight = 3;
                    title.setTextSize(14);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setPadding(10, 10, 10, 0);
                    title.setLayoutParams(titleParams);
                    title.setMaxLines(2);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    layoutDeals.addView(title);

                    descParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // descParams.weight = 2;

                    setTextFromFeed(desc, DESC, index);
                    desc.setTextSize(12);
                    desc.setMaxLines(5);
                    // descParams.bottomMargin = 60;
                    desc.setLayoutParams(descParams);

                    descParams.bottomMargin = MARGIN_VERTICAL;
                    desc.setPadding(10, 0, 30, 30);
                    layoutDeals.addView(desc);

                    break;
                case CardType.HEADLINE:
                    layout.setOrientation(LinearLayout.VERTICAL);
                    int topPosition = 0;
                    if (mPosition == 0) {
                        topPosition = 200;
                            // (int) (BraveActivity.getBraveActivity().getImageCreditLayoutBottom() + 20);
                    }
                    linearLayoutParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayoutParams.setMargins(horizontalMargin, topPosition, horizontalMargin, 30);

                    // layout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));
                    layout.setLayoutParams(linearLayoutParams);

                    layout.addView(image);
                    layout.addView(title);
                    layout.addView(desc);
                    layout.addView(publisher);
                           
                    // int height = (int) (image.getWidth() * 0.6);
                    // Log.d("bn", "after setting params for CardType.HEADLINE image.getWidth()"
                    // +image.getWidth() + " height:"+height+ " imageParams:"+imageParams.width + "
                    // imageParams: "+ imageParams.height);

                    // imageParams.height = 400;//(int) (image.getWidth() * 0.6);
                    imageParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;

                    // imageParams.weight = 5;
                    // imageParams.bottomMargin = 10;
                    image.setLayoutParams(imageParams);
                    setImage(image, "image", index);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    // Log.d("bn", "after setting params for CardType.HEADLINE image.getWidth()"
                    // +image.getWidth() + " height:"+height); Log.d("bn", "after setting params for
                    // CardType.HEADLINE image.getWidth() layout" +layout.getWidth() );

                    titleParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // titleParams.weight = 0;
                    // titleParams.bottomMargin = 10;
                    title.setTextSize(17);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setMaxLines(5);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setPadding(50, 0, 50, 0);
                    title.setLayoutParams(titleParams);

                    descParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // descParams.weight = 0;
                    // descParams.bottomMargin = 10;
                    setTextFromFeed(desc, TIME, index);
                    desc.setTextSize(11);
                    desc.setLayoutParams(descParams);
                    desc.setPadding(50, 0, 50, 30);

                    setTextFromFeed(publisher, PUBLISHER, 0);

                    Log.d("bn", "crashinvestigation setting LayoutParams");
                    publisher.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    publisher.setTextSize(12);
                    publisherParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // publisherParams.weight = 1;
                    publisherParams.bottomMargin = 10;
                    publisher.setPadding(40, 0, 40, 0);

                    publisherParams.bottomMargin = 2 * MARGIN_VERTICAL;;
                    publisher.setLayoutParams(publisherParams);
                    // Log.d("bn", "crashinvestigation set LayoutParams");

                    break;
                case CardType.DISPLAY_AD:
                    layout.setOrientation(LinearLayout.VERTICAL);
                    linearLayoutParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayoutParams.setMargins(horizontalMargin, 0, horizontalMargin, 50);
                    layout.setLayoutParams(linearLayoutParams);

                    layout.addView(image);
                    layout.addView(title);
                    layout.addView(desc);
                    layout.addView(logo);
                    //
                    imageParams.height = 0;
                    imageParams.weight = 7;
                    image.setLayoutParams(imageParams);
                    // image.setImageResource(R.drawable.img2);
                    setImage(image, "image", index);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    titleParams.height = 0;
                    titleParams.weight = 3;
                    titleParams.rightMargin = 20;
                    title.setTextSize(16);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setLines(3);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setPadding(30, 30, 30, 0);
                    title.setLayoutParams(titleParams);

                    descParams.weight = 1;
                    descParams.height = 0;
                    desc.setTextSize(11);
                    setTextFromFeed(desc, TIME, index);
                    desc.setLayoutParams(descParams);
                    desc.setPadding(30, 0, 30, 30);

                    logoParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    logoParams.height = 0;
                    logoParams.weight = 1;
                    logo.setPadding(30, 0, 30, 40);
                    logoParams.gravity = Gravity.CENTER_VERTICAL;
                    logo.setScaleType(ImageView.ScaleType.FIT_START);
                    logo.setImageResource(R.drawable.logo);
                    // setImage(logo, "image");
                    logo.setLayoutParams(logoParams);

                    break;
                case CardType.PROMOTED_ARTICLE:
                    layout.setOrientation(LinearLayout.VERTICAL);
                    linearLayoutParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayoutParams.setMargins(horizontalMargin, 0, horizontalMargin, 30);
                    layout.setLayoutParams(linearLayoutParams);

                    TextView promoted = new TextView(mActivity);
                    LinearLayout promotedLogoLayout = new LinearLayout(mActivity);


                    imageParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    titleParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);                                      
                    descParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);                      
                    publisherParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT); 

                    layout.addView(image);
                    layout.addView(title);
                    layout.addView(desc);
                    layout.addView(promotedLogoLayout);
                    //
                    imageParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // imageParams.weight = 7;
                    image.setLayoutParams(imageParams);
                    // image.setImageResource(R.drawable.img2);
                    setImage(image, "image", index);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    titleParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // titleParams.weight = 3;
                    titleParams.rightMargin = 20;
                    title.setTextSize(17);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setMaxLines(5);
                    titleParams.bottomMargin = MARGIN_VERTICAL;
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setPadding(50, 30, 50, 0);
                    title.setLayoutParams(titleParams);

                    descParams.weight = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // descParams.height = 0;
                    setTextFromFeed(desc, TIME, index);
                    desc.setTextSize(11);
                    desc.setLayoutParams(descParams);
                    desc.setPadding(50, 0, 50, 30);

                    LinearLayout.LayoutParams promotedParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    publisherParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    LinearLayout.LayoutParams promotedLayoutParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    promotedLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    promotedLogoLayout.setPadding(50, 0, 50, 0);
                    // promotedLayoutParams.setMargins(50, 0, 50, 0);
                    promotedParams.width = 0;
                    promotedParams.weight = 1;
                    // promotedParams.gravity =  Gravity.CENTER_VERTICAL | Gravity.END;
                    promotedLayoutParams.bottomMargin = 10;
                    promotedLogoLayout.setOrientation(LinearLayout.HORIZONTAL);

                    promoted.setGravity(Gravity.CENTER);
                    promotedParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
                    promotedLayoutParams.gravity = Gravity.CENTER;  
                    publisherParams.gravity = Gravity.CENTER_VERTICAL;

                    promoted.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_promoted, 0, 0, 0);
                    StringBuilder promotedTitle = new StringBuilder(" "
                            + mActivity.getResources().getString(
                                    R.string.brave_news_promoted_title));
                    promoted.setText(mActivity.getResources().getString(
                                    R.string.brave_news_promoted_title));

                    promoted.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    promoted.setTextSize(12);
                    promoted.setLayoutParams(promotedParams);
                    // promotedLogoLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.news_promoted_background_color));
                    promoted.setBackground(
                            makeRound(BUTTON_LAYOUT, R.color.news_promoted_background_color, 15));
                    promotedLogoLayout.setLayoutParams(promotedLayoutParams);

                    setTextFromFeed(publisher, PUBLISHER, 0);
                    publisher.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    publisher.setTextSize(12);
                    publisherParams.width = 0;
                    publisherParams.weight = 2;
                    publisherParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;

                    publisher.setLayoutParams(publisherParams);
                    publisherParams.bottomMargin = 2 * MARGIN_VERTICAL;
                    promotedLogoLayout.addView(publisher);
                    promotedLogoLayout.addView(promoted);

                    break;
                case CardType.HEADLINE_PAIRED:
                    Log.d("bn", "HEADLINE_PAIRED single position:" + index + " type: " + itemType);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    imageParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    titleParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);                                      
                    descParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);                      
                    publisherParams =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT); 

                    // imageParams.weight = 6;
                    // imageParams.height = 0;
                    // image.setAdjustViewBounds(true);

                    int height = (int) (mDeviceWidth* 0.3);
                    // image.setMaxHeight(height);
                    // image.setMaxHeight(height);
                    imageParams.height = height;
                    image.setLayoutParams(imageParams);
                    setImage(image, "image", index);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageParams.bottomMargin = MARGIN_VERTICAL;
                    layout.addView(image);

                    // titleParams.weight = 4;
                    // titleParams.height = 0;
                    title.setLayoutParams(titleParams);
                    title.setTextSize(13);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setMaxLines(5);
                    titleParams.bottomMargin = MARGIN_VERTICAL;
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setPadding(50, 0, 50, 0);
                    layout.addView(title);

                    descParams.weight = 1;
                    descParams.height = 0;
                    desc.setTextSize(11);
                    setTextFromFeed(desc, TIME, index);
                    desc.setLayoutParams(descParams);
                    desc.setPadding(50, 0, 50, 0);
                    descParams.bottomMargin = MARGIN_VERTICAL;
                    layout.addView(desc);

                    setTextFromFeed(publisher, PUBLISHER, index);
                    publisher.setTextColor(
                            mActivity.getResources().getColor(R.color.news_text_color));
                    publisher.setTextSize(12);
                    // publisherParams.height = 0;

                    publisherParams.gravity = Gravity.BOTTOM;
                    publisher.setGravity(Gravity.BOTTOM);
                    publisherParams.setMargins(0, 0, 0, 2 * MARGIN_VERTICAL);
                    publisher.setPadding(50, 0, 50, 0);
                    publisher.setLayoutParams(publisherParams);
                    layout.addView(publisher);
                    layout.setBackground(roundedBackground());

                    // layout.setMaxHeight(650);

                    // layout.setBackground(makeRound(CARD_LAYOUT, R.color.card_background, 15));

                    break;

                case CardType.PUBLISHER_GROUP: // 3 numbered rows no photos

                    TableLayout.LayoutParams layoutRowParams = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    TableRow layoutRow = (TableRow) view;
                    TextView no = new TextView(mActivity);
                    
                    layoutRow.removeAllViews();
                    layou1.removeAllViews();

                    layoutRowParams.setMargins(0, 20, 0, 20);
                    layoutRowParams.weight = 1;
                    layoutRowParams.height = 0;
                    layoutRow.setPadding(30,0,30,0);
                    layoutRow.setLayoutParams(layoutRowParams);


                    TableRow.LayoutParams noParams = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);      
                    TableRow.LayoutParams layou1Params = new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);    

                    no.setText(String.valueOf(index + 1));
                    no.setTextColor(mActivity.getResources().getColor(R.color.news_text_color));
                    noParams.width = 0;
                    noParams.weight = 1;
                    noParams.gravity =  Gravity.CENTER_VERTICAL;
                    no.setLayoutParams(noParams);

                    layoutRow.addView(no);

                    layou1.setOrientation(LinearLayout.VERTICAL);
                    layou1Params.width = 0;
                    layou1Params.weight = 7;
                    // linearLayoutRowParams.setMargins(0, 0, 0, 40);

                    // layou1.setPadding(30, 0, 30, 0);

                    layou1.setLayoutParams(layou1Params);

                    titleRowParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // titleRowParams.weight = 2;
                    // titleRowParams.rightMargin = 20;
                    title.setTextSize(15);
                    titleRowParams.bottomMargin = MARGIN_VERTICAL;
                    title.setTypeface(null, Typeface.BOLD);
                    title.setLayoutParams(titleRowParams);
                    title.setMaxLines(5);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    layou1.addView(title);

                    descRowParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // descRowParams.weight = 1;
                    setTextFromFeed(desc, TIME, index);
                    descRowParams.bottomMargin = 20;
                    desc.setTextSize(12);
                    desc.setLayoutParams(descRowParams);
                    layou1.addView(desc);

                    layoutRow.addView(layou1);
                    break;
                case CardType.CATEGORY_GROUP:// TOP_NEWS
                    TableRow layoutRowPhotos = (TableRow) view;

                    tableParamsTopNews.setMargins(0, 0, 0, 5 * MARGIN_VERTICAL);
                    tableParamsTopNews.weight = 1;
                    tableParamsTopNews.height = 0;
                    layoutRowPhotos.setPadding(30, 0, 30, 0);
                    layoutRowPhotos.setLayoutParams(tableParamsTopNews);

                    layou1.setOrientation(LinearLayout.VERTICAL);
                    linearLayoutRowParams.width = 0;
                    linearLayoutRowParams.weight = 2;
                    linearLayoutRowParams.gravity = Gravity.CENTER_VERTICAL;
                    layou1.setGravity(Gravity.CENTER_VERTICAL);
                    layou1.setLayoutParams(linearLayoutRowParams);

                    // sourceRowParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // sourceRowParams.weight = 1;
                    source.setLines(1);
                    sourceRowParams.gravity = Gravity.BOTTOM;
                    source.setPadding(0, 0, 0, 0);
                    sourceRowParams.height = TableRow.LayoutParams.WRAP_CONTENT;
                    sourceRowParams.setMargins(0, 0, 0, MARGIN_VERTICAL);
                    source.setLayoutParams(sourceRowParams);
                    source.setTextSize(12);
                    setTextFromFeed(source, PUBLISHER, index);
                    source.setTextColor(mActivity.getResources().getColor(R.color.news_time_color));
                    layou1.addView(source);

                    titleRowParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // titleRowParams.weight = 1;
                    titleRowParams.rightMargin = 20;
                    title.setTextSize(13);
                    title.setTypeface(null, Typeface.BOLD);
                    titleRowParams.bottomMargin = MARGIN_VERTICAL;
                    title.setLayoutParams(titleRowParams);
                    title.setMaxLines(4);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    layou1.addView(title);

                    descRowParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    // descRowParams.weight = 2;
                    desc.setPadding(0, 0, 0, 0);
                    desc.setTextSize(11);
                    descRowParams.bottomMargin = 0;
                    setTextFromFeed(desc, TIME, index);
                    desc.setLayoutParams(descRowParams);
                    layou1.addView(desc);

                    layoutRowPhotos.addView(layou1);

                    imageRowParams.width = 0;
                    imageRowParams.height = 300;
                    // image.setMinimumHeight(200);
                    imageRowParams.weight = 1;
                    imageRowParams.setMargins(0, 0, 0, 0);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    // image.setImageResource(R.drawable.img3);
                    setImage(image, "image", index);

                    image.setLayoutParams(imageRowParams);
                    layoutRowPhotos.addView(image);

                    break;
                default:
                    image.setImageResource(R.drawable.img4);
                    // title.setText("mNewsItem.getTitle()");
                    // desc.setText("mNewsItem.getDescription()");

                    setTextFromFeed(title, TITLE, index);
                    setTextFromFeed(desc, DESC, index);
                    layout.addView(image);
                    layout.addView(title);
                    layout.addView(desc);
                    break;
            }

            title.setTextColor(mActivity.getResources().getColor(R.color.news_text_color));
            desc.setTextColor(mActivity.getResources().getColor(R.color.news_time_color));

            // this section is common to all cases
            // title.setText("mNewsItem.getTitle()");
            setTextFromFeed(title, TITLE, index);
            // desc.setText("mNewsItem.getPublish_time()");
            // setTextFromFeed(desc, TIME, index);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Test", "cardclicklistener linearLayout: "+view + " position:"+mPosition+" index:" + index);
                    // @TODO alex refactor this with listener in BraveNewTabPageLayout
                    BraveActivity.getBraveActivity().setNewsFeedScrollPosition(mPosition);
                    FeedItemMetadata itemData = getItemData(index);
                    TabUtils.openUrlInSameTab(itemData.url.url);
                    // if (mClickListener != null) mClickListener.onItemClick(view, mPosition);
                }
            });

            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Test", "cardclicklistener linearLayout: "+view + " position:"+mPosition+" index:" + index);
                    // @TODO alex refactor this with listener in BraveNewTabPageLayout
                    BraveActivity.getBraveActivity().setNewsFeedScrollPosition(mPosition);
                    FeedItemMetadata itemData = getItemData(index);
                    TabUtils.openUrlInSameTab(itemData.url.url);
                    // if (mClickListener != null) mClickListener.onItemClick(view, mPosition);
                }
            });

        } catch (Exception e) {
            Log.d("bn", "crashinvestigation exception after addElementsToSingleLayout: " + e);
        }
    }

    private void setTextFromFeed(TextView textView, int type, int index) {
        
        FeedItemMetadata itemData = getItemData(index);
        setText(itemData, textView, type );
        setListeners(textView, itemData.url.url, creativeInstanceId, isPromo);
      
    }    

    private FeedItemMetadata getItemData(int index) {
        FeedItemMetadata itemData = null;
        // FeedItem feedItem = mNewsItem.getFeedItem();
        List<FeedItemCard> feedItemsCard = mNewsItem.getFeedItems();
        DisplayAd displayAd = mNewsItem.getDisplayAd();
        if (feedItemsCard == null) {
            return null;
        }
        FeedItemCard feedItemCard = feedItemsCard.get(index);
        FeedItem feedItem = feedItemCard.getFeedItem();

        // FeedItem feedItem = mNewsItem.getFeedItems().get(index).getFeedItem();
        switch(feedItem.which()){
            case FeedItem.Tag.Article:
                
                Article article = feedItem.getArticle();
                FeedItemMetadata articleData = article.data;
                itemData = article.data;

                
                // braveNewsItems.add(article.data);
                
                Log.d("bn", "getfeed feed pages item type articleData: " + articleData.title);
                break;
            case FeedItem.Tag.PromotedArticle:
                PromotedArticle promotedArticle = feedItem.getPromotedArticle();
                FeedItemMetadata promotedArticleData = promotedArticle.data;
                creativeInstanceId = promotedArticle.creativeInstanceId;
                // braveNewsItems.add(item.getPromotedArticle());
                itemData = promotedArticle.data;
                // setText(itemData, textView, type );
                isPromo = true;
                Log.d("bn", "getfeed feed pages item type PromotedArticle: " + promotedArticleData.title);
                // Log.d("bn", "getfeed feed pages item type PromotedArticle creativeInstanceId: " +
                // creativeInstanceId);
                break;                                            
            case FeedItem.Tag.Deal:
                Deal deal = feedItem.getDeal();
                FeedItemMetadata dealData = deal.data;
                offersCategory = deal.offersCategory;
                itemData = deal.data;
                // setText(itemData, textView, type );
                // isDeal = true;
                // braveNewsItems.add(item.getDeal());
                // braveNewsItems.add(deal.data);
                Log.d("bn", "getfeed feed pages item type Deal: " + dealData.title);
                // Log.d("bn", "getfeed feed pages item type Deal offersCategory: " +
                // offersCategory);
                break;
              // textView.setText(itemData.title);  
        }

        final FeedItemMetadata itemDataFinal = itemData;

        return itemDataFinal;
    }

    private void setListeners(View view, String urlString, String creativeInstanceId, boolean isPromo ){
        
        DisplayAd displayAd = mNewsItem.getDisplayAd();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Test", "click itemid:" + urlString);

                if (isPromo) {
                    // Updates the no. of promotion cards visited
                    mBraveNewsController.onPromotedItemVisit(
                            mNewsItem.getUuid(), creativeInstanceId);
                    // }

                    Log.d("bn",
                            "newsEvents onPromotedItemVisit: "
                                    + SharedPreferencesManager.getInstance().readInt(
                                            BravePreferenceKeys
                                                    .BRAVE_NEWS_PROMOTION_CARDS_VISITED));

                } else if (displayAd != null) {
                    // Updates the no. of ads cards visited
                    mBraveNewsController.onDisplayAdVisit(
                            displayAd.uuid, displayAd.creativeInstanceId);

                    Log.d("bn",
                            "newsEvents onDisplayAdVisit: "
                                    + SharedPreferencesManager.getInstance().readInt(
                                            BravePreferenceKeys
                                                    .BRAVE_NEWS_DISPLAYAD_CARDS_VISITED));

                } else {
                    // Brave News updates the no. of "normal" cards visited
                    int visitedNewsCardsCount = SharedPreferencesManager.getInstance().readInt(
                            BravePreferenceKeys.BRAVE_NEWS_CARDS_VISITED);
                    visitedNewsCardsCount++;
                    SharedPreferencesManager.getInstance().writeInt(
                            BravePreferenceKeys.BRAVE_NEWS_CARDS_VISITED, visitedNewsCardsCount);
                    if (visitedNewsCardsCount % 4 == 0) {
                        mBraveNewsController.onSessionCardVisitsCountChanged(
                                (short) visitedNewsCardsCount);
                    }

                    Log.d("bn",
                            "newsEvents onSessionCardVisitsCountChanged: "
                                    + SharedPreferencesManager.getInstance().readInt(
                                            BravePreferenceKeys.BRAVE_NEWS_CARDS_VISITED));
                }

                
            }
        });
    }

    private void setText(FeedItemMetadata itemData, TextView textView, int type){
        switch (type) {
            case TITLE:
                    textView.setText(itemData.title);
                    break;
            case DESC:
                    textView.setText(itemData.description);
                    break;            
            case PUBLISHER:
                    textView.setText(itemData.publisherName);
                    break;                
            case TIME:
                    textView.setText(itemData.relativeTimeDescription);
                    break;    
            case DEALS:
                    Log.d("bn", "getfeed feed pages item type Deal settext offersCategory: "+ offersCategory);
                    textView.setText(offersCategory);                
                    break;
            case CATEGORY:
                    textView.setText(itemData.categoryName);
                    break;
        }
        // Typeface typeface = ResourcesCompat.getFont(context, R.font.roboto);
        // textView.setTypeface(typeface);
    }


    private void setImage(ImageView imageView, String type, int index) {

        List<FeedItemCard> feedItemsCard = mNewsItem.getFeedItems();
        if (feedItemsCard != null) {

            FeedItemCard feedItemCard = feedItemsCard.get(index);
            FeedItem item = feedItemCard.getFeedItem();

            FeedItemMetadata itemMetaData = new FeedItemMetadata();
            switch(item.which()){
            case FeedItem.Tag.Article:
                Article article = item.getArticle();
                itemMetaData = article.data;
                break;
            case FeedItem.Tag.PromotedArticle:
                PromotedArticle promotedArticle = item.getPromotedArticle();
                itemMetaData = promotedArticle.data;
                break;                                            
            case FeedItem.Tag.Deal:
                Deal deal = item.getDeal();
                itemMetaData = deal.data;
                break;
            }

            Url itemImageUrl = getImage(itemMetaData);
            mBraveNewsController.getImageData(itemImageUrl, imageData -> {
                if (imageData != null){
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    imageView.setImageBitmap(decodedByte);
                    Log.d("bn", "after setting params for CardType.HEADLINE image.getWidth()" +imageView.getWidth() + " height:"+imageView.getHeight());
                    // Glide.with(mActivity).clear(imageView);
                    // Glide.with(mActivity)
                    //     // .asDrawable()
                    //     // .asBitmap()
                    //     .load(imageData)
                    //     // .override(Target.SIZE_ORIGINAL, imageView.getHeight())
                    //     // .centerCrop()
                    //     .transition(withCrossFade())
                    //     .into(imageView);

                }
            });  
        }
    }   


    private LayerDrawable makeRound(int type, int background, int radius) {
        float[] outerRadii = new float[] {radius, radius, radius, radius, radius, radius, radius, radius};
        float[] innerRadii = new float[] {radius, radius, radius, radius, radius, radius, radius, radius};

        // Set the shape background card_background
        ShapeDrawable backgroundShape = null;
        new ShapeDrawable(new RoundRectShape(outerRadii, null, innerRadii));

        if (type == BUTTON_LAYOUT) {
            backgroundShape = new ShapeDrawable(new RoundRectShape(outerRadii, null, innerRadii));
            backgroundShape.getPaint().setColor(
                    mActivity.getResources().getColor(background)); // background color
            backgroundShape.getPaint().setStyle(Paint.Style.FILL); // Define background
            backgroundShape.getPaint().setAntiAlias(true);
            backgroundShape.setPadding(10, 10, 10, 10);

        } else if (type == CARD_LAYOUT) {
            backgroundShape = new ShapeDrawable(new RoundRectShape(outerRadii, null, innerRadii));
            backgroundShape.getPaint().setColor(
                    mActivity.getResources().getColor(background)); // background color
            backgroundShape.getPaint().setStyle(Paint.Style.FILL); // Define background
        }

        // Initialize an array of drawables
        Drawable[] drawables = new Drawable[] {backgroundShape};
        // Initialize a layer drawable object
        LayerDrawable layerDrawable = new LayerDrawable(drawables);
        return layerDrawable;
    }

    public void setCardClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);

        void onOptInClick(View view);

        void onCloseClick(View view);
    }
}
