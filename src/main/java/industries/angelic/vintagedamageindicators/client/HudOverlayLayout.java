package industries.angelic.vintagedamageindicators.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

public final class HudOverlayLayout {

    public static final int BOSS_BAR_WIDTH = 182;
    public static final int BOSS_BAR_HEIGHT = 5;
    public static final int BOSS_BAR_BAR_Y = 12;
    public static final int BOSS_BAR_TEXT_Y = 2;
    public static final int BOSS_BAR_BOTTOM = BOSS_BAR_BAR_Y + BOSS_BAR_HEIGHT;
    public static final int BOSS_BAR_TEXT_TO_BAR_OFFSET = BOSS_BAR_BAR_Y - BOSS_BAR_TEXT_Y;
    public static final int BOSS_BAR_MARGIN = 4;
    public static final int PLAYER_LIST_TOP = 10;
    public static final int PLAYER_LIST_ROW_HEIGHT = 9;
    public static final int PLAYER_LIST_MARGIN = 4;
    public static final int PLAYER_LIST_MAX_WIDTH = 300;
    public static final int PLAYER_LIST_MAX_COLUMN_WIDTH = 150;

    private HudOverlayLayout() {}

    public static boolean isHudVisible(Minecraft minecraft) {
        return minecraft != null && minecraft.thePlayer != null
            && Config.hudIndicatorEnabled
            && !minecraft.gameSettings.hideGUI
            && (!minecraft.gameSettings.showDebugInfo || !Config.hideOnDebug)
            && (minecraft.currentScreen == null || minecraft.currentScreen instanceof GuiChat)
            && VintageDamageIndicators.varInstanceClient != null
            && VintageDamageIndicators.varInstanceClient.hudIndicatorState.getTargetEntity() != null;
    }

    public static OverlayRect getConfiguredHudRect(int screenWidth, int screenHeight) {
        int width = Math.max(1, Math.round(HudPreviewMath.PANEL_WIDTH * Config.hudIndicatorSize));
        int height = Math.max(1, Math.round(HudPreviewMath.PANEL_HEIGHT * Config.hudIndicatorSize));
        int x = Config.hudIndicatorAlignLeft ? Config.hudIndicatorPositionX
            : screenWidth - width - Config.hudIndicatorPositionX;
        int y = Config.hudIndicatorAlignTop ? Config.hudIndicatorPositionY
            : screenHeight - height - Config.hudIndicatorPositionY;
        x = Math.max(0, Math.min(x, Math.max(0, screenWidth - width)));
        y = Math.max(0, Math.min(y, Math.max(0, screenHeight - height)));
        return new OverlayRect(x, y, width, height);
    }

    public static BossBarLayout resolveBossBarLayout(int screenWidth, int screenHeight, OverlayRect hudRect,
        boolean visible, int bossNameWidth) {
        int defaultLeft = screenWidth / 2 - BOSS_BAR_WIDTH / 2;
        int defaultTop = BOSS_BAR_TEXT_Y;
        if (!visible) {
            return new BossBarLayout(
                defaultLeft,
                defaultTop,
                false,
                false,
                buildBossBarRect(defaultLeft, defaultTop, bossNameWidth));
        }

        OverlayRect defaultRect = buildBossBarRect(defaultLeft, defaultTop, bossNameWidth);
        if (hudRect == null || !defaultRect.intersects(hudRect)) {
            return new BossBarLayout(defaultLeft, defaultTop, true, false, defaultRect);
        }

        int rightCandidate = hudRect.right + BOSS_BAR_MARGIN;
        if (fitsHorizontal(rightCandidate, BOSS_BAR_WIDTH, screenWidth)) {
            OverlayRect rightRect = buildBossBarRect(rightCandidate, defaultTop, bossNameWidth);
            if (!rightRect.intersects(hudRect)) {
                return new BossBarLayout(rightCandidate, defaultTop, true, true, rightRect);
            }
        }

        int leftCandidate = hudRect.left - BOSS_BAR_MARGIN - BOSS_BAR_WIDTH;
        if (fitsHorizontal(leftCandidate, BOSS_BAR_WIDTH, screenWidth)) {
            OverlayRect leftRect = buildBossBarRect(leftCandidate, defaultTop, bossNameWidth);
            if (!leftRect.intersects(hudRect)) {
                return new BossBarLayout(leftCandidate, defaultTop, true, true, leftRect);
            }
        }

        int downTop = Math.max(defaultTop, hudRect.bottom + BOSS_BAR_MARGIN);
        OverlayRect downRect = buildBossBarRect(defaultLeft, downTop, bossNameWidth);
        if (downRect.bottom <= screenHeight && !downRect.intersects(hudRect)) {
            return new BossBarLayout(defaultLeft, downTop, true, true, downRect);
        }

        return new BossBarLayout(defaultLeft, defaultTop, true, false, defaultRect);
    }

    public static PlayerListLayout resolvePlayerListLayout(int screenWidth, int screenHeight, OverlayRect hudRect,
        BossBarLayout bossBar, FontRenderer fontRenderer, List<String> displayNames, int playerCount, int maxPlayers) {
        int safeMaxPlayers = Math.max(1, TabFacesCompatHelper.shouldTrimTabMenu() ? playerCount : maxPlayers);
        int rows = safeMaxPlayers;
        int columns = 1;
        while (rows > 20) {
            columns++;
            rows = (safeMaxPlayers + columns - 1) / columns;
        }

        int columnWidth = PLAYER_LIST_MAX_WIDTH / columns;
        if (columnWidth > PLAYER_LIST_MAX_COLUMN_WIDTH) {
            columnWidth = PLAYER_LIST_MAX_COLUMN_WIDTH;
        }
        columnWidth = TabFacesCompatHelper.adjustColumnWidth(columnWidth, fontRenderer, displayNames);

        int width = columns * columnWidth + 1;
        int defaultLeft = (screenWidth - columns * columnWidth) / 2;
        int defaultTop = PLAYER_LIST_TOP;
        int top = hudRect == null ? defaultTop : Math.max(defaultTop, hudRect.top);
        if (bossBar != null && bossBar.visible) {
            top = Math.max(top, bossBar.bounds.bottom + PLAYER_LIST_MARGIN);
        }

        int left = defaultLeft;
        boolean moved = false;

        OverlayRect centeredRect = buildPlayerListRect(left, top, rows, columns, columnWidth);
        if (hudRect != null && centeredRect.intersects(hudRect)) {
            boolean placedBesideHud = false;
            int rightCandidate = hudRect.right + PLAYER_LIST_MARGIN;
            if (fitsHorizontal(rightCandidate, width, screenWidth)) {
                left = rightCandidate;
                placedBesideHud = true;
            } else {
                int leftCandidate = hudRect.left - PLAYER_LIST_MARGIN - width;
                if (fitsHorizontal(leftCandidate, width, screenWidth)) {
                    left = leftCandidate;
                    placedBesideHud = true;
                }
            }

            if (!placedBesideHud) {
                top = Math.max(top, hudRect.bottom + PLAYER_LIST_MARGIN);
            }
        }

        OverlayRect finalRect = buildPlayerListRect(left, top, rows, columns, columnWidth);
        if (finalRect.bottom > screenHeight) {
            top = Math.max(0, screenHeight - (rows * PLAYER_LIST_ROW_HEIGHT + 1));
        }

        moved = left != defaultLeft || top != defaultTop;
        return new PlayerListLayout(left, top, rows, columns, columnWidth, playerCount, moved);
    }

    public static PlayerListShift resolveCustomPlayerListShift(int screenWidth, int screenHeight, OverlayRect hudRect,
        BossBarLayout bossBar, int playerListWidth) {
        int safeWidth = Math.max(1, Math.min(playerListWidth, screenWidth));
        int defaultLeft = screenWidth / 2 - safeWidth / 2;
        int defaultTop = PLAYER_LIST_TOP;
        int left = defaultLeft;
        int top = defaultTop;

        if (bossBar != null && bossBar.visible) {
            top = Math.max(top, bossBar.bounds.bottom + PLAYER_LIST_MARGIN);
        }

        OverlayRect playerListRect = buildCustomPlayerListRect(left, top, safeWidth, screenHeight);
        if (hudRect != null && playerListRect.intersects(hudRect)) {
            int rightCandidate = hudRect.right + PLAYER_LIST_MARGIN + 1;
            if (fitsCustomPlayerListHorizontally(rightCandidate, safeWidth, screenWidth)) {
                left = rightCandidate;
            } else {
                int leftCandidate = hudRect.left - PLAYER_LIST_MARGIN - safeWidth - 1;
                if (fitsCustomPlayerListHorizontally(leftCandidate, safeWidth, screenWidth)) {
                    left = leftCandidate;
                } else {
                    top = Math.max(top, hudRect.bottom + PLAYER_LIST_MARGIN + 1);
                }
            }
        }

        return new PlayerListShift(left - defaultLeft, top - defaultTop);
    }

    private static OverlayRect buildBossBarRect(int left, int top, int bossNameWidth) {
        int nameLeft = left + BOSS_BAR_WIDTH / 2 - bossNameWidth / 2;
        int right = Math.max(left + BOSS_BAR_WIDTH, nameLeft + bossNameWidth);
        int realLeft = Math.min(left, nameLeft);
        return new OverlayRect(realLeft, top, right - realLeft, BOSS_BAR_TEXT_TO_BAR_OFFSET + BOSS_BAR_HEIGHT);
    }

    private static OverlayRect buildPlayerListRect(int left, int top, int rows, int columns, int columnWidth) {
        return new OverlayRect(left - 1, top - 1, columnWidth * columns + 1, rows * PLAYER_LIST_ROW_HEIGHT + 1);
    }

    private static OverlayRect buildCustomPlayerListRect(int left, int top, int width, int screenHeight) {
        return new OverlayRect(left - 1, top - 1, width + 2, Math.max(1, screenHeight - top + 1));
    }

    private static boolean fitsCustomPlayerListHorizontally(int left, int width, int screenWidth) {
        return left - 1 >= 0 && left + width + 1 <= screenWidth;
    }

    private static boolean fitsHorizontal(int left, int width, int screenWidth) {
        return left >= 0 && left + width <= screenWidth;
    }

    public static final class OverlayRect {

        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        public OverlayRect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.right = left + width;
            this.bottom = top + height;
        }

        public boolean intersects(OverlayRect other) {
            return other != null && this.left < other.right
                && this.right > other.left
                && this.top < other.bottom
                && this.bottom > other.top;
        }
    }

    public static final class BossBarLayout {

        public final int left;
        public final int top;
        public final boolean visible;
        public final boolean moved;
        public final OverlayRect bounds;

        private BossBarLayout(int left, int top, boolean visible, boolean moved, OverlayRect bounds) {
            this.left = left;
            this.top = top;
            this.visible = visible;
            this.moved = moved;
            this.bounds = bounds;
        }

        public int getNameX(int bossNameWidth) {
            return this.left + BOSS_BAR_WIDTH / 2 - bossNameWidth / 2;
        }

        public int getNameY() {
            return this.top;
        }

        public int getBarY() {
            return this.top + BOSS_BAR_TEXT_TO_BAR_OFFSET;
        }
    }

    public static final class PlayerListLayout {

        public final int left;
        public final int top;
        public final int rows;
        public final int columns;
        public final int columnWidth;
        public final int playerCount;
        public final boolean moved;

        private PlayerListLayout(int left, int top, int rows, int columns, int columnWidth, int playerCount,
            boolean moved) {
            this.left = left;
            this.top = top;
            this.rows = rows;
            this.columns = columns;
            this.columnWidth = columnWidth;
            this.playerCount = playerCount;
            this.moved = moved;
        }
    }

    public static final class PlayerListShift {

        public final int x;
        public final int y;

        private PlayerListShift(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
