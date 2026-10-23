package com.platform.backend.modules.menu.domain.tree;

import com.platform.backend.modules.menu.domain.entities.MenuEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.platform.backend.modules.menu.MenuTestFixtures.group;
import static com.platform.backend.modules.menu.MenuTestFixtures.screen;
import static org.assertj.core.api.Assertions.assertThat;

class MenuTreeTest {

    private MenuEntity dashboard;
    private MenuEntity competition;
    private MenuEntity events;
    private MenuEntity formats;
    private MenuEntity platform;
    private MenuEntity advanced;
    private MenuEntity menusScreen;
    private MenuTree tree;

    /**
     * Dashboard
     * Competition ── Events, Formats
     * Platform ── Advanced ── Menus
     */
    @BeforeEach
    void setUp() {
        dashboard = screen("Dashboard", "/dashboard", null, 0);
        competition = group("Competition", null, 1);
        events = screen("Events", "/events", competition, 0);
        formats = screen("Formats", "/formats", competition, 1);
        platform = group("Platform", null, 2);
        advanced = group("Advanced", platform, 0);
        menusScreen = screen("Menus", "/menus", advanced, 0);
        menusScreen.setSystem(true);
        tree = new MenuTree(List.of(dashboard, competition, events, formats, platform, advanced, menusScreen));
    }

    // ─── Shape ──────────────────────────────────────────────────────────────────

    @Test
    void childrenOf_keepsTheOrderTheItemsCameIn() {
        assertThat(tree.childrenOf(competition.getId())).containsExactly(events, formats);
        assertThat(tree.childrenOf(null)).containsExactly(dashboard, competition, platform);
    }

    @Test
    void depthOf_countsLevelsFromTheTop() {
        assertThat(tree.depthOf(dashboard.getId())).isEqualTo(1);
        assertThat(tree.depthOf(events.getId())).isEqualTo(2);
        assertThat(tree.depthOf(menusScreen.getId())).isEqualTo(3);
    }

    @Test
    void heightOf_countsLevelsDownToTheDeepestDescendant() {
        assertThat(tree.heightOf(menusScreen.getId())).isEqualTo(1);
        assertThat(tree.heightOf(competition.getId())).isEqualTo(2);
        assertThat(tree.heightOf(platform.getId())).isEqualTo(3);
    }

    @Test
    void descendantsOf_listsEveryLevelButNotTheItself() {
        assertThat(tree.descendantsOf(platform.getId())).containsExactly(advanced, menusScreen);
        assertThat(tree.descendantsOf(events.getId())).isEmpty();
    }

    @Test
    void isDescendantOf_looksAtEveryLevel() {
        assertThat(tree.isDescendantOf(menusScreen.getId(), platform.getId())).isTrue();
        assertThat(tree.isDescendantOf(platform.getId(), menusScreen.getId())).isFalse();
        assertThat(tree.isDescendantOf(platform.getId(), platform.getId())).isFalse();
    }

    @Test
    void anItemWhoseParentIsMissingCountsAsTopLevel() {
        MenuEntity orphan = screen("Orphan", "/orphan", null, 3);
        orphan.setIdParentMenu(UUID.randomUUID());

        MenuTree withOrphan = new MenuTree(List.of(dashboard, orphan));

        assertThat(withOrphan.childrenOf(null)).containsExactly(dashboard, orphan);
        assertThat(withOrphan.parentIdOf(orphan.getId())).isNull();
        assertThat(withOrphan.depthOf(orphan.getId())).isEqualTo(1);
    }

    @Test
    void nextOrderUnder_goesAfterTheHighestOrderOfTheChildren() {
        assertThat(tree.nextOrderUnder(competition.getId())).isEqualTo(2);
        assertThat(tree.nextOrderUnder(null)).isEqualTo(3);
        assertThat(tree.nextOrderUnder(events.getId())).isZero();
    }

    @Test
    void containsSystemItem_isTrueForTheItemAndForEveryGroupAboveIt() {
        assertThat(tree.containsSystemItem(menusScreen.getId())).isTrue();
        assertThat(tree.containsSystemItem(platform.getId())).isTrue();
        assertThat(tree.containsSystemItem(competition.getId())).isFalse();
    }

    // ─── Names and urls ─────────────────────────────────────────────────────────

    @Test
    void isNameTaken_ignoresCaseAndSpacesButNotTheItemItself() {
        assertThat(tree.isNameTaken("  events ", null)).isTrue();
        assertThat(tree.isNameTaken("Events", events.getId())).isFalse();
        assertThat(tree.isNameTaken("Reports", null)).isFalse();
    }

    @Test
    void isUrlTaken_ignoresCaseButNotTheItemItself() {
        assertThat(tree.isUrlTaken("/EVENTS", null)).isTrue();
        assertThat(tree.isUrlTaken("/events", events.getId())).isFalse();
        assertThat(tree.isUrlTaken("/reports", null)).isFalse();
    }

    // ─── Hidden items ───────────────────────────────────────────────────────────

    @Test
    void isEffectivelyActive_isFalseForAnItemUnderAHiddenGroup() {
        competition.setIsActive(false);

        assertThat(tree.isEffectivelyActive(competition)).isFalse();
        assertThat(tree.isEffectivelyActive(events)).isFalse();
        assertThat(tree.isEffectivelyActive(dashboard)).isTrue();
    }

    @Test
    void isEffectivelyActive_treatsAMissingFlagAsActive() {
        events.setIsActive(null);

        assertThat(tree.isEffectivelyActive(events)).isTrue();
    }

    // ─── What a user sees ───────────────────────────────────────────────────────

    @Test
    void visibleFor_showsTheGroupsAroundAGrantedScreenWithoutGrantingThemDirectly() {
        List<MenuEntity> visible = tree.visibleFor(Set.of(events.getId()));

        assertThat(visible).containsExactly(competition, events);
    }

    @Test
    void visibleFor_keepsTheDisplayOrder() {
        List<MenuEntity> visible = tree.visibleFor(Set.of(menusScreen.getId(), formats.getId(), dashboard.getId(), events.getId()));

        assertThat(visible).containsExactly(dashboard, competition, events, formats, platform, advanced, menusScreen);
    }

    @Test
    void visibleFor_dropsAGroupLeftWithNothingInsideEvenIfItWasGrantedItself() {
        List<MenuEntity> visible = tree.visibleFor(Set.of(competition.getId(), dashboard.getId()));

        assertThat(visible).containsExactly(dashboard);
    }

    @Test
    void visibleFor_dropsHiddenItemsAndEverythingUnderThem() {
        competition.setIsActive(false);

        List<MenuEntity> visible = tree.visibleFor(Set.of(events.getId(), dashboard.getId()));

        assertThat(visible).containsExactly(dashboard);
    }

    @Test
    void visibleFor_dropsAGroupWhoseOnlyScreensAreHidden() {
        events.setIsActive(false);

        List<MenuEntity> visible = tree.visibleFor(Set.of(events.getId()));

        assertThat(visible).isEmpty();
    }

    @Test
    void visibleFor_showsAnythingThatOpensAScreenEvenWithoutAGroup() {
        assertThat(tree.visibleFor(Set.of(dashboard.getId()))).containsExactly(dashboard);
    }

    @Test
    void visibleFor_isEmptyWhenNothingWasGranted() {
        assertThat(tree.visibleFor(Set.of())).isEmpty();
    }
}
