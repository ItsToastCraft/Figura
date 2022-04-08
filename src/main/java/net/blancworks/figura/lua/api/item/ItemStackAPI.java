package net.blancworks.figura.lua.api.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.NBTAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Optional;

public class ItemStackAPI {

    public static Identifier getID() {
        return new Identifier("default", "item_stack");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set("createItem", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    ItemStack item = checkOrCreateItemStack(arg1);
                    if (!arg2.isnil()) setItemNbt(item, arg2.checkjstring());
                    return getTable(item);
                }
            });
        }};
    }

    public static LuaTable getTable(ItemStack stack) {
        return new LuaTable() {{
            set("figura$item_stack", LuaValue.userdataOf(stack));

            set("getType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(Registry.ITEM.getId(stack.getItem()).toString());
                }
            });

            set("getTag", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    NbtElement tag = stack.getNbt();
                    return NBTAPI.fromTag(tag);
                }
            });

            set("getCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getCount());
                }
            });

            set("getDamage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getDamage());
                }
            });

            set("getCooldown", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getBobbingAnimationTime());
                }
            });

            set("hasGlint", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(stack.hasGlint());
                }
            });

            set("getItemTags", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable table = new LuaTable();

                    Registry<Item> itemRegistry = MinecraftClient.getInstance().world.getRegistryManager().get(Registry.ITEM_KEY);
                    Optional<RegistryKey<Item>> key = itemRegistry.getKey(stack.getItem());

                    for (TagKey<Item> itemTagKey : itemRegistry.entryOf(key.get()).streamTags().toList()) {
                        table.insert(0, LuaValue.valueOf(itemTagKey.id().toString()));
                    }

                    return table;
                }
            });


            set("setCount", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    stack.setCount(arg.checkint());
                    return NIL;
                }
            });

            set("setDamage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    stack.setDamage(arg.checkint());
                    return NIL;
                }
            });

            set("setTag", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    setItemNbt(stack, arg.checkjstring());
                    return NIL;
                }
            });

            set("isBlockItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(stack.getItem() instanceof BlockItem);
                }
            });

            set("isFood", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(stack.isFood());
                }
            });

            set("getUseAction", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getUseAction().toString());
                }
            });

            set("getName", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getName().getString());
                }
            });

            set("getMaxCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getMaxCount());
                }
            });

            set("getRarity", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getRarity().toString());
                }
            });

            set("isEnchantable", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.isEnchantable());
                }
            });

            set("getMaxDamage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getMaxDamage());
                }
            });

            set("isDamageable", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.isDamageable());
                }
            });

            set("isStackable", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.isStackable());
                }
            });

            set("getRepairCost", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getRepairCost());
                }
            });

            set("getMaxUseTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(stack.getMaxUseTime());
                }
            });

            set("toStackString", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    String ret = Registry.ITEM.getId(stack.getItem()).toString();

                    NbtCompound nbt = stack.getNbt();
                    if (nbt != null)
                        ret += nbt.toString();

                    return LuaValue.valueOf(ret);
                }
            });
        }};
    }

    public static void setItemNbt(ItemStack item, String s) {
        StringReader reader = new StringReader(s);

        try {
            item.setNbt((NbtCompound) new StringNbtReader(reader).parseElement());
        } catch (CommandSyntaxException e) {
            throw new LuaError("NBT parse error\n" + e.getMessage());
        } catch (Exception e) {
            throw new LuaError("Could not parse NBT");
        }
    }

    public static ItemStack checkOrCreateItemStack(LuaValue arg1) {
        ItemStack item = (ItemStack) arg1.get("figura$item_stack").touserdata(ItemStack.class);
        if (item != null)
            return item;

        try {
            return ItemStackArgumentType.itemStack(new CommandRegistryAccess(DynamicRegistryManager.BUILTIN.get())).parse(new StringReader(arg1.checkjstring())).createStack(1, false);
        } catch (CommandSyntaxException e) {
            throw new LuaError("Could not create item stack\n" + e.getMessage());
        }
    }
}
