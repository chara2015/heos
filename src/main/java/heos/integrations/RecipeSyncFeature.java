package heos.integrations;

import heos.Heos;
import heos.interfaces.PlayerAuth;
import heos.utils.HeosLogger;
import heos.utils.ProtocolCompatibility;

//? if >= 1.21.2 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
//? if >= 1.21.11 {
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
//?}
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//?}

/**
 * Mirrors the recipe viewer handshake behavior used by the Folia plugin.
 */
public final class RecipeSyncFeature {
    //? if >= 1.21.2 {
    private static final byte HANDSHAKE_PACKET_ID = 0;
    private static final int LATEST_JEI_REI_PROTOCOL_VERSION = 19;
    private static final CustomPacketPayload.Type<RecipeViewerHandshakePayload> JEI_HANDSHAKE =
            handshakeType("jei", "network");
    private static final CustomPacketPayload.Type<RecipeViewerHandshakePayload> REI_HANDSHAKE =
            handshakeType("rei", "networking");
    private static final CustomPacketPayload.Type<OpaquePayload> REI_MOVE_ITEMS =
            opaqueType("roughlyenoughitems", "move_items_new");
    //?}

    private RecipeSyncFeature() {
    }

    public static void initialize() {
        //? if >= 1.21.2 {
        if (!Heos.getConfig().enableRecipeViewerSync) {
            HeosLogger.info("Recipe viewer sync disabled by config");
            return;
        }

        boolean viaPlatformLoaded = ProtocolCompatibility.isViaPlatformLoaded();
        if (viaPlatformLoaded) {
            HeosLogger.info("Recipe viewer sync disabled because ViaVersion/ViaFabric is installed");
            return;
        }

        //? if >= 1.21.11 {
        synchronizeRecipeSerializers();
        //?}

        registerHandshake(JEI_HANDSHAKE);
        registerHandshake(REI_HANDSHAKE);
        if (!ProtocolCompatibility.isModLoaded("roughlyenoughitems")) {
            registerReiMoveItems();
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!isSameProtocol(handler.getPlayer())) {
                ServerPlayer player = handler.getPlayer();
                HeosLogger.debug("Skipping eager recipe viewer sync for " + player.getName().getString()
                        + " because client protocol " + ((PlayerAuth) player).heos$getClientProtocolVersion()
                        + " differs from server protocol " + SharedConstants.getProtocolVersion());
            }
        });
        HeosLogger.info("Recipe viewer sync registered");
        //?}
    }

    //? if >= 1.21.2 {
    //? if >= 1.21.11 {
    private static void synchronizeRecipeSerializers() {
        int synchronizedCount = 0;

        for (RecipeSerializer<?> serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
            Identifier key = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
            if (!"minecraft".equals(key.getNamespace())) {
                continue;
            }

            try {
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
                synchronizedCount++;
            } catch (RuntimeException exception) {
                HeosLogger.warn("Failed to enable recipe sync for serializer "
                        + key + ": " + exception.getMessage());
            }
        }

        HeosLogger.info("Enabled Fabric recipe sync for " + synchronizedCount + " vanilla recipe serializers");
    }

    //?}

    private static CustomPacketPayload.Type<RecipeViewerHandshakePayload> handshakeType(String namespace, String path) {
        //? if >= 1.21.11 {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(namespace, path));
        //?} else {
        /*return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(namespace, path));
        *///?}
    }

    private static CustomPacketPayload.Type<OpaquePayload> opaqueType(String namespace, String path) {
        //? if >= 1.21.11 {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(namespace, path));
        //?} else {
        /*return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(namespace, path));
        *///?}
    }

    private static void registerHandshake(CustomPacketPayload.Type<RecipeViewerHandshakePayload> type) {
        StreamCodec<RegistryFriendlyByteBuf, RecipeViewerHandshakePayload> codec = RecipeViewerHandshakePayload.codec(type);
        //? if >= 26 {
        /*PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        *///?} else {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);
        //?}
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            if (Heos.getConfig().enableRecipeViewerSync && payload.packetId() == HANDSHAKE_PACKET_ID) {
                if (!isSameProtocol(context.player())) {
                    HeosLogger.debug("Ignoring recipe viewer handshake from " + context.player().getName().getString()
                            + " because client protocol " + ((PlayerAuth) context.player()).heos$getClientProtocolVersion()
                            + " differs from server protocol " + SharedConstants.getProtocolVersion());
                    return;
                }
                giveAllRecipes(context.player(), context.server());
                sendHandshakeIfSupported(context.player(), type);
            }
        });
    }

    private static void registerReiMoveItems() {
        StreamCodec<RegistryFriendlyByteBuf, OpaquePayload> codec = OpaquePayload.codec(REI_MOVE_ITEMS);
        //? if >= 26 {
        /*PayloadTypeRegistry.serverboundPlay().register(REI_MOVE_ITEMS, codec);
        *///?} else {
        PayloadTypeRegistry.playC2S().register(REI_MOVE_ITEMS, codec);
        //?}
        // REI checks for this channel before it trusts vanilla recipe updates.
        ServerPlayNetworking.registerGlobalReceiver(REI_MOVE_ITEMS, (payload, context) -> {
        });
    }

    private static void giveAllRecipes(ServerPlayer player, MinecraftServer server) {
        player.awardRecipes(server.getRecipeManager().getRecipes());
    }

    private static boolean isSameProtocol(ServerPlayer player) {
        return ((PlayerAuth) player).heos$isSameProtocol();
    }

    private static void sendHandshakeIfSupported(ServerPlayer player, CustomPacketPayload.Type<RecipeViewerHandshakePayload> type) {
        if (!ServerPlayNetworking.canSend(player, type)) {
            return;
        }
        ServerPlayNetworking.send(player, new RecipeViewerHandshakePayload(type, HANDSHAKE_PACKET_ID, LATEST_JEI_REI_PROTOCOL_VERSION));
    }

    private record RecipeViewerHandshakePayload(
            CustomPacketPayload.Type<RecipeViewerHandshakePayload> type,
            byte packetId,
            int protocolVersion
    ) implements CustomPacketPayload {
        private static StreamCodec<RegistryFriendlyByteBuf, RecipeViewerHandshakePayload> codec(
                CustomPacketPayload.Type<RecipeViewerHandshakePayload> type
        ) {
            return CustomPacketPayload.codec(RecipeViewerHandshakePayload::write, buffer -> read(type, buffer));
        }

        private static RecipeViewerHandshakePayload read(
                CustomPacketPayload.Type<RecipeViewerHandshakePayload> type,
                RegistryFriendlyByteBuf buffer
        ) {
            return new RecipeViewerHandshakePayload(type, buffer.readByte(), buffer.readInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(packetId);
            buffer.writeInt(protocolVersion);
        }
    }

    private record OpaquePayload(
            CustomPacketPayload.Type<OpaquePayload> type,
            byte[] data
    ) implements CustomPacketPayload {
        private static StreamCodec<RegistryFriendlyByteBuf, OpaquePayload> codec(
                CustomPacketPayload.Type<OpaquePayload> type
        ) {
            return CustomPacketPayload.codec(OpaquePayload::write, buffer -> read(type, buffer));
        }

        private static OpaquePayload read(
                CustomPacketPayload.Type<OpaquePayload> type,
                RegistryFriendlyByteBuf buffer
        ) {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new OpaquePayload(type, data);
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBytes(data);
        }
    }
    //?}
}
