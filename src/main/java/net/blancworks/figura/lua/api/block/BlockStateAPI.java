package net.blancworks.figura.lua.api.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.AbstractBlockAccessorMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

public class BlockStateAPI {

    public static Identifier getID() {
        return new Identifier("default", "block_state");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set("createBlock", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    BlockState block = checkOrCreateBlockState(arg1);
                    BlockPos pos = arg2.isnil() ? ((LuaVector) LuaVector.of(Vec3f.ZERO)).asBlockPos() : LuaVector.checkOrNew(arg2).asBlockPos();
                    return getTable(block, MinecraftClient.getInstance().world, pos);
                }
            });
        }};
    }

    public static LuaTable getTable(BlockState state, World world, BlockPos pos) {
        return new BlockStateTable(state, world, pos).getTable();
    }

    private static class BlockStateTable extends LuaTable {
        private final BlockState state;
        private final World world;
        private BlockPos blockPos;

        public BlockStateTable(BlockState state, World world, BlockPos blockPos) {
            this.state = state;
            this.world = world;
            this.blockPos = blockPos;
        }

        public LuaTable getTable() {
            LuaTable tbl = (LuaTable) NBTAPI.fromTag(NbtHelper.fromBlockState(state));

            tbl.set(LuaValue.valueOf("figura$block_state"), LuaValue.userdataOf(state));

            tbl.set(LuaValue.valueOf("setPos"), new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    blockPos = LuaVector.checkOrNew(arg).asBlockPos();
                    return NIL;
                }
            });

            tbl.set(LuaValue.valueOf("getBlockTags"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable table = new LuaTable();

                    Registry<Block> blockRegistry = world.getRegistryManager().get(Registry.BLOCK_KEY);
                    Optional<RegistryKey<Block>> key = blockRegistry.getKey(state.getBlock());

                    for (TagKey<Block> blockTagKey : blockRegistry.entryOf(key.get()).streamTags().toList()) {
                        table.insert(0, LuaValue.valueOf(blockTagKey.id().toString()));
                    }

                    return table;
                }
            });

            tbl.set(LuaValue.valueOf("getMaterial"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    for (Field field : Material.class.getFields()) {
                        try {
                            if (field.get(null) == state.getMaterial())
                                return LuaValue.valueOf(field.getName());
                        } catch (Exception ignored) {}
                    }

                    return NIL;
                }
            });

            tbl.set(LuaValue.valueOf("getMapColor"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getBlock().getDefaultMapColor().color);
                }
            });

            tbl.set(LuaValue.valueOf("isSolidBlock"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isSolidBlock(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("isFullCube"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isFullCube(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("hasBlockEntity"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.hasBlockEntity());
                }
            });

            tbl.set(LuaValue.valueOf("isOpaque"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isOpaque());
                }
            });

            tbl.set(LuaValue.valueOf("hasEmissiveLighting"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.hasEmissiveLighting(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("isTranslucent"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isTranslucent(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("emitsRedstonePower"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.emitsRedstonePower());
                }
            });

            tbl.set(LuaValue.valueOf("getOpacity"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getOpacity(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("getLuminance"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getLuminance());
                }
            });

            tbl.set(LuaValue.valueOf("getHardness"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getHardness(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("getComparatorOutput"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getComparatorOutput(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("getSlipperiness"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getBlock().getSlipperiness());
                }
            });

            tbl.set(LuaValue.valueOf("getVelocityMultiplier"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getBlock().getVelocityMultiplier());
                }
            });

            tbl.set(LuaValue.valueOf("getJumpVelocityMultiplier"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getBlock().getJumpVelocityMultiplier());
                }
            });

            tbl.set(LuaValue.valueOf("getBlastResistance"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getBlock().getBlastResistance());
                }
            });

            tbl.set(LuaValue.valueOf("isCollidable"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(((AbstractBlockAccessorMixin) state.getBlock()).isCollidable());
                }
            });

            tbl.set(LuaValue.valueOf("getCollisionShape"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return voxelShapeToTable(state.getCollisionShape(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("getOutlineShape"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return voxelShapeToTable(state.getOutlineShape(world, blockPos));
                }
            });

            tbl.set(LuaValue.valueOf("getSoundGroup"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable stateGroup = new LuaTable();
                    BlockSoundGroup snd = state.getSoundGroup();

                    stateGroup.set(LuaValue.valueOf("pitch"), LuaValue.valueOf(snd.getPitch()));
                    stateGroup.set(LuaValue.valueOf("volume"), LuaValue.valueOf(snd.getVolume()));
                    stateGroup.set(LuaValue.valueOf("break"), LuaValue.valueOf(snd.getBreakSound().getId().toString()));
                    stateGroup.set(LuaValue.valueOf("fall"), LuaValue.valueOf(snd.getFallSound().getId().toString()));
                    stateGroup.set(LuaValue.valueOf("hit"), LuaValue.valueOf(snd.getHitSound().getId().toString()));
                    stateGroup.set(LuaValue.valueOf("plate"), LuaValue.valueOf(snd.getPlaceSound().getId().toString()));
                    stateGroup.set(LuaValue.valueOf("step"), LuaValue.valueOf(snd.getStepSound().getId().toString()));

                    return stateGroup;
                }
            });

            tbl.set(LuaValue.valueOf("getEntityData"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    BlockEntity entity = world.getBlockEntity(blockPos);
                    return NBTAPI.fromTag(entity != null ? entity.createNbt() : new NbtCompound());
                }
            });

            tbl.set(LuaValue.valueOf("toStateString"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    BlockEntity entity = world.getBlockEntity(blockPos);
                    NbtCompound tag = entity != null ? entity.createNbt() : new NbtCompound();

                    return LuaValue.valueOf(BlockArgumentParser.stringifyBlockState(state) + tag);
                }
            });

            return tbl;
        }
    }

    private static LuaValue voxelShapeToTable(VoxelShape shape) {
        LuaTable shapes = new LuaTable();
        List<Box> boxes = shape.getBoundingBoxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            shapes.set(i + 1, new LuaVector((float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ));
        }
        return shapes;
    }

    public static BlockState checkOrCreateBlockState(LuaValue arg1) {
        BlockState block = (BlockState) arg1.get("figura$block_state").touserdata(BlockState.class);
        if (block != null)
            return block;

        try {
            return BlockStateArgumentType.blockState(new CommandRegistryAccess(DynamicRegistryManager.BUILTIN.get())).parse(new StringReader(arg1.checkjstring())).getBlockState();
        } catch (CommandSyntaxException e) {
            throw new LuaError("Could not create block state\n" + e.getMessage());
        }
    }
}