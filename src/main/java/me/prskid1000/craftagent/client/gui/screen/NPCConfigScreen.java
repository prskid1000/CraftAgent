package me.prskid1000.craftagent.client.gui.screen;

import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.prskid1000.craftagent.client.networking.ClientNetworkManager;
import me.prskid1000.craftagent.config.NPCConfig;
import me.prskid1000.craftagent.llm.LLMType;
import me.prskid1000.craftagent.networking.packet.CreateNpcPacket;
import me.prskid1000.craftagent.networking.packet.UpdateNpcConfigPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static me.prskid1000.craftagent.CraftAgent.MOD_ID;

public class NPCConfigScreen extends ConfigScreen<NPCConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "npcconfig");

    public NPCConfigScreen(
            ClientNetworkManager networkManager,
            NPCConfig npcConfig,
            boolean isEdit
    ) {
        super(networkManager, npcConfig, isEdit, ID);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");

        drawNpcNameRow(panel);
        drawLLMTypeButtons(panel);

        // these depend on llmType, so build once + rebuild on change
        redrawLlmDependentFields(panel);

        drawGenderRow(panel);
        drawAgeRow(panel);

        onPressSaveButton(rootComponent, button -> {
            if (isEdit) {
                networkManager.sendPacket(new UpdateNpcConfigPacket(config));
            } else {
                networkManager.sendPacket(new CreateNpcPacket(config));
            }
            close();
        });
    }

    private void drawNpcNameRow(FlowLayout panel) {
        LabelComponent npcNameLabel = panel.childById(LabelComponent.class, "npcName-label");

        if (isEdit) {
            npcNameLabel.text(Text.of(NPCConfig.EDIT_NPC.formatted(config.getNpcName())));
        } else {
            npcNameLabel.text(Text.of(NPCConfig.NPC_NAME));
            TextAreaComponent npcName = Components.textArea(Sizing.fill(35), Sizing.fill(7))
                    .text(config.getNpcName());
            npcName.onChanged().subscribe(config::setNpcName);

            panel.childById(FlowLayout.class, "npcNameRow").child(npcName);
        }
    }

    /**
     * LLM Type as buttons in a single row instead of dropdown
     */
    private void drawLLMTypeButtons(FlowLayout panel) {
        panel.childById(LabelComponent.class, "llmType-label").text(Text.of(NPCConfig.LLM_TYPE));

        FlowLayout llmTypeRow = panel.childById(FlowLayout.class, "llmTypeRow");

        // Remove any old button row if rebuild happens (safe)
        // Keep the label (first child), clear everything after it:
        while (llmTypeRow.children().size() > 1) {
            llmTypeRow.removeChild(llmTypeRow.children().get(1));
        }

        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(35), Sizing.content());
        buttonRow.gap(6);

        // If editing, you can lock selection (or still allow switching - your choice)
        buttonRow.child(typeButton(LLMType.OLLAMA, panel, isEdit));
        buttonRow.child(typeButton(LLMType.LM_STUDIO, panel, isEdit));

        llmTypeRow.child(buttonRow);
    }

    private ButtonComponent typeButton(LLMType type, FlowLayout panel, boolean disabled) {
        boolean active = config.getLlmType() == type;

        ButtonComponent btn = Components.button(Text.of(type.toString()), button -> {
            if (disabled) return;
            config.setLlmType(type);
            redrawLlmDependentFields(panel);
            // optionally also refresh visual state by rebuilding type row
            drawLLMTypeButtons(panel);
        });

        // Light visual hint: add top margin; you can also add tooltip etc.
        btn.margins(Insets.top(2));

        // Optional: disable current active button to make it feel “selected”
        if (active) btn.active(!disabled && false);
        if (disabled) btn.active(false);

        return btn;
    }

    /**
     * Rebuild model row + llmInfo whenever llmType changes
     */
    private void redrawLlmDependentFields(FlowLayout panel) {
        drawLLMModelRow(panel);
        drawLlmInfo(panel);
    }

    private void drawLLMModelRow(FlowLayout panel) {
        panel.childById(LabelComponent.class, "llmModel-label").text(Text.of(NPCConfig.LLM_MODEL));

        FlowLayout llmModelRow = panel.childById(FlowLayout.class, "llmModelRow");

        // Clear old model input(s) but keep label
        while (llmModelRow.children().size() > 1) {
            llmModelRow.removeChild(llmModelRow.children().get(1));
        }

        // Both types use model text input
        TextAreaComponent llmModel = Components.textArea(Sizing.fill(35), Sizing.fill(7))
                .text(config.getLlmModel());
        llmModel.onChanged().subscribe(config::setLlmModel);

        llmModelRow.child(llmModel);
    }

    private void drawLlmInfo(FlowLayout panel) {
        FlowLayout llmInfo = panel.childById(FlowLayout.class, "llmInfo");
        llmInfo.clearChildren();

        // URL row
        TextAreaComponent urlInput = Components.textArea(Sizing.fill(35), Sizing.fill(7));

        switch (config.getLlmType()) {
            case OLLAMA -> {
                llmInfo.child(Components.label(Text.of(NPCConfig.OLLAMA_URL)).shadow(true));
                urlInput.text(config.getOllamaUrl()).onChanged().subscribe(config::setOllamaUrl);
                llmInfo.child(urlInput);
            }
            case LM_STUDIO -> {
                llmInfo.child(Components.label(Text.of(NPCConfig.LM_STUDIO_URL)).shadow(true));
                urlInput.text(config.getLmStudioUrl()).onChanged().subscribe(config::setLmStudioUrl);
                llmInfo.child(urlInput);
            }
        }

        // System prompt row
        llmInfo.child(
                Components.label(Text.of(NPCConfig.CUSTOM_SYSTEM_PROMPT))
                        .shadow(true)
                        .margins(Insets.top(7))
        );

        TextAreaComponent customSystemPrompt = Components.textArea(Sizing.fill(35), Sizing.fill(40));
        customSystemPrompt.text(config.getCustomSystemPrompt())
                .onChanged()
                .subscribe(config::setCustomSystemPrompt);

        llmInfo.child(customSystemPrompt);
    }

    private void drawGenderRow(FlowLayout panel) {
        FlowLayout genderRow = panel.childById(FlowLayout.class, "genderRow");
        genderRow.clearChildren();

        genderRow.child(Components.label(Text.of(NPCConfig.GENDER)).shadow(true));

        DropdownComponent genderDropdown = Components.dropdown(Sizing.fill(35));
        String currentGender = config.getGender();
        if (currentGender == null || currentGender.isEmpty()) currentGender = "neutral";

        genderDropdown.button(Text.of(capitalize(currentGender)), b -> {});
        genderDropdown.button(Text.of("Male"), b -> config.setGender("male"));
        genderDropdown.button(Text.of("Female"), b -> config.setGender("female"));
        genderDropdown.button(Text.of("Neutral"), b -> config.setGender("neutral"));

        genderRow.child(genderDropdown);
    }

    private void drawAgeRow(FlowLayout panel) {
        FlowLayout ageRow = panel.childById(FlowLayout.class, "ageRow");
        ageRow.clearChildren();

        ageRow.child(Components.label(Text.of(NPCConfig.AGE)).shadow(true));

        TextAreaComponent ageInput = Components.textArea(Sizing.fill(35), Sizing.fill(7))
                .text(String.valueOf(config.getAge()));

        ageInput.onChanged().subscribe(value -> {
            try {
                int age = Integer.parseInt(value);
                config.setAge(age);
            } catch (NumberFormatException ignored) {}
        });

        ageRow.child(ageInput);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
