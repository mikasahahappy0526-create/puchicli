package jp.puchicli.app.ui.home;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class EfficiencyTextsTest {
    private static int failed;

    public static void main(String[] args) {
        phase();
        swipe();
        reservation();
        permissions();
        stop();
        path();
        if (failed > 0) {
            throw new AssertionError(failed + " assertion(s) failed");
        }
        System.out.println("EfficiencyTextsTest ok");
    }

    private static void phase() {
        eq("停止中", EfficiencyTexts.phase(true, true, true));
        eq("開始中", EfficiencyTexts.phase(false, true, false));
        eq("実行中", EfficiencyTexts.phase(true, false, false));
        eq("待機", EfficiencyTexts.phase(false, false, false));
        eq("開始", EfficiencyTexts.primaryLabel(false, false, false));
        eq("停止", EfficiencyTexts.primaryLabel(true, false, false));
        eq("開始中", EfficiencyTexts.primaryLabel(false, true, false));
        check(!EfficiencyTexts.primaryStops(false, false, false, false));
        check(EfficiencyTexts.primaryStops(false, false, false, true));
        check(EfficiencyTexts.primaryStops(true, false, false, false));
    }

    private static void swipe() {
        eq("次のスワイプ：約8秒", EfficiencyTexts.nextSwipe(8000L, 1000L, 1000L));
        eq("次のスワイプ：約1秒", EfficiencyTexts.nextSwipe(8000L, 7500L, 0L));
        eq("次のスワイプ：約0秒", EfficiencyTexts.nextSwipe(8000L, 9000L, 0L));
        eq("00:01:05", EfficiencyTexts.hms(65000L));
        eq("経過 00:00:01　残り 01:59:59", EfficiencyTexts.runningTimes(1000L, (2 * 3600L - 1) * 1000L));
    }

    private static void reservation() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.of(2026, 9, 28, 1, 0), zone);
        long nowMs = now.toInstant().toEpochMilli();
        EfficiencyTexts.Reservation none = EfficiencyTexts.reservation(false, 2, 0, 0L, 0L, nowMs, zone);
        check(!none.present);
        eq("次の予約：予約なし", none.line);

        EfficiencyTexts.Reservation daily = EfficiencyTexts.reservation(true, 2, 5, 0L, 0L, nowMs, zone);
        check(daily.present);
        eq("02:05", daily.hhmm);
        eq("次の予約：02:05", daily.line);

        ZonedDateTime later = ZonedDateTime.of(LocalDateTime.of(2026, 9, 28, 3, 0), zone);
        EfficiencyTexts.Reservation tomorrow = EfficiencyTexts.reservation(true, 2, 5, 0L, 0L, later.toInstant().toEpochMilli(), zone);
        eq("02:05", tomorrow.hhmm);

        long oneShot = now.plusMinutes(30).toInstant().toEpochMilli();
        EfficiencyTexts.Reservation sooner = EfficiencyTexts.reservation(true, 2, 5, oneShot, 0L, nowMs, zone);
        eq("01:30", sooner.hhmm);

        check(EfficiencyTexts.isPending(nowMs, nowMs));
        check(!EfficiencyTexts.isPending(0L, nowMs));
        check(!EfficiencyTexts.isPending(nowMs - 5000L, nowMs));
    }

    private static void permissions() {
        eq("準備完了", EfficiencyTexts.permissionLine(0));
        eq("権限NG 2件", EfficiencyTexts.permissionLine(2));
        check(EfficiencyTexts.startAllowed(true, true, true));
        check(!EfficiencyTexts.startAllowed(true, true, false));
        check(!EfficiencyTexts.startAllowed(false, true, true));
        eq("次のスワイプ：約3秒", EfficiencyTexts.detail(true, "次のスワイプ：約3秒", "次の予約：予約なし"));
        eq("次の予約：予約なし", EfficiencyTexts.detail(false, "次のスワイプ：約3秒", "次の予約：予約なし"));
        eq("実行中 · 次のスワイプ：約3秒 · 経過 00:00:01　残り 00:01:00",
                EfficiencyTexts.notificationLine("実行中", "次のスワイプ：約3秒", "経過 00:00:01　残り 00:01:00"));
    }

    private static void stop() {
        eq("手動で停止しました", EfficiencyTexts.stopTitle("USER"));
        eq("制限時間に達しました", EfficiencyTexts.stopTitle("TIME_LIMIT"));
        eq("ユーザー補助がオフです", EfficiencyTexts.stopTitle("ACCESSIBILITY_LOST"));
        eq("パネルを閉じました", EfficiencyTexts.stopTitle("CLOSE"));
        eq("再開", EfficiencyTexts.stopAction("USER", false, true));
        eq("新しく開始", EfficiencyTexts.stopAction("TIME_LIMIT", false, true));
        eq("権限を設定", EfficiencyTexts.stopAction("TIME_LIMIT", true, true));
        eq("権限を設定", EfficiencyTexts.stopAction("ACCESSIBILITY_LOST", false, false));
        eq("再開", EfficiencyTexts.stopAction("ACCESSIBILITY_LOST", false, true));
        eq("再開", EfficiencyTexts.stopAction("CLOSE", false, true));
        eq("権限を設定", EfficiencyTexts.stopAction("CLOSE", true, false));
    }

    private static void path() {
        eq("既定", EfficiencyTexts.pathValue(false));
        eq("カスタム", EfficiencyTexts.pathValue(true));
        eq("オン", EfficiencyTexts.onOff(true));
        eq("オフ", EfficiencyTexts.onOff(false));
        eq("09:05", EfficiencyTexts.hhmm(9, 5));
        eq("23:59", EfficiencyTexts.hhmm(99, 99));
    }

    private static void eq(String expect, String actual) {
        if (!expect.equals(actual)) {
            failed++;
            System.out.println("FAIL expected [" + expect + "] actual [" + actual + "]");
        }
    }

    private static void check(boolean value) {
        if (!value) {
            failed++;
            System.out.println("FAIL expected true");
        }
    }
}
