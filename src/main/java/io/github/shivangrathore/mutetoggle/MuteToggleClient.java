package io.github.shivangrathore.mutetoggle;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}

//? if >=1.21.9
import net.minecraft.resources.Identifier;

/**
 * Binds a key that toggles the master volume between 0 and its previous level.
 *
 * <p>Source here is written against the newest supported Minecraft version;
 * Stonecutter rewrites the {@code //?} blocks for the older ones. Everything
 * this class touches kept the same Mojang-mapped name and signature from 1.20.1
 * onwards except key registration and the HUD accessor, which is why those are
 * the only two places with version conditionals.
 */
public class MuteToggleClient implements ClientModInitializer {
    public static final String MOD_ID = "mutetoggle";
    public static final String VERSION = /*$ mod_version*/ "1.0.0";

    /** Volume restored on unmute if the master volume was already 0 when muted. */
    private static final double FALLBACK_VOLUME = 0.5D;

    private static KeyMapping muteKey;

    private double savedVolume = FALLBACK_VOLUME;

    @Override
    public void onInitializeClient() {
        muteKey = registerMuteKey();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // consumeClick() drains the queued presses, so holding the key down
            // toggles once rather than once per tick.
            while (muteKey.consumeClick()) toggle(client);
        });
    }

    private static KeyMapping registerMuteKey() {
        // 1.21.9 replaced the category translation key with a Category object.
        //? if >=1.21.9 {
        KeyMapping mapping = new KeyMapping(
                "key." + MOD_ID + ".toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main")));
        //?} else {
        /*KeyMapping mapping = new KeyMapping(
                "key." + MOD_ID + ".toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories." + MOD_ID);
        *///?}

        // 26.1 renamed the Fabric API module and its helper along with it.
        //? if >=26.1 {
        return KeyMappingHelper.registerKeyMapping(mapping);
        //?} else {
        /*return KeyBindingHelper.registerKeyBinding(mapping);
        *///?}
    }

    private void toggle(Minecraft client) {
        Options options = client.options;
        // Read the live value rather than tracking a muted flag, so changing the
        // volume in the options screen while muted behaves sensibly.
        double volume = options.getSoundSourceVolume(SoundSource.MASTER);

        if (volume > 0.0D) {
            this.savedVolume = volume;
            setMasterVolume(options, 0.0D);
            notify(client, "text." + MOD_ID + ".muted");
        } else {
            setMasterVolume(options, this.savedVolume > 0.0D ? this.savedVolume : FALLBACK_VOLUME);
            notify(client, "text." + MOD_ID + ".unmuted");
        }
    }

    private static void setMasterVolume(Options options, double volume) {
        options.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
        // Without this the change is lost if the game is killed rather than quit.
        options.save();
    }

    private static void notify(Minecraft client, String key) {
        // 26.2 split the HUD out of Gui into its own class.
        //? if >=26.2 {
        client.gui.hud.setOverlayMessage(Component.translatable(key), false);
        //?} else {
        /*client.gui.setOverlayMessage(Component.translatable(key), false);
        *///?}
    }
}
