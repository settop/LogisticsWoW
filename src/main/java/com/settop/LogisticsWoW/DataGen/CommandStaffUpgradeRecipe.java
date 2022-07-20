package com.settop.LogisticsWoW.DataGen;

import com.google.gson.JsonObject;
import com.settop.LogisticsWoW.Items.WispCommandStaff;
import com.settop.LogisticsWoW.LogisticsWoW;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class CommandStaffUpgradeRecipe implements CraftingRecipe, net.minecraftforge.common.crafting.IShapedRecipe<CraftingContainer>
{
    private final ShapedRecipe baseRecipe;

    public CommandStaffUpgradeRecipe(@NonNull ShapedRecipe baseRecipe)
    {
        this.baseRecipe = baseRecipe;
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level)
    {
        return baseRecipe.matches(container, level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer craftingContainer)
    {
        IItemHandler sourceStaffInv = null;
        for(int i = 0; i < craftingContainer.getContainerSize(); ++i)
        {
            ItemStack staffItem = craftingContainer.getItem(i);
            if(staffItem.getItem() instanceof WispCommandStaff)
            {
                LazyOptional<IItemHandler> itemHandler = staffItem.getCapability(ITEM_HANDLER_CAPABILITY);
                sourceStaffInv = itemHandler.resolve().orElse(null);
                break;
            }
        }
        ItemStack upgradedStaff = baseRecipe.assemble(craftingContainer);
        if(sourceStaffInv != null)
        {
            LazyOptional<IItemHandler> itemHandlerOpt = upgradedStaff.getCapability(ITEM_HANDLER_CAPABILITY);
            IItemHandler finalSourceStaffInv = sourceStaffInv;
            itemHandlerOpt.ifPresent((itemHandler)->
            {
                assert itemHandler.getSlots() >= finalSourceStaffInv.getSlots();
                for(int i = 0; i < itemHandler.getSlots(); ++i)
                {
                    itemHandler.insertItem(i, finalSourceStaffInv.getStackInSlot(i), false);
                }
            });
        }

        return upgradedStaff;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return baseRecipe.canCraftInDimensions(width, height);
    }

    @Override
    public @NotNull ItemStack getResultItem()
    {
        return baseRecipe.getResultItem();
    }

    @Override
    public @NotNull ResourceLocation getId()
    {
        return baseRecipe.getId();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer()
    {
        return LogisticsWoW.RecipeSerializers.COMMAND_STAFF_UPGRADE.get();
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer craftingContainer)
    {
        return NonNullList.withSize(craftingContainer.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients()
    {
        return baseRecipe.getIngredients();
    }

    @Override
    public boolean isSpecial()
    {
        return baseRecipe.isSpecial();
    }

    @Override
    public @NotNull String getGroup()
    {
        return baseRecipe.getGroup();
    }

    @Override
    public @NotNull ItemStack getToastSymbol()
    {
        return baseRecipe.getToastSymbol();
    }

    @Override
    public boolean isIncomplete()
    {
        return baseRecipe.isIncomplete();
    }

    @Override
    public int getRecipeWidth()
    {
        return baseRecipe.getRecipeWidth();
    }

    @Override
    public int getRecipeHeight()
    {
        return baseRecipe.getRecipeHeight();
    }

    public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<RecipeSerializer<?>>  implements RecipeSerializer<CommandStaffUpgradeRecipe>
    {
        public @NotNull CommandStaffUpgradeRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json)
        {
            ShapedRecipe baseRecipe = RecipeSerializer.SHAPED_RECIPE.fromJson(recipeId, json);
            return new CommandStaffUpgradeRecipe(baseRecipe);
        }

        public CommandStaffUpgradeRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf)
        {
            ShapedRecipe baseRecipe = RecipeSerializer.SHAPED_RECIPE.fromNetwork(recipeId, buf);
            assert baseRecipe != null;
            return new CommandStaffUpgradeRecipe(baseRecipe);
        }

        public void toNetwork(@NotNull FriendlyByteBuf buf, CommandStaffUpgradeRecipe recipe)
        {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe.baseRecipe);
        }
    }
}
