/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.turtle.upgrades.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Register a new turtle tool.
 */
public class AddTurtleTool implements IAction
{
    private interface Factory
    {
        TurtleTool create( ResourceLocation location, ItemStack craftItem, ItemStack toolItem );
    }

    private static final Map<String, Factory> kinds = new HashMap<>();

    static
    {
        kinds.put( "tool", TurtleTool::new );
        kinds.put( "axe", TurtleAxe::new );
        kinds.put( "hoe", TurtleHoe::new );
        kinds.put( "shovel", TurtleShovel::new );
        kinds.put( "sword", TurtleSword::new );
    }

    private final String id;
    private final ItemStack craftItem;
    private final ItemStack toolItem;
    private final String kind;

    public AddTurtleTool( String id, ItemStack craftItem, ItemStack toolItem, String kind )
    {
        this.id = id;
        this.craftItem = craftItem;
        this.toolItem = toolItem;
        this.kind = kind;
    }

    @Override
    public void apply()
    {
        Factory factory = kinds.get( kind );
        if( factory == null )
        {
            ComputerCraft.log.error( "Unknown turtle upgrade kind '{}' (this should have been rejected by verify!)", kind );
            return;
        }

        try
        {
            TurtleUpgrades.register( factory.create( new ResourceLocation( id ), craftItem, toolItem ) );
        }
        catch( RuntimeException e )
        {
            ComputerCraft.log.error( "Registration of turtle tool failed", e );
        }
    }

    @Override
    public String describe()
    {
        return String.format( "Add new turtle %s '%s' (crafted with '%s', uses a '%s')", kind, id, craftItem, toolItem );
    }

    public Optional<String> getValidationProblem()
    {
        if( craftItem.isEmpty() ) return Optional.of( "Crafting item stack is empty." );
        if( craftItem.hasTagCompound() && !craftItem.getTagCompound().hasNoTags() )
        {
            return Optional.of( "Crafting item has NBT." );
        }
        if( toolItem.isEmpty() ) return Optional.of( "Tool item stack is empty." );
        if( !kinds.containsKey( kind ) ) return Optional.of( String.format( "Unknown kind '%s'.", kind ) );

        if( TurtleUpgrades.get( id ) != null )
        {
            return Optional.of( String.format( "An upgrade with the same name ('%s') has already been registered.", id ) );
        }

        return Optional.empty();
    }
}
