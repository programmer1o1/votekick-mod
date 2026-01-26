package sierra.thing.votekick.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
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

    //? if >=1.21.9 {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        // Check against the keybinds - this properly handles rebinding
        // Originally used direct key codes which broke with custom bindings
        boolean isYesKey = VoteKickClient.voteYesKey.matches(keyEvent);
        boolean isNoKey = VoteKickClient.voteNoKey.matches(keyEvent);

        handleVoteKeyPress(window, action, isYesKey, isNoKey, ci);
    }
    *///?} else {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // Check against the keybinds - this properly handles rebinding
        // Originally used direct key codes which broke with custom bindings
        boolean isYesKey = VoteKickClient.voteYesKey.matches(key, scancode);
        boolean isNoKey = VoteKickClient.voteNoKey.matches(key, scancode);

        handleVoteKeyPress(window, action, isYesKey, isNoKey, ci);
    }
    //?}

    private void handleVoteKeyPress(long window, int action, boolean isYesKey, boolean isNoKey, CallbackInfo ci) {
        //? if >=1.21.9 {
        /*long windowHandle = minecraft.getWindow().handle();
        *///?} else {
        long windowHandle = minecraft.getWindow().getWindow();
        //?}

        // Bail early if no vote active or wrong window
        if (!VoteKickHud.isVotePanelShowing() ||
                minecraft == null ||
                windowHandle != window) {
            return;
        }

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
