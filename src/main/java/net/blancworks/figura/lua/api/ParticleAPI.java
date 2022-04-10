package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.AbstractMap;
import java.util.HashMap;

public class ParticleAPI {

    public static HashMap<String, ParticleType<?>> particleTypes = new HashMap<>() {{
        for (Identifier id : Registry.PARTICLE_TYPE.getIds()) {
            ParticleType<?> type = Registry.PARTICLE_TYPE.get(id);

            put(id.getPath(), type);
            put(id.toString(), type);
        }
    }};

    public static Identifier getID() {
        return new Identifier("default", "particle");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set("addParticle", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    //argos
                    LuaValue arg1 = args.arg(1);
                    LuaValue arg2 = args.arg(2);
                    LuaValue arg3 = args.arg(3);
                    LuaValue arg4 = args.arg(4);

                    //setup particle
                    AbstractMap.Entry<Identifier, ParticleType<?>> particleType = particleSetup(script, arg1);
                    if (particleType == null) return NIL;

                    //particle special argos
                    ParticleEffect particle;
                    switch (particleType.getKey().toString()) {
                        //3 argos
                        case "minecraft:dust" -> {
                            LuaVector color = LuaVector.checkOrNew(arg3);
                            particle = new DustParticleEffect(color.asV3f(), color.w());
                        }
                        case "minecraft:falling_dust" -> {
                            BlockState state = BlockStateAPI.checkOrCreateBlockState(arg3);
                            particle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state);
                        }
                        case "minecraft:block" -> {
                            BlockState state = BlockStateAPI.checkOrCreateBlockState(arg3);
                            particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
                        }
                        case "minecraft:item" -> {
                            ItemStack stack = ItemStackAPI.checkOrCreateItemStack(arg3);
                            particle = new ItemStackParticleEffect(ParticleTypes.ITEM, stack);
                        }
                        case "minecraft:block_marker" -> {
                            BlockState state = BlockStateAPI.checkOrCreateBlockState(arg3);
                            particle = new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, state);
                        }
                        case "minecraft:shriek" -> {
                            int delay = arg3.checkint();
                            particle = new ShriekParticleEffect(delay);
                        }
                        case "minecraft:sculk_charge" -> {
                            float roll = arg3.checknumber().tofloat();
                            particle = new SculkChargeParticleEffect(roll);
                        }
                        //4 argos
                        case "minecraft:dust_color_transition" -> {
                            LuaVector fromColor = LuaVector.checkOrNew(arg3);
                            LuaVector toColor = LuaVector.checkOrNew(arg4);
                            particle = new DustColorTransitionParticleEffect(fromColor.asV3f(), toColor.asV3f(), fromColor.w());
                        }
                        case "minecraft:vibration" -> {
                            LuaVector dist = LuaVector.checkOrNew(arg3);
                            int speed = arg4.checkint();

                            BlockPositionSource distance = new BlockPositionSource(new BlockPos(dist.asV3d()));
                            particle = new VibrationParticleEffect(distance, speed);
                        }
                        //default particle type
                        default -> {
                            if (particleType.getValue() instanceof DefaultParticleType type)
                                particle = type;
                            else return NIL;
                        }
                    }

                    //add particle
                    summonParticle(particle, LuaVector.checkOrNew(arg2));

                    return NIL;
                }
            });
        }};
    }

    private static AbstractMap.Entry<Identifier, ParticleType<?>> particleSetup(CustomScript script, LuaValue id) {
        //check string or script particle count
        if (!id.isstring() || script.particleSpawnCount < 1)
            return null;

        //increase particle count
        script.particleSpawnCount--;

        //get particle id and particle type
        Identifier identifier = new Identifier(id.checkjstring());
        ParticleType<?> type = particleTypes.get(identifier.toString());

        //return
        return type == null ? null : new AbstractMap.SimpleEntry<>(identifier, type);
    }

    private static void summonParticle(ParticleEffect particle, LuaVector pos) {
        World w = MinecraftClient.getInstance().world;

        //summon particle
        if (!MinecraftClient.getInstance().isPaused() && w != null) {
            w.addParticle(particle,
                    pos.x(), pos.y(), pos.z(), pos.w(), pos.t(), pos.h()
            );
        }
    }
}
