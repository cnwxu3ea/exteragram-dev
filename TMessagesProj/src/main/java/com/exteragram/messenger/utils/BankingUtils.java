package com.exteragram.messenger.utils;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankingUtils {

    private static final List<BankingApp> banks = new ArrayList<>();

    // array of banks, that doesn't work for now and idk why
    private static final String[] skip = new String[]{"ru.raiffeisennews"};

    static {
        updateBanks();
    }

    public static List<BankingApp> getBanks() {
        updateBanks();
        return banks;
    }

    private static void updateBanks() {
        if (!banks.isEmpty()) {
            banks.clear();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:00000000000"));

        PackageManager packageManager = ApplicationLoader.applicationContext.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !Arrays.asList(skip).contains(packageName)) {
                addBank(packageName, resolveInfo.activityInfo.name);
            }
        }
    }

    private static void addBank(String packageName, String activityName) {
        if (SystemUtils.isAppInstalled(packageName)) {
            banks.add(new BankingApp(packageName, activityName));
        }
    }

    public static class BankingApp {
        private final String activityName;
        private final String packageName;
        private final String appName;
        private final Drawable appIcon;

        public BankingApp(String packageName, String activityName) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.appIcon = fetchIcon();
            this.appName = fetchName();
        }

        public String getName() {
            return appName;
        }

        public Drawable getIcon() {
            return appIcon;
        }

        private String fetchName() {
            final PackageManager pm = ApplicationLoader.applicationContext.getPackageManager();
            ApplicationInfo info = null;
            String name = null;
            try {
                info = pm.getApplicationInfo(packageName, 0);
            } catch (Exception e) {
                name = LocaleController.getString(R.string.NumberUnknown);
            }
            return info == null ? name : (String) pm.getApplicationLabel(info);
        }

        private Drawable fetchIcon() {
            Drawable icon;
            try {
                icon = ApplicationLoader.applicationContext.getPackageManager().getApplicationIcon(packageName);
                icon = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), CanvasUtils.drawableToBitmap(icon, dp(24), dp(24)));
            } catch (Exception e) {
                icon = ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_payment_provider);
            }
            return icon;
        }

        public void open(Activity activity, String phone) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + phone));
                intent.setClassName(packageName, activityName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivityForResult(intent, 500);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }
}