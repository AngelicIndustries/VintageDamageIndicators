package industries.angelic.vintagedamageindicators.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.vintagedamageindicators.gui.GuiFactory;

public class OpenOverrideEditorCommand extends CommandBase {

    private boolean openRequested;

    @Override
    public String getCommandName() {
        return "vdioverrides";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vdioverrides";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("vdioverridegui");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        this.openRequested = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !this.openRequested) {
            return;
        }

        this.openRequested = false;
        if (!GuiFactory.openEntityOverrideScreen(null)) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.thePlayer != null) {
                minecraft.thePlayer.addChatMessage(new ChatComponentText("Failed to open the override editor."));
            }
        }
    }
}
