/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public class TurtleCompareCommand implements ITurtleCommand
{
    private final InteractDirection m_direction;

    public TurtleCompareCommand( InteractDirection direction )
    {
        m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get world direction from direction
        EnumFacing direction = m_direction.toWorldDir( turtle );

        // Get currently selected stack
        ItemStack selectedStack = turtle.getInventory().getStackInSlot( turtle.getSelectedSlot() );

        // Get stack representing thing in front
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );

        ItemStack lookAtStack = ItemStack.EMPTY;
        if( !world.isAirBlock( newPosition ) )
        {
            IBlockState lookAtState = world.getBlockState( newPosition );
            Block lookAtBlock = lookAtState.getBlock();
            if( !lookAtBlock.isAir( lookAtState, world, newPosition ) )
            {
                // Try getSilkTouchDrop first
                if( !lookAtBlock.hasTileEntity( lookAtState ) )
                {
                    try
                    {
                        Method method = ReflectionHelper.findMethod( Block.class, "getSilkTouchDrop", "func_180643_i", IBlockState.class );
                        lookAtStack = (ItemStack) method.invoke( lookAtBlock, lookAtState );
                    }
                    catch( ReflectiveOperationException | RuntimeException ignored )
                    {
                    }
                }

                // See if the block drops anything with the same ID as itself
                // (try 5 times to try and beat random number generators)
                for( int i = 0; i < 5 && lookAtStack.isEmpty(); i++ )
                {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    lookAtBlock.getDrops( drops, world, newPosition, lookAtState, 0 );
                    if( !drops.isEmpty() )
                    {
                        for( ItemStack drop : drops )
                        {
                            if( drop.getItem() == Item.getItemFromBlock( lookAtBlock ) )
                            {
                                lookAtStack = drop;
                                break;
                            }
                        }
                    }
                }

                // Last resort: roll our own (which will probably be wrong)
                if( lookAtStack.isEmpty() )
                {
                    Item item = Item.getItemFromBlock( lookAtBlock );
                    if( item != null && item.getHasSubtypes() )
                    {
                        lookAtStack = new ItemStack( item, 1, lookAtBlock.getMetaFromState( lookAtState ) );
                    }
                    else
                    {
                        lookAtStack = new ItemStack( item, 1, 0 );
                    }
                }
            }
        }

        // If they're both empty, obviously the same
        if( selectedStack.isEmpty() && lookAtStack.isEmpty() ) return TurtleCommandResult.success();

        // If the items don't match, obviously different.
        if( selectedStack.isEmpty() || lookAtStack == null || selectedStack.getItem() != lookAtStack.getItem() )
        {
            return TurtleCommandResult.failure();
        }

        // If the damage matches, or the damage doesn't matter, then the same.
        if( !selectedStack.getHasSubtypes() || selectedStack.getItemDamage() == lookAtStack.getItemDamage() )
        {
            return TurtleCommandResult.success();
        }

        // Otherwise just double check the translation is the same. It's a pretty good guess.
        return selectedStack.getUnlocalizedName().equals( lookAtStack.getUnlocalizedName() )
            ? TurtleCommandResult.success()
            : TurtleCommandResult.failure();
    }
}
