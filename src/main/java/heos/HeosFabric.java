package heos;

import heos.commands.BanCommands;
import heos.commands.ChangePasswordCommand;
import heos.commands.HeosAdminCommand;
import heos.commands.LoginCommand;
import heos.commands.RegisterCommand;
import heos.event.AuthEventHandler;
import heos.utils.HeosLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
//? if < 1.21 {
/*import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
*///?}

/**
 * Fabric mod initializer for Heos
 */
public class HeosFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Heos.gameDirectory = FabricLoader.getInstance().getGameDir();

        HeosLogger.info("Initializing Heos authentication system...");
        HeosLogger.info("Game directory: " + Heos.gameDirectory);

        registerCommands();
        registerEvents();

        HeosLogger.info("Heos authentication system initialized successfully!");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LoginCommand.registerCommand(dispatcher);
            RegisterCommand.registerCommand(dispatcher);
            ChangePasswordCommand.register(dispatcher);
            HeosAdminCommand.register(dispatcher);
            BanCommands.register(dispatcher);
            HeosLogger.info("Registered Heos commands");
        });
    }

    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(Heos::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(Heos::onStopServer);

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
            AuthEventHandler.onBreakBlock(player)
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
            AuthEventHandler.onUseBlock(player)
        );

        //? if >= 1.21 {
        UseItemCallback.EVENT.register((player, world, hand) ->
            AuthEventHandler.onUseItem(player)
        );
        //?} else {
        /*UseItemCallback.EVENT.register((player, world, hand) -> {
            InteractionResult result = AuthEventHandler.onUseItem(player);
            if (result == InteractionResult.FAIL) {
                return InteractionResultHolder.fail(ItemStack.EMPTY);
            }
            if (result == InteractionResult.SUCCESS) {
                return InteractionResultHolder.success(ItemStack.EMPTY);
            }
            return InteractionResultHolder.pass(ItemStack.EMPTY);
        });
        *///?}

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
            AuthEventHandler.onAttackEntity(player)
        );

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
            AuthEventHandler.onUseEntity(player)
        );
    }
}
