/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.blocks.ComputerState;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.util.StringUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem
{
    private static final String NBT_UPGRADE = "upgrade";
    private static final String NBT_UPGRADE_INFO = "upgrade_info";
    public static final String NBT_LIGHT = "modemLight";

    private static final String NBT_INSTANCE = "instanceID";
    private static final String NBT_SESSION = "sessionID";

    public ItemPocketComputer()
    {
        setMaxStackSize( 1 );
        setHasSubtypes( true );
        setUnlocalizedName( "computercraft:pocket_computer" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        addPropertyOverride( new ResourceLocation( ComputerCraft.MOD_ID, "state" ), COMPUTER_STATE );
        addPropertyOverride( new ResourceLocation( ComputerCraft.MOD_ID, "coloured" ), COMPUTER_COLOURED );
    }

    public ItemStack create( int id, String label, int colour, ComputerFamily family, IPocketUpgrade upgrade )
    {
        // Ignore types we can't handle
        if( family != ComputerFamily.Normal && family != ComputerFamily.Advanced ) return null;

        // Build the stack
        int damage = family == ComputerFamily.Advanced ? 1 : 0;
        ItemStack result = new ItemStack( this, 1, damage );
        if( id >= 0 || upgrade != null )
        {
            NBTTagCompound compound = new NBTTagCompound();
            if( id >= 0 ) compound.setInteger( NBT_ID, id );
            if( upgrade != null ) compound.setString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
            result.setTagCompound( compound );
        }

        if( label != null ) result.setStackDisplayName( label );
        if( colour != -1 ) IColouredItem.setColourBasic( result, colour );

        return result;
    }

    @Override
    public void getSubItems( @Nonnull CreativeTabs tabs, @Nonnull NonNullList<ItemStack> list )
    {
        if( !isInCreativeTab( tabs ) ) return;
        getSubItems( list, ComputerFamily.Normal );
        getSubItems( list, ComputerFamily.Advanced );
    }

    private static void getSubItems( NonNullList<ItemStack> list, ComputerFamily family )
    {
        list.add( PocketComputerItemFactory.create( -1, null, -1, family, null ) );
        for( IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades() )
        {
            list.add( PocketComputerItemFactory.create( -1, null, -1, family, upgrade ) );
        }
    }

    @Override
    public void onUpdate( ItemStack stack, World world, Entity entity, int slotNum, boolean selected )
    {
        if( !world.isRemote )
        {
            // Server side
            IInventory inventory = entity instanceof EntityPlayer ? ((EntityPlayer) entity).inventory : null;
            PocketServerComputer computer = createServerComputer( world, inventory, entity, stack );
            if( computer != null )
            {
                IPocketUpgrade upgrade = getUpgrade( stack );

                // Ping computer
                computer.keepAlive();
                computer.setWorld( world );
                computer.updateValues( entity, stack, upgrade );

                // Sync ID
                int id = computer.getID();
                if( id != getComputerID( stack ) )
                {
                    setComputerID( stack, id );
                    if( inventory != null ) inventory.markDirty();
                }

                // Sync label
                String label = computer.getLabel();
                if( !Objects.equal( label, getLabel( stack ) ) )
                {
                    setLabel( stack, label );
                    if( inventory != null ) inventory.markDirty();
                }

                // Update pocket upgrade
                if( upgrade != null )
                {
                    upgrade.update( computer, computer.getPeripheral( ComputerSide.BACK ) );
                }
            }
        }
        else
        {
            // Client side
            createClientComputer( stack );
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick( World world, EntityPlayer player, @Nonnull EnumHand hand )
    {
        ItemStack stack = player.getHeldItem( hand );
        if( !world.isRemote )
        {
            PocketServerComputer computer = createServerComputer( world, player.inventory, player, stack );

            boolean stop = false;
            if( computer != null )
            {
                computer.turnOn();

                IPocketUpgrade upgrade = getUpgrade( stack );
                if( upgrade != null )
                {
                    computer.updateValues( player, stack, upgrade );
                    stop = upgrade.onRightClick( world, computer, computer.getPeripheral( ComputerSide.BACK ) );
                }
            }

            if( !stop ) Containers.openPocketComputerGUI( player, hand );
        }
        return new ActionResult<>( EnumActionResult.SUCCESS, stack );
    }

    @Nonnull
    @Override
    public String getUnlocalizedName( @Nonnull ItemStack stack )
    {
        switch( getFamily( stack ) )
        {
            case Normal:
            default:
                return "item.computercraft:pocket_computer";
            case Advanced:
                return "item.computercraft:advanced_pocket_computer";
        }
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName( @Nonnull ItemStack stack )
    {
        String baseString = getUnlocalizedName( stack );
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            return StringUtil.translateFormatted(
                baseString + ".upgraded.name",
                StringUtil.translate( upgrade.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return StringUtil.translate( baseString + ".name" );
        }
    }


    @Override
    public void addInformation( @Nonnull ItemStack stack, World world, List<String> list, ITooltipFlag flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getComputerID( stack );
            if( id >= 0 ) list.add( StringUtil.translateFormatted( "gui.computercraft.tooltip.computer_id", id ) );
        }
    }

    @Nullable
    @Override
    public String getCreatorModId( ItemStack stack )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
            // belongs to.
            String mod = PocketUpgrades.getOwner( upgrade );
            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
        }

        return super.getCreatorModId( stack );
    }

    private PocketServerComputer createServerComputer( final World world, IInventory inventory, Entity entity, @Nonnull ItemStack stack )
    {
        if( world.isRemote ) return null;

        PocketServerComputer computer;
        int instanceID = getInstanceID( stack );
        int sessionID = getSessionID( stack );
        int correctSessionID = ComputerCraft.serverComputerRegistry.getSessionID();

        if( instanceID >= 0 && sessionID == correctSessionID &&
            ComputerCraft.serverComputerRegistry.contains( instanceID ) )
        {
            computer = (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID );
        }
        else
        {
            if( instanceID < 0 || sessionID != correctSessionID )
            {
                instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                setInstanceID( stack, instanceID );
                setSessionID( stack, correctSessionID );
            }
            int computerID = getComputerID( stack );
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer" );
                setComputerID( stack, computerID );
            }
            computer = new PocketServerComputer(
                world,
                computerID,
                getLabel( stack ),
                instanceID,
                getFamily( stack )
            );
            computer.updateValues( entity, stack, getUpgrade( stack ) );
            computer.addAPI( new PocketAPI( computer ) );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            if( inventory != null ) inventory.markDirty();
        }
        computer.setWorld( world );
        return computer;
    }

    public static ServerComputer getServerComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        return instanceID >= 0 ? ComputerCraft.serverComputerRegistry.get( instanceID ) : null;
    }

    public static ClientComputer createClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        if( instanceID >= 0 )
        {
            if( !ComputerCraft.clientComputerRegistry.contains( instanceID ) )
            {
                ComputerCraft.clientComputerRegistry.add( instanceID, new ClientComputer( instanceID ) );
            }
            return ComputerCraft.clientComputerRegistry.get( instanceID );
        }
        return null;
    }

    private static ClientComputer getClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        return instanceID >= 0 ? ComputerCraft.clientComputerRegistry.get( instanceID ) : null;
    }

    // IComputerItem implementation

    private static void setComputerID( @Nonnull ItemStack stack, int computerID )
    {
        if( !stack.hasTagCompound() ) stack.setTagCompound( new NBTTagCompound() );
        stack.getTagCompound().setInteger( NBT_ID, computerID );
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public ComputerFamily getFamily( @Nonnull ItemStack stack )
    {
        int damage = stack.getItemDamage();
        switch( damage )
        {
            case 0:
            default:
                return ComputerFamily.Normal;
            case 1:
                return ComputerFamily.Advanced;
        }
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return PocketComputerItemFactory.create(
            getComputerID( stack ), getLabel( stack ), getColour( stack ),
            family, getUpgrade( stack )
        );
    }

    // IMedia

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setStackDisplayName( label );
        }
        else
        {
            stack.clearCustomName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
        }
        return null;
    }

    private static int getInstanceID( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( NBT_INSTANCE ) ? nbt.getInteger( NBT_INSTANCE ) : -1;
    }

    private static void setInstanceID( @Nonnull ItemStack stack, int instanceID )
    {
        if( !stack.hasTagCompound() ) stack.setTagCompound( new NBTTagCompound() );
        stack.getTagCompound().setInteger( NBT_INSTANCE, instanceID );
    }

    private static int getSessionID( @Nonnull ItemStack stack )
    {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey( NBT_SESSION ) ? nbt.getInteger( NBT_SESSION ) : -1;
    }

    private static void setSessionID( @Nonnull ItemStack stack, int sessionID )
    {
        if( !stack.hasTagCompound() ) stack.setTagCompound( new NBTTagCompound() );
        stack.getTagCompound().setInteger( NBT_SESSION, sessionID );
    }

    @SideOnly( Side.CLIENT )
    public static ComputerState getState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        return computer == null ? ComputerState.Off : computer.getState();
    }

    @SideOnly( Side.CLIENT )
    public static int getLightState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        if( computer != null && computer.isOn() )
        {
            NBTTagCompound computerNBT = computer.getUserData();
            if( computerNBT != null && computerNBT.hasKey( NBT_LIGHT, Constants.NBT.TAG_ANY_NUMERIC ) )
            {
                return computerNBT.getInteger( NBT_LIGHT );
            }
        }
        return -1;
    }

    public IPocketUpgrade getUpgrade( @Nonnull ItemStack stack )
    {
        NBTTagCompound compound = stack.getTagCompound();
        if( compound != null )
        {
            if( compound.hasKey( NBT_UPGRADE, Constants.NBT.TAG_STRING ) )
            {
                String name = compound.getString( NBT_UPGRADE );
                return PocketUpgrades.get( name );
            }
            else if( compound.hasKey( NBT_UPGRADE, Constants.NBT.TAG_ANY_NUMERIC ) )
            {
                int id = compound.getInteger( NBT_UPGRADE );
                if( id == 1 ) return ComputerCraft.PocketUpgrades.wirelessModem;
            }
        }

        return null;
    }

    public static void setUpgrade( @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        NBTTagCompound compound = stack.getTagCompound();
        if( compound == null ) stack.setTagCompound( compound = new NBTTagCompound() );

        if( upgrade == null )
        {
            compound.removeTag( NBT_UPGRADE );
        }
        else
        {
            compound.setString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        }

        compound.removeTag( NBT_UPGRADE_INFO );
    }

    public static NBTTagCompound getUpgradeInfo( @Nonnull ItemStack stack )
    {
        NBTTagCompound tag = stack.getTagCompound();
        if( tag == null )
        {
            tag = new NBTTagCompound();
            stack.setTagCompound( tag );
        }

        if( tag.hasKey( NBT_UPGRADE_INFO, Constants.NBT.TAG_COMPOUND ) )
        {
            return tag.getCompoundTag( NBT_UPGRADE_INFO );
        }
        else
        {
            NBTTagCompound sub = new NBTTagCompound();
            tag.setTag( NBT_UPGRADE_INFO, sub );
            return sub;
        }
    }

    private static final IItemPropertyGetter COMPUTER_STATE = ( stack, world, player ) -> getState( stack ).ordinal();

    private static final IItemPropertyGetter COMPUTER_COLOURED = ( stack, world, player ) ->
        IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0;
}
