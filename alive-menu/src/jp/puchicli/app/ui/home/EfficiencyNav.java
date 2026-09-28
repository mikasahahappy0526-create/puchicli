package jp.puchicli.app.ui.home;

/** Holds the Compose screen state so 詳細設定 opens the existing settings screen. */
public final class EfficiencyNav {
    private static Object screen;

    private EfficiencyNav() {
    }

    public static void rememberScreen(Object state) {
        screen = state;
    }

    public static void show(String name) {
        Object state = screen;
        if (state == null || name == null) {
            return;
        }
        try {
            state.getClass().getMethod("setValue", Object.class).invoke(state, name);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
