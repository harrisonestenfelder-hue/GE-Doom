
package com.doom.godemperor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DoomSkillTreeScreen extends Screen {

    private DoomSkillTree.Stage stage;
    private DoomSkillTree.Branch branch;
    private DoomSkillTree.Skill selectedSkill;

    private static final int ROW_HEIGHT = 76;

    private static final int GREEN = 0xFF36D66A;
    private static final int GREEN_DARK = 0xFF123C25;
    private static final int WHITE = 0xFFE8F0F5;
    private static final int SILVER = 0xFF9EADB8;
    private static final int GOLD = 0xFFD8B45A;
    private static final int PANEL = 0xF00B0E12;
    private static final int PANEL_LIGHT = 0xFF171D24;

    private int scrollOffset = 0;

    private Button unlockButton;
    private final List<Button> loadoutButtons = new ArrayList<>();

    public DoomSkillTreeScreen() {
        this(DoomSkillTree.Stage.DOCTOR_DOOM, null);
    }

    public DoomSkillTreeScreen(
            DoomSkillTree.Stage stage,
            DoomSkillTree.Branch branch
    ) {
        super(Component.literal("Doom: Path to Absolute Power"));
        this.stage = stage;
        this.branch = branch;

        List<DoomSkillTree.Skill> skills = getVisibleSkills();
        if (!skills.isEmpty()) {
            selectedSkill = skills.get(0);
        }
    }

    private boolean isDoctorDoom() {
        return stage == DoomSkillTree.Stage.DOCTOR_DOOM;
    }

    private int accent() {
        return isDoctorDoom() ? GREEN : WHITE;
    }

    private int accentDark() {
        return isDoctorDoom() ? GREEN_DARK : 0xFF343D48;
    }

    private List<DoomSkillTree.Skill> getVisibleSkills() {
        return DoomSkillTree.SKILLS.stream()
                .filter(skill -> skill.stage() == stage)
                .filter(skill -> branch == null || skill.branch() == branch)
                .toList();
    }

    @Override
    protected void init() {
        clearWidgets();
        loadoutButtons.clear();

        int center = width / 2;

        // STAGE TABS

        addRenderableWidget(Button.builder(
                Component.literal("DOCTOR DOOM"),
                button -> openStage(DoomSkillTree.Stage.DOCTOR_DOOM)
        ).bounds(center - 190, 29, 180, 25).build());

        addRenderableWidget(Button.builder(
                Component.literal("GOD EMPEROR"),
                button -> openStage(DoomSkillTree.Stage.GOD_EMPEROR)
        ).bounds(center + 10, 29, 180, 25).build());

        // BRANCH FILTERS

        DoomSkillTree.Branch[] branches = {
                null,
                DoomSkillTree.Branch.TECHNOLOGY,
                DoomSkillTree.Branch.SORCERY,
                DoomSkillTree.Branch.COMBAT,
                DoomSkillTree.Branch.DOMINION
        };

        if (!isDoctorDoom()) {
            branches = new DoomSkillTree.Branch[] {
                    null,
                    DoomSkillTree.Branch.REALITY,
                    DoomSkillTree.Branch.COSMIC,
                    DoomSkillTree.Branch.CREATION,
                    DoomSkillTree.Branch.ABSOLUTE_POWER
            };
        }

        String[] names = {
                "ALL",
                "TECH",
                "SORCERY",
                "COMBAT",
                "DOMINION"
        };

        if (!isDoctorDoom()) {
            names = new String[] {
                    "ALL",
                    "REALITY",
                    "COSMIC",
                    "CREATION",
                    "ABSOLUTE"
            };
        }

        int tabWidth = Math.min(94, (width - 48) / 5);

        for (int i = 0; i < branches.length; i++) {
            final DoomSkillTree.Branch selected = branches[i];

            addRenderableWidget(Button.builder(
                    Component.literal(names[i]),
                    button -> openBranch(selected)
            ).bounds(
                    24 + i * (tabWidth + 3),
                    65,
                    tabWidth,
                    22
            ).build());
        }

        // UNLOCK BUTTON

        int detailX = width - 278;

        unlockButton = addRenderableWidget(Button.builder(
                Component.literal("UNLOCK SKILL"),
                button -> {
                    if (selectedSkill != null) {
                        DoomNetwork.requestUnlock(selectedSkill.id());
                    }
                }
        ).bounds(
                detailX + 15,
                height - 210,
                248,
                25
        ).build());

        // NINE ABILITY LOADOUT SLOTS

        String[] keys = {
                "Z", "X", "C",
                "V", "F", "R",
                "Q", "M4", "M5"
        };

        for (int i = 0; i < 9; i++) {
            final int slot = i;

            Button button = addRenderableWidget(Button.builder(
                    Component.literal((i + 1) + "  " + keys[i]),
                    pressed -> {
                        if (selectedSkill != null) {
                            DoomNetwork.requestAssign(
                                    slot,
                                    selectedSkill.id()
                            );
                        }
                    }
            ).bounds(
                    detailX + 15 + (i % 3) * 84,
                    height - 166 + (i / 3) * 29,
                    78,
                    24
            ).build());

            loadoutButtons.add(button);
        }

        // CLOSE

        addRenderableWidget(Button.builder(
                Component.literal("CLOSE"),
                button -> onClose()
        ).bounds(
                center - 45,
                height - 29,
                90,
                20
        ).build());

        updateScrollLimit();
    }

    private void openStage(DoomSkillTree.Stage selected) {
        DoomSkillTree.Branch firstBranch = null;

        Minecraft.getInstance().setScreen(
                new DoomSkillTreeScreen(selected, firstBranch)
        );
    }

    private void openBranch(DoomSkillTree.Branch selected) {
        Minecraft.getInstance().setScreen(
                new DoomSkillTreeScreen(stage, selected)
        );
    }

    private void updateScrollLimit() {
        int contentHeight = getVisibleSkills().size() * ROW_HEIGHT;
        int viewportHeight = Math.max(100, height - 165);

        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void selectSkill(DoomSkillTree.Skill skill) {
        selectedSkill = skill;
    }

    private String formatName(String name) {
        String lower = name.toLowerCase().replace('_', ' ');
        String[] words = lower.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        return result.toString().trim();
    }

    private void drawLine(
            GuiGraphics graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int color
    ) {
        if (x1 == x2) {
            graphics.fill(
                    x1 - 1,
                    Math.min(y1, y2),
                    x1 + 2,
                    Math.max(y1, y2),
                    color
            );
        } else if (y1 == y2) {
            graphics.fill(
                    Math.min(x1, x2),
                    y1 - 1,
                    Math.max(x1, x2),
                    y1 + 2,
                    color
            );
        }
    }

    private void drawNode(
            GuiGraphics graphics,
            int x,
            int y,
            boolean selected,
            boolean hovered
    ) {
        int color = accent();

        int outer = selected ? 0xFFFFD56A : color;
        int inner = isDoctorDoom()
                ? 0xFF07150D
                : 0xFF111820;

        if (hovered) {
            graphics.fill(
                    x - 13, y - 13,
                    x + 14, y + 14,
                    0x447FFFFFFF
            );
        }

        graphics.fill(
                x - 10, y - 10,
                x + 11, y + 11,
                outer
        );

        graphics.fill(
                x - 6, y - 6,
                x + 7, y + 7,
                inner
        );

        graphics.fill(
                x - 2, y - 2,
                x + 3, y + 3,
                selected ? GOLD : color
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        boolean doom = isDoctorDoom();

        int accent = accent();
        int accentDark = accentDark();

        int left = 18;
        int top = 96;
        int detailX = width - 278;
        int detailWidth = 260;
        int treeRight = detailX - 12;
        int treeWidth = treeRight - left;

        int treeBottom = height - 48;
        int viewportHeight = treeBottom - top;

        // BACKGROUND - NO BLUR, TO AVOID THE PREVIOUS CRASH

        graphics.fill(
                0, 0, width, height,
                0xE8000000
        );

        // SUBTLE BACKGROUND PANELS

        graphics.fill(
                0, 0, width, height,
                doom ? 0x180C5B2A : 0x183C5369
        );

        // TITLE

        graphics.drawCenteredString(
                font,
                "DOOM: PATH TO ABSOLUTE POWER",
                width / 2,
                10,
                GOLD
        );

        graphics.drawCenteredString(
                font,
                doom
                        ? "THE LATVERIAN ASCENSION"
                        : "THE POWER BEYOND REALITY",
                width / 2,
                19,
                accent
        );

        // STAGE AND BRANCH LABELS

        graphics.drawString(
                font,
                doom ? "DOCTOR DOOM" : "GOD EMPEROR DOOM",
                left + 5,
                91,
                accent
        );

        // TREE PANEL

        graphics.fill(
                left,
                top,
                treeRight,
                treeBottom,
                PANEL
        );

        graphics.renderOutline(
                left,
                top,
                treeWidth,
                viewportHeight,
                accentDark
        );

        // TREE HEADER

        graphics.fill(
                left + 1,
                top + 1,
                treeRight - 1,
                top + 24,
                0xFF171D24
        );

        graphics.drawString(
                font,
                branch == null
                        ? "COMPLETE SKILL TREE"
                        : formatName(branch.name()).toUpperCase(),
                left + 12,
                top + 8,
                accent
        );

        // TREE CONTENT

        List<DoomSkillTree.Skill> skills = getVisibleSkills();

        int nodeX = left + 27;
        int firstY = top + 52;

        graphics.enableScissor(
                left + 2,
                top + 26,
                treeRight - 2,
                treeBottom - 2
        );

        // CONNECTING PATH

        if (skills.size() > 1) {
            int firstNodeY = firstY - scrollOffset + 10;
            int lastNodeY = firstY
                    + (skills.size() - 1) * ROW_HEIGHT
                    - scrollOffset + 10;

            drawLine(
                    graphics,
                    nodeX,
                    firstNodeY,
                    nodeX,
                    lastNodeY,
                    accentDark
            );
        }

        for (int i = 0; i < skills.size(); i++) {
            DoomSkillTree.Skill skill = skills.get(i);

            int rowY = firstY + i * ROW_HEIGHT - scrollOffset;
            int nodeY = rowY + 22;

            if (rowY + 55 < top + 25 ||
                    rowY > treeBottom) {
                continue;
            }

            boolean selected = selectedSkill != null &&
                    selectedSkill.id().equals(skill.id());

            boolean hovered =
                    mouseX >= left + 5 &&
                    mouseX <= treeRight - 5 &&
                    mouseY >= rowY - 4 &&
                    mouseY <= rowY + 57;

            // NODE CARD

            graphics.fill(
                    left + 10,
                    rowY - 4,
                    treeRight - 10,
                    rowY + 56,
                    selected
                            ? (doom ? 0xFF173B26 : 0xFF293744)
                            : PANEL_LIGHT
            );

            graphics.renderOutline(
                    left + 10,
                    rowY - 4,
                    treeWidth - 20,
                    60,
                    selected ? GOLD : 0xFF3D4852
            );

            // NODE

            drawNode(
                    graphics,
                    nodeX,
                    nodeY,
                    selected,
                    hovered
            );

            // SKILL NAME

            graphics.drawString(
                    font,
                    skill.name(),
                    nodeX + 20,
                    rowY + 4,
                    selected ? GOLD : 0xFFFFFFFF
            );

            // BRANCH AND TIER

            graphics.drawString(
                    font,
                    formatName(skill.branch().name())
                            + "  |  TIER " + skill.tier(),
                    nodeX + 20,
                    rowY + 19,
                    accent
            );

            // COST

            graphics.drawString(
                    font,
                    skill.pointCost() + " SKILL POINTS",
                    nodeX + 20,
                    rowY + 35,
                    GOLD
            );

            graphics.drawString(
                    font,
                    selected ? "SELECTED" : "SELECT NODE",
                    treeRight - 92,
                    rowY + 19,
                    selected ? GOLD : SILVER
            );
        }

        graphics.disableScissor();

        // SCROLL INDICATOR

        int contentHeight = skills.size() * ROW_HEIGHT;

        if (contentHeight > viewportHeight - 26) {
            graphics.drawString(
                    font,
                    "SCROLL TO EXPLORE",
                    treeRight - 116,
                    treeBottom - 13,
                    SILVER
            );
        }

        // DETAIL PANEL

        graphics.fill(
                detailX,
                top,
                detailX + detailWidth,
                treeBottom,
                PANEL
        );

        graphics.renderOutline(
                detailX,
                top,
                detailWidth,
                viewportHeight,
                accentDark
        );

        graphics.fill(
                detailX + 1,
                top + 1,
                detailX + detailWidth - 1,
                top + 25,
                0xFF171D24
        );

        graphics.drawString(
                font,
                "POWER DETAILS",
                detailX + 12,
                top + 9,
                accent
        );

        if (selectedSkill != null) {
            int y = top + 42;

            graphics.drawString(
                    font,
                    selectedSkill.name(),
                    detailX + 14,
                    y,
                    GOLD
            );

            y += 22;

            graphics.drawString(
                    font,
                    formatName(selectedSkill.branch().name()),
                    detailX + 14,
                    y,
                    accent
            );

            y += 20;

            graphics.drawString(
                    font,
                    "TIER " + selectedSkill.tier(),
                    detailX + 14,
                    y,
                    SILVER
            );

            y += 22;

            graphics.drawString(
                    font,
                    "DESCRIPTION",
                    detailX + 14,
                    y,
                    accent
            );

            y += 14;

            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(
                            Component.literal(selectedSkill.description()),
                            detailWidth - 28
                    );

            for (var line : lines) {
                graphics.drawString(
                        font,
                        line,
                        detailX + 14,
                        y,
                        0xFFD0D6DC
                );
                y += 12;
            }

            y += 10;

            graphics.drawString(
                    font,
                    "COST: " + selectedSkill.pointCost() + " SP",
                    detailX + 14,
                    y,
                    GOLD
            );

            y += 22;

            graphics.drawString(
                    font,
                    "PREREQUISITES",
                    detailX + 14,
                    y,
                    accent
            );

            y += 14;

            if (selectedSkill.prerequisites().isEmpty()) {
                graphics.drawString(
                        font,
                        "No prerequisites",
                        detailX + 14,
                        y,
                        SILVER
                );
            } else {
                for (String prerequisite :
                        selectedSkill.prerequisites()) {
                    graphics.drawString(
                            font,
                            "- " + formatName(prerequisite),
                            detailX + 14,
                            y,
                            SILVER
                    );
                    y += 13;
                }
            }
        } else {
            graphics.drawString(
                    font,
                    "Select a skill node.",
                    detailX + 14,
                    top + 48,
                    SILVER
            );
        }

        // LOADOUT HEADER

        graphics.drawString(
                font,
                "ABILITY LOADOUT",
                detailX + 14,
                height - 190,
                accent
        );

        graphics.drawString(
                font,
                "Select a skill, then choose a slot.",
                detailX + 14,
                height - 178,
                SILVER
        );

        // FOOTER

        graphics.drawCenteredString(
                font,
                doom
                        ? "GREEN POWER  |  LATVERIAN TECHNOLOGY  |  SORCERY"
                        : "WHITE POWER  |  REALITY  |  COSMIC DOMINION",
                width / 2,
                height - 13,
                accent
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // CLICK SKILL NODES

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.buttonInfo().button() != 0) {
            return false;
        }

        double mouseX = event.x();
        double mouseY = event.y();

        int left = 18;
        int detailX = width - 278;
        int treeRight = detailX - 12;
        int top = 96;
        int treeBottom = height - 48;

        if (mouseX < left + 5 ||
                mouseX > treeRight - 5 ||
                mouseY < top + 26 ||
                mouseY > treeBottom) {
            return false;
        }

        List<DoomSkillTree.Skill> skills = getVisibleSkills();

        int firstY = top + 52;

        for (int i = 0; i < skills.size(); i++) {
            int rowY = firstY + i * ROW_HEIGHT - scrollOffset;

            if (mouseY >= rowY - 4 &&
                    mouseY <= rowY + 56) {
                selectSkill(skills.get(i));
                return true;
            }
        }

        return false;
    }

    // SCROLL THE TREE

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        int left = 18;
        int detailX = width - 278;
        int treeRight = detailX - 12;
        int top = 96;
        int treeBottom = height - 48;

        if (mouseX >= left &&
                mouseX <= treeRight &&
                mouseY >= top &&
                mouseY <= treeBottom) {

            scrollOffset -= (int) (verticalAmount * 45);
            updateScrollLimit();

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
