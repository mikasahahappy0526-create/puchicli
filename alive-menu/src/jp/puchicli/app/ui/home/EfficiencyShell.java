package jp.puchicli.app.ui.home;

import android.app.Activity;
import android.app.Application;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Home and settings placed over the existing screens, using the same start and settings store. */
public final class EfficiencyShell implements Application.ActivityLifecycleCallbacks {
    private static final int BG = 0xFFF4FBF9;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF102623;
    private static final int MUTED = 0xFF3F4947;
    private static final int START = 0xFF15803D;
    private static final int STOP = 0xFFDC2626;
    private static final int NG = 0xFFB91C1C;

    private static final EfficiencyShell INSTANCE = new EfficiencyShell();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Activity activity;
    private Runnable ticker;
    private View shell;
    private View page;
    private View legacyBack;
    private boolean settingsBuilt;
    private boolean legacyOpen;
    private boolean settingsPage;
    private TextView phaseView;
    private TextView detailView;
    private TextView timesView;
    private TextView permView;
    private TextView missingView;
    private Button primary;
    private LinearLayout reasonCard;
    private TextView reasonText;
    private Button reasonAction;
    private TextView pathView;
    private TextView scheduleView;
    private TextView nextView;
    private TextView checkinView;
    private TextView dayView;
    private Button cancelButton;
    private LinearLayout settingsList;

    private EfficiencyShell() {
    }

    public static void install(Application application) {
        application.unregisterActivityLifecycleCallbacks(INSTANCE);
        application.registerActivityLifecycleCallbacks(INSTANCE);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (!isMain(activity)) {
            return;
        }
        activity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                attach(activity);
            }
        });
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!isMain(activity)) {
            return;
        }
        this.activity = activity;
        if (shell == null) {
            attach(activity);
        }
        settingsBuilt = false;
        Intent intent = activity.getIntent();
        if (intent != null && ("settings".equals(intent.getStringExtra(EfficiencyOverlay.PAGE_EXTRA))
                || intent.getBooleanExtra("jp.puchicli.app.OPEN_SETTINGS", false))) {
            settingsPage = true;
            legacyOpen = false;
            settingsBuilt = false;
        }
        startTicker();
        refresh();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isMain(activity)) {
            stopTicker();
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (this.activity == activity) {
            stopTicker();
            this.activity = null;
            shell = null;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    private void attach(Activity activity) {
        if (!isMain(activity)) {
            return;
        }
        this.activity = activity;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag("efficiency-shell") != null) {
            shell = content == null ? null : content.findViewWithTag("efficiency-shell");
            return;
        }
        float d = activity.getResources().getDisplayMetrics().density;
        int pad = dp(d, 16);
        FrameLayout host = new FrameLayout(activity);
        host.setTag("efficiency-shell");
        host.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(pad, pad + statusBar(activity), pad, pad);
        root.setClickable(true);
        host.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout status = card(activity, d);
        phaseView = text(activity, 22, TEXT, true);
        detailView = text(activity, 16, TEXT, false);
        timesView = text(activity, 14, MUTED, false);
        permView = text(activity, 15, TEXT, true);
        missingView = text(activity, 14, NG, false);
        status.addView(phaseView);
        status.addView(detailView);
        status.addView(timesView);
        status.addView(permView);
        status.addView(missingView);
        root.addView(status);

        primary = new Button(activity);
        primary.setAllCaps(false);
        primary.setTextSize(22);
        primary.setTypeface(Typeface.DEFAULT_BOLD);
        primary.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams primaryLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(d, 88));
        primaryLp.topMargin = dp(d, 12);
        primary.setLayoutParams(primaryLp);
        primary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrimary();
            }
        });
        root.addView(primary);

        reasonCard = card(activity, d);
        ((LinearLayout.LayoutParams) reasonCard.getLayoutParams()).topMargin = dp(d, 10);
        reasonText = text(activity, 15, TEXT, true);
        reasonAction = button(activity, EfficiencyTexts.ACTION_RESUME, false);
        reasonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReason();
            }
        });
        reasonCard.addView(reasonText);
        reasonCard.addView(reasonAction);
        reasonCard.setVisibility(View.GONE);
        root.addView(reasonCard);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = dp(d, 10);
        root.addView(scroll, scrollLp);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout drive = card(activity, d);
        TextView driveTitle = text(activity, 16, TEXT, true);
        driveTitle.setText("運転設定");
        drive.addView(driveTitle);
        pathView = text(activity, 15, TEXT, false);
        scheduleView = text(activity, 15, TEXT, false);
        nextView = text(activity, 15, TEXT, false);
        checkinView = text(activity, 15, TEXT, false);
        dayView = text(activity, 15, TEXT, false);
        drive.addView(pathView);
        drive.addView(rowButtons(activity, "変更", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editPath();
            }
        }, "既定に戻す", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runStore(new StoreOp() {
                    @Override
                    public void run(Context context) throws ReflectiveOperationException {
                        EfficiencyStore.useDefaultPath(context);
                    }
                });
            }
        }));
        drive.addView(scheduleView);
        drive.addView(nextView);
        drive.addView(rowButtons(activity, "時刻を変更", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editScheduleTime();
            }
        }, "オン／オフ", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSchedule();
            }
        }));
        cancelButton = button(activity, "次回の予約を取消", false);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runStore(new StoreOp() {
                    @Override
                    public void run(Context context) throws ReflectiveOperationException {
                        EfficiencyStore.cancelNextReservation(context);
                    }
                });
            }
        });
        drive.addView(cancelButton);
        drive.addView(checkinView);
        drive.addView(dayView);
        drive.addView(rowButtons(activity, "日数を変更", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDay();
            }
        }, "設定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        }));
        body.addView(drive);

        settingsList = new LinearLayout(activity);
        settingsList.setOrientation(LinearLayout.VERTICAL);
        settingsList.setVisibility(View.GONE);
        body.addView(settingsList, 0);

        Button legacy = button(activity, "詳細設定", false);
        legacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLegacy();
            }
        });
        body.addView(legacy);

        legacyBack = button(activity, "運転画面へ", true);
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        backLp.topMargin = pad + statusBar(activity);
        backLp.rightMargin = pad;
        legacyBack.setLayoutParams(backLp);
        legacyBack.setVisibility(View.GONE);
        legacyBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                legacyOpen = false;
                settingsPage = false;
                EfficiencyNav.show("home");
                refresh();
            }
        });
        host.addView(legacyBack);
        content.addView(host);
        shell = host;
        page = root;
        refresh();
    }

    private void showSettings() {
        settingsPage = true;
        legacyOpen = false;
        refresh();
    }

    private void openLegacy() {
        legacyOpen = true;
        EfficiencyNav.show("settings");
        refresh();
    }

    private void startTicker() {
        stopTicker();
        ticker = new Runnable() {
            @Override
            public void run() {
                refresh();
                handler.postDelayed(this, 400L);
            }
        };
        handler.post(ticker);
    }

    private void stopTicker() {
        if (ticker != null) {
            handler.removeCallbacks(ticker);
        }
    }

    private void refresh() {
        Activity activity = this.activity;
        if (activity == null || shell == null || phaseView == null) {
            return;
        }
        String screen = screenValue();
        if ("onboarding".equals(screen)) {
            shell.setVisibility(View.GONE);
            return;
        }
        shell.setVisibility(View.VISIBLE);
        if (legacyOpen && "home".equals(screen)) {
            legacyOpen = false;
        }
        if (legacyOpen) {
            page.setVisibility(View.GONE);
            legacyBack.setVisibility(View.VISIBLE);
            return;
        }
        page.setVisibility(View.VISIBLE);
        legacyBack.setVisibility(View.GONE);
        EfficiencyOverlay.Snapshot snap = EfficiencyOverlay.snapshot(activity);
        phaseView.setText(snap.phase);
        detailView.setText(snap.detail);
        if (snap.running && snap.times.length() > 0) {
            timesView.setVisibility(View.VISIBLE);
            timesView.setText(snap.times);
        } else {
            timesView.setVisibility(View.GONE);
        }
        permView.setText(snap.permissionLine);
        permView.setTextColor(snap.startAllowed ? START : NG);
        if (snap.missingNames.length() > 0) {
            missingView.setVisibility(View.VISIBLE);
            missingView.setText(snap.missingNames);
        } else {
            missingView.setVisibility(View.GONE);
        }
        primary.setText(EfficiencyTexts.primaryLabel(snap.running, snap.starting, snap.stopping));
        boolean busy = snap.starting || snap.stopping;
        primary.setEnabled(!busy);
        primary.setBackgroundColor(snap.running ? STOP : START);
        int homeVisibility = settingsPage ? View.GONE : View.VISIBLE;
        ((View) phaseView.getParent()).setVisibility(homeVisibility);
        primary.setVisibility(homeVisibility);
        String title = snap.reason == null ? "" : EfficiencyTexts.stopTitle(snap.reason);
        if (!settingsPage && !snap.running && !snap.starting && !snap.stopping && title.length() > 0) {
            reasonCard.setVisibility(View.VISIBLE);
            reasonText.setText(title);
            reasonAction.setText(EfficiencyTexts.stopAction(snap.reason, !snap.startAllowed, snap.accessibility));
        } else {
            reasonCard.setVisibility(View.GONE);
        }
        try {
            Object settings = EfficiencyStore.settings(activity);
            pathView.setText("経路：" + EfficiencyTexts.pathValue(EfficiencyStore.customPath(settings)));
            boolean scheduleOn = EfficiencyStore.scheduleEnabled(settings);
            scheduleView.setText("毎日の開始予約：" + EfficiencyTexts.onOff(scheduleOn)
                    + "　" + EfficiencyTexts.hhmm(EfficiencyStore.scheduleHour(settings), EfficiencyStore.scheduleMinute(settings)));
            EfficiencyTexts.Reservation reservation = EfficiencyTexts.reservation(
                    scheduleOn,
                    EfficiencyStore.scheduleHour(settings),
                    EfficiencyStore.scheduleMinute(settings),
                    EfficiencyStore.oneShotAt(settings),
                    EfficiencyStore.adminTwoAt(settings),
                    System.currentTimeMillis(),
                    java.time.ZoneId.systemDefault());
            nextView.setText("次回：" + (reservation.present ? reservation.hhmm : EfficiencyTexts.NO_RESERVATION));
            cancelButton.setVisibility(reservation.present ? View.VISIBLE : View.GONE);
            if (EfficiencyStore.showCheckinSchedule()) {
                checkinView.setVisibility(View.VISIBLE);
                checkinView.setText("チェックイン予約：" + EfficiencyTexts.onOff(EfficiencyStore.checkinEnabled(settings))
                        + "　" + EfficiencyTexts.hhmm(EfficiencyStore.checkinHour(settings), EfficiencyStore.checkinMinute(settings)));
            } else {
                checkinView.setVisibility(View.GONE);
            }
            dayView.setText(EfficiencyStore.installDay(activity) + "日目");
        } catch (ReflectiveOperationException ignored) {
        }
        if (settingsPage) {
            settingsList.setVisibility(View.VISIBLE);
            if (!settingsBuilt) {
                rebuildSettings(activity);
                settingsBuilt = true;
            }
        } else {
            settingsList.setVisibility(View.GONE);
            settingsBuilt = false;
        }
    }

    private void rebuildSettings(Activity activity) {
        settingsList.removeAllViews();
        float d = activity.getResources().getDisplayMetrics().density;
        Button back = button(activity, "ホームへ", false);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsPage = false;
                refresh();
            }
        });
        settingsList.addView(back);
        TextView title = text(activity, 18, TEXT, true);
        title.setText("開始に必要な権限");
        title.setPadding(0, dp(d, 8), 0, 0);
        settingsList.addView(title);
        final List<Perm> required = requiredPerms(activity);
        for (Perm perm : required) {
            settingsList.addView(permRow(activity, perm));
        }
        Button openNext = button(activity, "未設定を順に開く", true);
        openNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFirstMissing(activity);
            }
        });
        settingsList.addView(openNext);

        TextView extra = text(activity, 18, TEXT, true);
        extra.setText("予約を使う場合に必要");
        extra.setPadding(0, dp(d, 12), 0, 0);
        settingsList.addView(extra);
        boolean scheduleOn = false;
        try {
            scheduleOn = EfficiencyStore.scheduleEnabled(EfficiencyStore.settings(activity));
        } catch (ReflectiveOperationException ignored) {
        }
        if (!scheduleOn) {
            TextView note = text(activity, 13, MUTED, false);
            note.setText("予約オフのため補足です。開始はブロックしません。");
            settingsList.addView(note);
        }
        for (Perm perm : schedulePerms(activity)) {
            settingsList.addView(permRow(activity, perm));
        }
        TextView same = text(activity, 13, MUTED, false);
        same.setText("経路・予約時刻・日数は上の運転設定と同じ編集です。");
        same.setPadding(0, dp(d, 8), 0, 0);
        settingsList.addView(same);
    }

    private void openFirstMissing(Activity activity) {
        List<Perm> all = new ArrayList<Perm>();
        all.addAll(requiredPerms(activity));
        all.addAll(schedulePerms(activity));
        for (Perm perm : all) {
            if (!perm.ok) {
                perm.open.run();
                return;
            }
        }
        Toast.makeText(activity, "未設定の項目はありません", Toast.LENGTH_SHORT).show();
    }

    private List<Perm> requiredPerms(Activity activity) {
        List<Perm> list = new ArrayList<Perm>();
        final Activity host = activity;
        boolean overlay = flag("jp.puchicli.app.util.PermissionHelper", "canDrawOverlays", host);
        boolean a11y = flag("jp.puchicli.app.util.PermissionHelper", "accessibilityReady", host) && gestureBound();
        boolean lite = flag("jp.puchicli.app.target.TargetApps", "anyKnownInstalled", host);
        list.add(new Perm("他のアプリの上に表示（オーバーレイ）", overlay, "許可済み", "未設定", new Runnable() {
            @Override
            public void run() {
                flag("jp.puchicli.app.util.PermissionHelper", "openOverlaySettingsScreen", host);
            }
        }));
        list.add(new Perm("ユーザー補助（アライブ）", a11y, "許可済み", "未設定", new Runnable() {
            @Override
            public void run() {
                startIntent("jp.puchicli.app.util.PermissionHelper", "accessibilityIntent", host);
            }
        }));
        list.add(new Perm("TikTok Lite", lite, "インストール済み", "未インストール", new Runnable() {
            @Override
            public void run() {
                openLiteStore(host);
            }
        }));
        return list;
    }

    private List<Perm> schedulePerms(Activity activity) {
        List<Perm> list = new ArrayList<Perm>();
        final Activity host = activity;
        boolean alarm = flag("jp.puchicli.app.util.PermissionHelper", "canScheduleExactAlarms", host);
        boolean notes = flag("jp.puchicli.app.wake.WakeNotificationPolicy", "notificationsUsable", host);
        boolean battery = flag("jp.puchicli.app.util.PermissionHelper", "ignoringBatteryOptimizations", host);
        list.add(new Perm("正確なアラーム", alarm, "許可済み", "未設定", new Runnable() {
            @Override
            public void run() {
                startIntentWithContext("jp.puchicli.app.util.PermissionHelper", "exactAlarmIntent", host);
            }
        }));
        list.add(new Perm("通知", notes, "許可済み", "未設定", new Runnable() {
            @Override
            public void run() {
                flag("jp.puchicli.app.wake.WakeNotificationPolicy", "openNotificationSettings", host);
            }
        }));
        list.add(new Perm("バックグラウンド実行／電池最適化除外", battery, "許可済み", "未設定", new Runnable() {
            @Override
            public void run() {
                if (!flag("jp.puchicli.app.util.PermissionHelper", "openBackgroundRunSettings", host)) {
                    flag("jp.puchicli.app.util.PermissionHelper", "openBatterySettingsScreen", host);
                }
            }
        }));
        return list;
    }

    private View permRow(Activity activity, final Perm perm) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity.getResources().getDisplayMetrics().density, 6), 0, 0);
        TextView name = text(activity, 14, TEXT, false);
        name.setText(perm.title);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        name.setLayoutParams(nameLp);
        TextView state = text(activity, 14, perm.ok ? START : NG, true);
        state.setText(perm.ok ? perm.okLabel : perm.ngLabel);
        row.addView(name);
        row.addView(state);
        if (!perm.ok) {
            Button open = button(activity, "開く", true);
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    perm.open.run();
                }
            });
            row.addView(open);
        }
        return row;
    }

    private void onPrimary() {
        Activity activity = this.activity;
        if (activity == null || !EfficiencyOverlay.allowTap()) {
            return;
        }
        EfficiencyOverlay.Snapshot snap = EfficiencyOverlay.snapshot(activity);
        if (EfficiencyTexts.primaryStops(snap.running, snap.starting, snap.stopping, false)) {
            launch(activity, "jp.puchicli.app.action.STOP", false);
            refresh();
            return;
        }
        if (!snap.startAllowed) {
            showSettings();
            return;
        }
        if (airplane(activity)) {
            return;
        }
        requestNotifications(activity);
        launch(activity, "jp.puchicli.app.action.START", dim());
        activity.moveTaskToBack(true);
    }

    private void onReason() {
        Activity activity = this.activity;
        if (activity == null || !EfficiencyOverlay.allowTap()) {
            return;
        }
        EfficiencyOverlay.Snapshot snap = EfficiencyOverlay.snapshot(activity);
        String label = EfficiencyTexts.stopAction(snap.reason, !snap.startAllowed, snap.accessibility);
        if (EfficiencyTexts.ACTION_SETTINGS.equals(label)) {
            showSettings();
            return;
        }
        if (airplane(activity)) {
            return;
        }
        requestNotifications(activity);
        launch(activity, "jp.puchicli.app.action.START", dim());
        activity.moveTaskToBack(true);
    }

    private void editPath() {
        final Activity activity = this.activity;
        if (activity == null) {
            return;
        }
        new android.app.AlertDialog.Builder(activity)
                .setTitle("経路")
                .setMessage("既定に戻すか、これまでの詳細設定で経路を編集します。")
                .setPositiveButton("既定に戻す", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        runStore(new StoreOp() {
                            @Override
                            public void run(Context context) throws ReflectiveOperationException {
                                EfficiencyStore.useDefaultPath(context);
                            }
                        });
                    }
                })
                .setNeutralButton("詳細で編集", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        openLegacy();
                    }
                })
                .setNegativeButton("やめる", null)
                .show();
    }

    private void editScheduleTime() {
        final Activity activity = this.activity;
        if (activity == null) {
            return;
        }
        int hour = 2;
        int minute = 0;
        try {
            Object settings = EfficiencyStore.settings(activity);
            hour = EfficiencyStore.scheduleHour(settings);
            minute = EfficiencyStore.scheduleMinute(settings);
        } catch (ReflectiveOperationException ignored) {
        }
        new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker view, final int h, final int m) {
                runStore(new StoreOp() {
                    @Override
                    public void run(Context context) throws ReflectiveOperationException {
                        EfficiencyStore.setSchedule(context, true, h, m);
                    }
                });
            }
        }, hour, minute, true).show();
    }

    private void toggleSchedule() {
        final Activity activity = this.activity;
        if (activity == null) {
            return;
        }
        try {
            final Object settings = EfficiencyStore.settings(activity);
            final boolean next = !EfficiencyStore.scheduleEnabled(settings);
            runStore(new StoreOp() {
                @Override
                public void run(Context context) throws ReflectiveOperationException {
                    EfficiencyStore.setSchedule(context, next,
                            EfficiencyStore.scheduleHour(settings),
                            EfficiencyStore.scheduleMinute(settings));
                }
            });
        } catch (ReflectiveOperationException e) {
            toast(activity, "予約を更新できませんでした");
        }
    }

    private void editDay() {
        final Activity activity = this.activity;
        if (activity == null) {
            return;
        }
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(EfficiencyStore.installDay(activity)));
        String title = string(activity, "home_install_day_edit_title", "1日目の日付");
        new android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage("今日を何日目にするかです。")
                .setView(input)
                .setPositiveButton(string(activity, "home_install_day_edit_ok", "決める"),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                try {
                                    int day = Integer.parseInt(input.getText().toString().trim());
                                    EfficiencyStore.setInstallDay(activity, day);
                                    refresh();
                                } catch (Exception e) {
                                    toast(activity, "日数を更新できませんでした");
                                }
                            }
                        })
                .setNegativeButton(string(activity, "home_install_day_edit_cancel", "やめる"), null)
                .show();
    }

    private void runStore(StoreOp op) {
        Activity activity = this.activity;
        if (activity == null) {
            return;
        }
        try {
            op.run(activity);
            refresh();
        } catch (ReflectiveOperationException e) {
            toast(activity, "設定を保存できませんでした");
        }
    }

    private void launch(Context context, String action, boolean dim) {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.overlay.OverlayService");
            Object companion = type.getField("Companion").get(null);
            companion.getClass().getMethod("launch", Context.class, String.class, boolean.class)
                    .invoke(companion, context, action, dim);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private boolean dim() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.overlay.ScreenDimPolicy");
            Object instance = type.getField("INSTANCE").get(null);
            return Boolean.TRUE.equals(type.getMethod("shouldDimForManualHomeStart").invoke(instance));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private boolean airplane(Context context) {
        return flag("jp.puchicli.app.session.AirplaneModePolicy", "refuseIfOn", context);
    }

    private void requestNotifications(Activity activity) {
        try {
            java.lang.reflect.Method method = activity.getClass().getDeclaredMethod("requestNotificationPermission");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean gestureBound() {
        try {
            Class<?> type = Class.forName("jp.puchicli.app.a11y.GestureBridge");
            Object instance = type.getField("INSTANCE").get(null);
            return type.getMethod("getService").invoke(instance) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean flag(String className, String method, Context context) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod(method, Context.class).invoke(instance, context);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static void startIntentWithContext(String className, String method, Context context) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod(method, Context.class).invoke(instance, context);
            if (value instanceof Intent) {
                Intent intent = (Intent) value;
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void startIntent(String className, String method, Context context) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getField("INSTANCE").get(null);
            Object value = type.getMethod(method).invoke(instance);
            if (value instanceof Intent) {
                Intent intent = (Intent) value;
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void openLiteStore(Context context) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.zhiliaoapp.musically.go"));
        market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(market);
        } catch (RuntimeException e) {
            Intent web = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.zhiliaoapp.musically.go"));
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(web);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static String screenValue() {
        try {
            java.lang.reflect.Field field = EfficiencyNav.class.getDeclaredField("screen");
            field.setAccessible(true);
            Object state = field.get(null);
            if (state == null) {
                return null;
            }
            Object value = state.getClass().getMethod("getValue").invoke(state);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String string(Context context, String name, String fallback) {
        int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
        if (id == 0) {
            return fallback;
        }
        return context.getString(id);
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private static boolean isMain(Activity activity) {
        return activity != null && "jp.puchicli.app.MainActivity".equals(activity.getClass().getName());
    }

    private static int statusBar(Activity activity) {
        int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id == 0) {
            return 0;
        }
        return activity.getResources().getDimensionPixelSize(id);
    }

    private static int dp(float density, int value) {
        return (int) (value * density + 0.5f);
    }

    private static LinearLayout card(Context context, float density) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(density, 14);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(density, 16));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        return card;
    }

    private static TextView text(Context context, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        view.setPadding(0, 2, 0, 2);
        return view;
    }

    private static Button button(Context context, String label, boolean filled) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(filled ? 0xFFFFFFFF : TEXT);
        if (filled) {
            button.setBackgroundColor(START);
        }
        return button;
    }

    private static LinearLayout rowButtons(Context context, String a, View.OnClickListener aClick, String b, View.OnClickListener bClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button left = button(context, a, true);
        Button right = button(context, b, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(lp);
        right.setLayoutParams(lp);
        left.setOnClickListener(aClick);
        right.setOnClickListener(bClick);
        row.addView(left);
        row.addView(right);
        return row;
    }

    private interface StoreOp {
        void run(Context context) throws ReflectiveOperationException;
    }

    private static final class Perm {
        final String title;
        final boolean ok;
        final String okLabel;
        final String ngLabel;
        final Runnable open;

        Perm(String title, boolean ok, String okLabel, String ngLabel, Runnable open) {
            this.title = title;
            this.ok = ok;
            this.okLabel = okLabel;
            this.ngLabel = ngLabel;
            this.open = open;
        }
    }
}
