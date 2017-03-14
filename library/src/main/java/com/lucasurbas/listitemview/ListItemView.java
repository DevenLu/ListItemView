package com.lucasurbas.listitemview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.lucasurbas.listitemview.util.ViewUtils;
import com.lucasurbas.listitemview.util.view.MenuView;

/**
 * Description.
 *
 * @author Lucas Urbas
 */
public class ListItemView extends FrameLayout {

    private static final String TAG = "ListItemView";

    private static final int DEFAULT_MENU_ITEMS_ROOM = 2;

    private static final int SINGLE_LINE_ITEM_HEIGHT_DP = 48;

    private static final int SINGLE_LINE_AVATAR_ITEM_HEIGHT_DP = 56;

    private static final int TWO_LINE_ITEM_HEIGHT_DP = 72;

    private static final int AVATAR_WIDTH_DP = 40;

    private static final int ICON_WIDTH_DP = 24;

    private static final int TITLE_LEADING_SP = 24;

    private static final int SUBTITLE_LEADING_SP = 20;

    private LinearLayout mItemLayout;

    private TextView mTitleView;

    private TextView mSubtitleView;

    private ImageView mIconView;

    // private CircularIconView mCircularIconView;

    private ImageView mAvatarView;

    private MenuView mMenuView;

    // VARIABLES

    private int mMenuId = -1;

    private int mMenuItemsRoom;

    private String mTitle;

    private String mSubtitle;

    private boolean mIsMultiline;

    private int mPaddingEnd;

    private int mPaddingStart;

    private int mPaddingVertical;

    private int mKeyline;

    private boolean mForceKeyline;

    private int mAvatarWidth;

    private int mIconWidth;

    private Drawable mIconDrawable;

    @ColorInt
    private int mIconColor;

    /**
     * Interface for implementing a listener to listen
     * when an menu item has been selected.
     */
    public interface OnMenuItemClickListener {

        /**
         * Called when a menu item has been selected.
         *
         * @param item the selected menu item.
         */
        void onActionMenuItemSelected(MenuItem item);
    }

    public ListItemView(@NonNull final Context context) {
        super(context);
        init(null);
    }

    public ListItemView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(final AttributeSet attrs) {

        inflate(getContext(), R.layout.liv_list_item_layout, this);

        mItemLayout = (LinearLayout) findViewById(R.id.item_layout);
        mMenuView = (MenuView) findViewById(R.id.menu_view);
        mTitleView = (TextView) findViewById(R.id.title_view);
        mSubtitleView = (TextView) findViewById(R.id.subtitle_view);
        mIconView = (ImageView) findViewById(R.id.icon_view);

        mPaddingEnd = getResources().getDimensionPixelSize(R.dimen.liv_padding_end);
        mPaddingStart = getResources().getDimensionPixelSize(R.dimen.liv_padding_start);
        mKeyline = getResources().getDimensionPixelSize(R.dimen.liv_keyline);

        mAvatarWidth = (int) ViewUtils.dpToPixel(AVATAR_WIDTH_DP);
        mIconWidth = (int) ViewUtils.dpToPixel(ICON_WIDTH_DP);

        if (attrs != null) {
            applyAttrs(attrs);
        }

        setupView();
    }

    private void applyAttrs(final AttributeSet attrs) {

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ListItemView);
        int defaultColor = ViewUtils.getDefaultColor(getContext());

        try {

            mMenuId = a.getResourceId(R.styleable.ListItemView_liv_menu, -1);
            mMenuItemsRoom = a.getInteger(R.styleable.ListItemView_liv_menuItemsRoom,
                    DEFAULT_MENU_ITEMS_ROOM);

            mTitle = a.getString(R.styleable.ListItemView_liv_title);
            mSubtitle = a.getString(R.styleable.ListItemView_liv_subtitle);
            mIsMultiline = a.getBoolean(R.styleable.ListItemView_liv_multiline, false);

            mPaddingEnd = a.getDimensionPixelSize(R.styleable.ListItemView_liv_paddingEnd,
                    mPaddingEnd);
            mPaddingStart = a.getDimensionPixelSize(R.styleable.ListItemView_liv_paddingStart,
                    mPaddingStart);
            mKeyline = a.getDimensionPixelSize(R.styleable.ListItemView_liv_keyline, mKeyline);
            mForceKeyline = a.getBoolean(R.styleable.ListItemView_liv_forceKeyline, false);

            mIconDrawable = a.getDrawable(R.styleable.ListItemView_liv_icon);
            mIconColor = a.getColor(R.styleable.ListItemView_liv_iconColor, defaultColor);

        } finally {
            a.recycle();
        }
    }

    private void setupView() {

        assertPadding();

        setupTextView(mTitleView, (int) ViewUtils.spToPixel(TITLE_LEADING_SP), 1);
        setupTextView(mSubtitleView, (int) ViewUtils.spToPixel(SUBTITLE_LEADING_SP), 1);

        setIcon(mIconDrawable);
        setMultiline(mIsMultiline);
        setTitle(mTitle);
        setSubtitle(mSubtitle);
        inflateMenu(mMenuId);

    }

    private void assertPadding() {
        if (hasAvatar() || hasCircularIcon()) {
            if (mKeyline - mAvatarWidth < mPaddingStart) {
                throw new IllegalArgumentException("keyline value is to small");
            }
        } else if (hasIcon()) {
            if (mKeyline - mIconWidth < mPaddingStart) {
                throw new IllegalArgumentException("keyline value is to small");
            }
        } else {
            if (mKeyline < mPaddingStart) {
                throw new IllegalArgumentException("keyline value is to small");
            }
        }
    }

    private boolean useKeyline() {
        return mForceKeyline || mIconDrawable != null;
    }

    private void adjustPadding() {
        mItemLayout.setPaddingRelative(useKeyline() ? mKeyline : mPaddingStart, mPaddingVertical,
                mPaddingEnd, mPaddingVertical);
        ((MarginLayoutParams) mIconView.getLayoutParams()).setMarginStart(mPaddingStart);
    }

    private void setupTextView(final TextView textView, final int leading, final int step) {
        // This is to make the behavior more deterministic: remove extra top/bottom padding
        textView.setIncludeFontPadding(false);

        // Get font metrics and calculate required inter-line extra
        Paint.FontMetricsInt metrics = textView.getPaint().getFontMetricsInt();
        final int extra = leading - metrics.descent + metrics.ascent;
        textView.setLineSpacing(extra, 1);

        // Determine minimum required top extra so that the view lands on the grid
        final int alignTopExtra = (step + metrics.ascent % step) % step;
        // Determine minimum required bottom extra so that view bounds are aligned with the grid
        final int alignBottomExtra = (step - metrics.descent % step) % step;

        textView.setPadding(textView.getPaddingLeft(), textView.getPaddingTop() + alignTopExtra,
                textView.getPaddingRight(), textView.getPaddingBottom() + alignBottomExtra);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mIsMultiline) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            int finalHeight = 0;
            if (mTitleView.getVisibility() != GONE && mSubtitleView.getVisibility() != GONE) {
                finalHeight = (int) ViewUtils.dpToPixel(TWO_LINE_ITEM_HEIGHT_DP);
            } else if (mAvatarView != null && mAvatarView.getVisibility() != GONE) {
                finalHeight = (int) ViewUtils.dpToPixel(SINGLE_LINE_AVATAR_ITEM_HEIGHT_DP);
            } else {
                finalHeight = (int) ViewUtils.dpToPixel(SINGLE_LINE_ITEM_HEIGHT_DP);
            }
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
        }
    }

    /**
     * Set a title that will appear in the first line.
     *
     * @param title a string title
     */
    public void setTitle(final String title) {
        mTitle = title;
        mTitleView.setText(title);
        mTitleView.setVisibility(TextUtils.isEmpty(title) ? GONE : VISIBLE);
    }

    /**
     * Set a subtitle that will appear in the second line.
     *
     * @param subtitle a string subtitle
     */
    public void setSubtitle(final String subtitle) {
        mSubtitle = subtitle;
        mSubtitleView.setText(subtitle);
        mSubtitleView.setVisibility(TextUtils.isEmpty(subtitle) ? GONE : VISIBLE);
    }

    /**
     * Inflates the menu items from
     * an xml resource.
     *
     * @param menuId a menu xml resource identifier
     */
    public void inflateMenu(final int menuId) {
        mMenuId = menuId;
        mMenuView.reset(menuId, mMenuItemsRoom);
    }

    /**
     * Allows multiline for title and subtitle.
     *
     * @param isMultiline a multiline flag
     */
    public void setMultiline(final boolean isMultiline) {
        mIsMultiline = isMultiline;
        if (isMultiline) {
            mPaddingVertical = (int) ViewUtils.dpToPixel(4);
            mItemLayout.setGravity(Gravity.TOP);
            ((LayoutParams) mIconView.getLayoutParams()).gravity = Gravity.TOP;
            mTitleView.setMaxLines(Integer.MAX_VALUE);
            mSubtitleView.setMaxLines(Integer.MAX_VALUE);
        } else {
            mPaddingVertical = 0;
            mItemLayout.setGravity(Gravity.CENTER_VERTICAL);
            ((LayoutParams) mIconView.getLayoutParams()).gravity = Gravity.CENTER_VERTICAL;
            mTitleView.setMaxLines(1);
            mSubtitleView.setMaxLines(1);
        }
        adjustPadding();
    }

    private void setIcon(Drawable iconDrawable) {
        mIconDrawable = iconDrawable;
        mIconView.setImageDrawable(iconDrawable);
        ViewUtils.setIconColor(mIconView, mIconColor);
        mIconView.setVisibility(iconDrawable == null ? GONE : VISIBLE);
    }

    /**
     * Check if item should display avatar.
     *
     * @return if item has avatar
     */
    public boolean hasAvatar() {
        return false;
    }

    /**
     * Check if item should display circular icon.
     *
     * @return if item has circular icon
     */
    public boolean hasCircularIcon() {
        return false;
    }

    /**
     * Check if item should display icon.
     *
     * @return if item has icon
     */
    public boolean hasIcon() {
        return false;
    }
}
