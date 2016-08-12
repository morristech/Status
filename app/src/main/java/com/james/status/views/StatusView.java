package com.james.status.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.james.status.R;
import com.james.status.data.icon.IconData;
import com.james.status.utils.ColorUtils;
import com.james.status.utils.ImageUtils;
import com.james.status.utils.PreferenceUtils;
import com.james.status.utils.StaticUtils;

import java.util.List;

public class StatusView extends FrameLayout {

    private LinearLayout status;
    private TextClock clock;
    private LinearLayout notificationIconLayout;

    @ColorInt
    private int color = 0;
    private boolean isSystemShowing, isDarkMode, isFullscreen;
    private ArrayMap<String, Notification> notifications;

    private List<IconData> icons;

    public StatusView(Context context) {
        super(context);
        setUp();
    }

    public StatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUp();
    }

    @TargetApi(21)
    public StatusView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setUp();
    }

    public void setUp() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.layout_status, null);
        status = (LinearLayout) v.findViewById(R.id.status);
        status.getLayoutParams().height = StaticUtils.getStatusBarHeight(getContext());

        clock = (TextClock) v.findViewById(R.id.clock);

        notificationIconLayout = (LinearLayout) v.findViewById(R.id.notificationIcons);

        Boolean isNotifications = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.SHOW_NOTIFICATIONS);
        if (isNotifications != null && !isNotifications)
            notificationIconLayout.setVisibility(View.GONE);

        VectorDrawableCompat.create(getResources(), R.drawable.ic_battery_alert, getContext().getTheme());

        Boolean isAmPmEnabled = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_CLOCK_AMPM);
        String format = isAmPmEnabled == null || isAmPmEnabled ? "h:mm a" : "h:mm";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            clock.setFormat12Hour(format);

        Boolean showClock = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.SHOW_CLOCK);
        if (showClock != null && !showClock) clock.setVisibility(View.GONE);

        addView(v);

        Boolean isStatusColorAuto = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_COLOR_AUTO);
        if (isStatusColorAuto != null && !isStatusColorAuto) {
            Integer statusBarColor = PreferenceUtils.getIntegerPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_COLOR);
            if (statusBarColor != null) setColor(statusBarColor);
        } else if (color > 0) setColor(color);
    }

    public void setIcons(List<IconData> icons) {
        this.icons = icons;

        for (IconData iconData : icons) {
            final View item = getIconView();

            iconData.setDrawableListener(new IconData.DrawableListener() {
                @Override
                public void onUpdate(@Nullable Drawable drawable) {
                    if (drawable != null) {
                        item.setVisibility(View.VISIBLE);
                        ((CustomImageView) item.findViewById(R.id.icon)).setImageDrawable(drawable);
                    } else item.setVisibility(View.GONE);
                }
            });

            iconData.setTextListener(new IconData.TextListener() {
                @Override
                public void onUpdate(@Nullable String text) {
                    TextView textView = (TextView) item.findViewById(R.id.text);
                    if (text != null) {
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(text);
                    } else textView.setVisibility(View.GONE);
                }
            });

            status.addView(item, 1);
        }
    }

    public List<IconData> getIcons() {
        return icons;
    }

    public void register() {
        for (IconData icon : icons) {
            icon.register();
        }
    }

    public void unregister() {
        for (IconData icon : icons) {
            icon.unregister();
        }
    }

    public void setNotifications(ArrayMap<String, Notification> notifications) {
        this.notifications = notifications;

        if (notificationIconLayout != null) {
            notificationIconLayout.removeAllViewsInLayout();
            for (Notification notification : notifications.values()) {
                View v = getIconView();
                Drawable drawable = getNotificationIcon(notification, null);

                if (drawable != null) {
                    CustomImageView icon = (CustomImageView) v.findViewById(R.id.icon);
                    icon.setImageDrawable(drawable);

                    if (isDarkMode) ImageUtils.setTint(icon, Color.BLACK);
                    notificationIconLayout.addView(v);
                }
            }
        }
    }

    public void addNotification(String key, Notification notification, @Nullable String packageName) {
        if (notifications == null) notifications = new ArrayMap<>();
        else {
            for (int i = 0; i < notifications.size(); i++) {
                if (notifications.containsKey(key)) {
                    notificationIconLayout.removeViewAt(i);
                    notifications.remove(key);
                }
            }
        }

        notifications.put(key, notification);

        if (notificationIconLayout != null) {
            View v = getIconView();
            Drawable drawable = getNotificationIcon(notification, packageName);

            if (drawable != null) {
                CustomImageView icon = (CustomImageView) v.findViewById(R.id.icon);
                icon.setImageDrawable(drawable);

                if (isDarkMode) ImageUtils.setTint(icon, Color.BLACK);
                notificationIconLayout.addView(v);
            }
        }
    }

    public void removeNotification(String key) {
        ArrayMap<String, Notification> notifications = new ArrayMap<>();
        if (this.notifications != null) {
            for (String key2 : this.notifications.keySet()) {
                if (!key.matches(key2))
                    notifications.put(key2, this.notifications.get(key2));
            }
        }

        setNotifications(notifications);
    }

    public void setSystemShowing(boolean isSystemShowing) {
        if (this.isFullscreen != isSystemShowing || this.isSystemShowing != isSystemShowing) {
            ValueAnimator animator = ValueAnimator.ofFloat(getY(), isSystemShowing ? -StaticUtils.getStatusBarHeight(getContext()) : 0f);
            animator.setDuration(150);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float y = (float) valueAnimator.getAnimatedValue();
                    setY(y);
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (StatusView.this.isSystemShowing) setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            animator.start();

            if (!isSystemShowing) setVisibility(View.VISIBLE);
        }

        this.isSystemShowing = isSystemShowing;
    }

    public void setFullscreen(boolean isFullscreen) {
        if (this.isFullscreen != isFullscreen && !isSystemShowing) {
            ValueAnimator animator = ValueAnimator.ofFloat(getY(), isFullscreen ? -StaticUtils.getStatusBarHeight(getContext()) : 0f);
            animator.setDuration(150);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float y = (float) valueAnimator.getAnimatedValue();
                    setY(y);
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (StatusView.this.isFullscreen) setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            animator.start();

            if (!isFullscreen) setVisibility(View.VISIBLE);

            this.isFullscreen = isFullscreen;
        }
    }

    public void setColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator animator = ValueAnimator.ofArgb(this.color, color);
            animator.setDuration(150);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int color = (int) valueAnimator.getAnimatedValue();
                    if (status != null)
                        status.setBackgroundColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
                    setDarkMode(!ColorUtils.isColorDark(color));
                }
            });
            animator.start();
        } else {
            if (status != null)
                status.setBackgroundColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
            setDarkMode(!ColorUtils.isColorDark(color));
        }

        this.color = color;
    }

    public void setDarkMode(boolean isDarkMode) {
        Boolean isDarkModeEnabled = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_DARK_ICONS);

        if (this.isDarkMode != isDarkMode && (isDarkModeEnabled == null || isDarkModeEnabled)) {
            setDarkView(status, isDarkMode ? Color.BLACK : Color.WHITE);
            this.isDarkMode = isDarkMode;
        }
    }

    private void setDarkView(View view, int color) {
        if (view instanceof LinearLayout) {
            for (int i = 0; i < ((LinearLayout) view).getChildCount(); i++) {
                setDarkView(((LinearLayout) view).getChildAt(i), color);
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        } else if (view instanceof CustomImageView) {
            ImageUtils.setTint((CustomImageView) view, color);
        }
    }

    public void setLockscreen(boolean lockscreen) {
        Boolean expand = PreferenceUtils.getBooleanPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_LOCKSCREEN_EXPAND);
        if (expand != null && expand)
            status.getLayoutParams().height = StaticUtils.getStatusBarHeight(getContext()) * (lockscreen ? 3 : 1);

        if (lockscreen) {
            Palette.from(ImageUtils.drawableToBitmap(WallpaperManager.getInstance(getContext()).getFastDrawable())).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    setColor(palette.getDarkVibrantColor(ColorUtils.darkColor(palette.getVibrantColor(Color.BLACK))));
                }
            });
        }
    }

    private View getIconView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_icon, null);

        Integer iconPadding = PreferenceUtils.getIntegerPreference(getContext(), PreferenceUtils.PreferenceIdentifier.STATUS_ICON_PADDING);
        if (iconPadding == null) iconPadding = 2;

        float iconPaddingDp = StaticUtils.getPixelsFromDp(getContext(), iconPadding);

        v.setPadding((int) iconPaddingDp, 0, (int) iconPaddingDp, 0);

        return v;
    }

    @Nullable
    private Drawable getNotificationIcon(Notification notification, @Nullable String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Resources resources = null;
            PackageInfo packageInfo = null;

            if (packageName == null) {
                if (notification.contentIntent != null)
                    packageName = notification.contentIntent.getCreatorPackage();
                else if (notification.deleteIntent != null)
                    packageName = notification.deleteIntent.getCreatorPackage();
                else if (notification.fullScreenIntent != null)
                    packageName = notification.fullScreenIntent.getCreatorPackage();
                else if (notification.actions != null && notification.actions.length > 0)
                    packageName = notification.actions[0].actionIntent.getCreatorPackage();
                else return null;
            }

            try {
                resources = getContext().getPackageManager().getResourcesForApplication(packageName);
                packageInfo = getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            if (resources != null && packageInfo != null) {
                Resources.Theme theme = resources.newTheme();
                theme.applyStyle(packageInfo.applicationInfo.theme, false);

                Drawable drawable = null;
                try {
                    drawable = ResourcesCompat.getDrawable(resources, notification.icon, theme);
                } catch (Resources.NotFoundException ignored) {
                }

                return drawable;
            }

        } else
            return notification.getSmallIcon().loadDrawable(getContext());

        return null;
    }
}
