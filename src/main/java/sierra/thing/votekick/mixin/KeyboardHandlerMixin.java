package sierra.thing.votekick.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sierra.thing.votekick.client.VoteKickClient;
import sierra.thing.votekick.client.VoteKickHud;

/**
 * Intercepts keyboard input to handle the vote keys.
 * Took forever to get this working correctly with Fabric keybinds.
 *
 * Had to be careful to only block vanilla behavior when actually voting,
 * otherwise players couldn't use F3 or chat when vote is active.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // Bail early if no vote active or wrong window
        if (!VoteKickHud.isVotePanelShowing() ||
                minecraft == null ||
                minecraft.getWindow().getWindow() != window) {
            return;
        }

        // Check against the keybinds - this properly handles rebinding
        // Originally used direct key codes which broke with custom bindings
        boolean isYesKey = VoteKickClient.voteYesKey.matches(key, scancode);
        boolean isNoKey = VoteKickClient.voteNoKey.matches(key, scancode);

        // Only intercept if it's one of our keys AND player hasn't voted yet
        if ((isYesKey || isNoKey) && !VoteKickHud.hasPlayerVoted()) {
            // Make sure it's an initial press - not held down or released
            if (action == GLFW.GLFW_PRESS) {
                // Send vote to server
                VoteKickClient.castVote(isYesKey);

                // Cancel vanilla handling so we don't trigger other actions
                // bounded to the same keys (like opening chat)
                ci.cancel();
            }
        }

        // Let vanilla handle everything else normally
    }
}