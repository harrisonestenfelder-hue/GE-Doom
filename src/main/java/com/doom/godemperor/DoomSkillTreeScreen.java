
package com.doom.godemperor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class DoomSkillTreeScreen extends Screen {

    private final DoomSkillTree.Stage stage;
    private final DoomSkillTree.Branch branch;

    private static final int PANEL_WIDTH = 500;

    public DoomSkillTreeScreen() {
        this(
                DoomSkillTree.Stage.DOCTOR_DOOM,
                DoomSkillTree.Branch.TECHNOLOGY
        );
    }

    public DoomSkillTreeScreen(
            DoomSkillTree.Stage stage,
            DoomSkillTree.Branch branch
    ) {
        super(Component.literal("Doom Skill Tree"));
        this.stage = stage;
        this.branch = branch;
    }

    @Override
    protected void init() {

        int center = this.width / 2;
        int left = center - PANEL_WIDTH / 2;

        // Stage selection

        addRenderableWidget(Button.builder(
                Component.literal("DOCTOR DOOM"),
                button -> openStage(
                        DoomSkillTree.Stage.DOCTOR_DOOM
                )
        ).bounds(left + 30, 38, 210, 24).build());

        addRenderableWidget(Button.builder(
                Component.literal("GOD EMPEROR"),
                button -> openStage(
                        DoomSkillTree.Stage.GOD_EMPEROR
                )
        ).bounds(left + 260, 38, 210, 24).build());

        // Branch selection

        DoomSkillTree.Branch[] branches =
                stage == DoomSkillTree.Stage.DOCTOR_DOOM
                        ? new DoomSkillTree.Branch[] {
                                DoomSkillTree.Branch.TECHNOLOGY,
                                DoomSkillTree.Branch.SORCERY,
                                DoomSkillTree.Branch.COMBAT,
                                DoomSkillTree.Branch.DOMINION
                        }
                        : new DoomSkillTree.Branch[] {
                                DoomSkillTree.Branch.REALITY,
                                DoomSkillTree.Branch.COSMIC,
                                DoomSkillTree.Branch.CREATION,
                                DoomSkillTree.Branch.ABSOLUTE_POWER
                        };

        for (int i = 0; i < branches.length; i++) {

            DoomSkillTree.Branch selected = branches[i];

            addRenderableWidget(Button.builder(
                    Component.literal(
                            formatName(selected.name())
                    ),
                    button -> openBranch(selected)
            ).bounds(
                    left + 20 + i * 117,
                    75,
                    110,
                    25
            ).build());
        }

        // Skills in selected branch

        List<DoomSkillTree.Skill> skills =
                DoomSkillTree.getBranch(stage, branch);

        int y = 125;

        for (DoomSkillTree.Skill skill : skills) {

            final String skillId = skill.id();

            addRenderableWidget(Button.builder(
                    Component.literal(
                            "Unlock - " + skill.pointCost() + " SP"
                    ),
                    button -> DoomNetwork.requestUnlock(skillId)
            ).bounds(
                    center + 75,
                    y + 12,
                    145,
                    25
            ).build());

            y += 65;
        }

        // Close button

        addRenderableWidget(Button.builder(
                Component.literal("CLOSE"),
                button -> onClose()
        ).bounds(
                center - 50,
                this.height - 38,
                100,
                25
        ).build());
    }

    private void openStage(DoomSkillTree.Stage selected) {

        DoomSkillTree.Branch firstBranch =
                selected == DoomSkillTree.Stage.DOCTOR_DOOM
                        ? DoomSkillTree.Branch.TECHNOLOGY
                        : DoomSkillTree.Branch.REALITY;

        Minecraft.getInstance().setScreen(
                new DoomSkillTreeScreen(
                        selected,
                        firstBranch
                )
        );
    }

    private void openBranch(DoomSkillTree.Branch selected) {

        Minecraft.getInstance().setScreen(
                new DoomSkillTreeScreen(stage, selected)
        );
    }

    private String formatName(String name) {
        return name.replace('_', ' ');
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        renderBackground(graphics, mouseX, mouseY, partialTick);

        int center = this.width / 2;
        int left = center - PANEL_WIDTH / 2;

        // Main panel

        graphics.fill(
                left,
                20,
                left + PANEL_WIDTH,
                this.height - 20,
                0xEE101419
        );

        graphics.renderOutline(
                left,
                20,
                PANEL_WIDTH,
                this.height - 40,
                0xFF82909A
        );

        // Title

        graphics.drawCenteredString(
                this.font,
                "DOOM: PATH TO ABSOLUTE POWER",
                center,
                24,
                0xFFE0E0E0
        );

        graphics.drawCenteredString(
                this.font,
                stage == DoomSkillTree.Stage.DOCTOR_DOOM
                        ? "DOCTOR DOOM - GREEN POWER"
                        : "GOD EMPEROR DOOM - WHITE POWER",
                center,
                109,
                stage == DoomSkillTree.Stage.DOCTOR_DOOM
                        ? 0xFF55DD66
                        : 0xFFFFFFFF
        );

        // Skill descriptions

        List<DoomSkillTree.Skill> skills =
                DoomSkillTree.getBranch(stage, branch);

        int y = 137;

        for (DoomSkillTree.Skill skill : skills) {

            graphics.fill(
                    left + 20,
                    y - 5,
                    left + PANEL_WIDTH - 20,
                    y + 55,
                    0xFF222A32
            );

            graphics.drawString(
                    this.font,
                    skill.name(),
                    left + 30,
                    y + 2,
                    0xFFFFFFFF
            );

            graphics.drawString(
                    this.font,
                    skill.description(),
                    left + 30,
                    y + 17,
                    0xFFB8C0C8
            );

            graphics.drawString(
                    this.font,
                    "Cost: " + skill.pointCost() + " skill points",
                    left + 30,
                    y + 32,
                    0xFFFFD56A
            );

            String prerequisites =
                    skill.prerequisites().isEmpty()
                            ? "No prerequisites"
                            : "Requires: " +
                              String.join(", ", skill.prerequisites());

            graphics.drawString(
                    this.font,
                    prerequisites,
                    left + 30,
                    y + 44,
                    0xFF9AA6B2
            );

            y += 65;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
