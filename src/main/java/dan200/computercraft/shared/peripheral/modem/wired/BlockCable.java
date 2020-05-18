/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class BlockCable extends BlockGeneric
{
    public static final PropertyEnum<BlockCableModemVariant> MODEM = PropertyEnum.create( "modem", BlockCableModemVariant.class );
    static final PropertyBool CABLE = PropertyBool.create( "cable" );
    static final PropertyBool NORTH = PropertyBool.create( "north" );
    static final PropertyBool SOUTH = PropertyBool.create( "south" );
    static final PropertyBool EAST = PropertyBool.create( "east" );
    static final PropertyBool WEST = PropertyBool.create( "west" );
    static final PropertyBool UP = PropertyBool.create( "up" );
    static final PropertyBool DOWN = PropertyBool.create( "down" );
    static final EnumMap<EnumFacing, PropertyBool> CONNECTIONS =
        new EnumMap<>( new ImmutableMap.Builder<EnumFacing, PropertyBool>()
            .put( EnumFacing.DOWN, DOWN ).put( EnumFacing.UP, UP )
            .put( EnumFacing.NORTH, NORTH ).put( EnumFacing.SOUTH, SOUTH )
            .put( EnumFacing.WEST, WEST ).put( EnumFacing.EAST, EAST )
            .build() );

    public BlockCable()
    {
        super( Material.ROCK );
        setHardness( 1.5f );
        setUnlocalizedName( "computercraft:cable" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( MODEM, BlockCableModemVariant.None )
            .withProperty( CABLE, false )
            .withProperty( NORTH, false )
            .withProperty( SOUTH, false )
            .withProperty( EAST, false )
            .withProperty( WEST, false )
            .withProperty( UP, false )
            .withProperty( DOWN, false )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this,
            MODEM,
            CABLE,
            NORTH,
            SOUTH,
            EAST,
            WEST,
            UP,
            DOWN
        );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        IBlockState state = getDefaultState();
        if( meta < 6 )
        {
            state = state.withProperty( CABLE, false );
            state = state.withProperty( MODEM, BlockCableModemVariant.from( EnumFacing.getFront( meta ) ) );
        }
        else if( meta < 12 )
        {
            state = state.withProperty( CABLE, true );
            state = state.withProperty( MODEM, BlockCableModemVariant.from( EnumFacing.getFront( meta - 6 ) ) );
        }
        else if( meta == 13 )
        {
            state = state.withProperty( CABLE, true );
            state = state.withProperty( MODEM, BlockCableModemVariant.None );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = 0;
        boolean cable = state.getValue( CABLE );
        BlockCableModemVariant modem = state.getValue( MODEM );
        if( cable && modem != BlockCableModemVariant.None )
        {
            meta = 6 + modem.getFacing().getIndex();
        }
        else if( modem != BlockCableModemVariant.None )
        {
            meta = modem.getFacing().getIndex();
        }
        else if( cable )
        {
            meta = 13;
        }
        return meta;
    }


    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing placedSide, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        switch( ComputerCraft.Items.cable.getPeripheralType( damage ) )
        {
            case Cable:
                return getDefaultState()
                    .withProperty( CABLE, true )
                    .withProperty( MODEM, BlockCableModemVariant.None );
            default:
            case WiredModem:
                return getDefaultState()
                    .withProperty( CABLE, false )
                    .withProperty( MODEM, BlockCableModemVariant.from( placedSide.getOpposite() ) );
            case WiredModemWithCable:
                return getDefaultState()
                    .withProperty( CABLE, true )
                    .withProperty( MODEM, BlockCableModemVariant.from( placedSide.getOpposite() ) );
        }
    }

    public static boolean canConnectIn( IBlockState state, EnumFacing direction )
    {
        return state.getValue( CABLE )
            && state.getValue( MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction )
    {
        if( !state.getValue( CABLE ) ) return false;
        if( state.getValue( MODEM ).getFacing() == direction ) return true;
        return ComputerCraftAPI.getWiredElementAt( world, pos.offset( direction ), direction.getOpposite() ) != null;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        state = state
            .withProperty( NORTH, doesConnectVisually( state, world, pos, EnumFacing.NORTH ) )
            .withProperty( SOUTH, doesConnectVisually( state, world, pos, EnumFacing.SOUTH ) )
            .withProperty( EAST, doesConnectVisually( state, world, pos, EnumFacing.EAST ) )
            .withProperty( WEST, doesConnectVisually( state, world, pos, EnumFacing.WEST ) )
            .withProperty( UP, doesConnectVisually( state, world, pos, EnumFacing.UP ) )
            .withProperty( DOWN, doesConnectVisually( state, world, pos, EnumFacing.DOWN ) );

        TileEntity tile = world.getTileEntity( pos );
        int anim = tile instanceof TileCable ? ((TileCable) tile).getState() : 0;

        BlockCableModemVariant modem = state.getValue( MODEM );
        if( modem != BlockCableModemVariant.None ) modem = BlockCableModemVariant.from( modem.getFacing(), anim );
        state = state.withProperty( MODEM, modem );

        return state;
    }

    @Override
    @Deprecated
    public boolean shouldSideBeRendered( IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side )
    {
        return true;
    }

    public static PeripheralType getPeripheralType( IBlockState state )
    {
        boolean cable = state.getValue( CABLE );
        BlockCableModemVariant modem = state.getValue( MODEM );
        if( cable && modem != BlockCableModemVariant.None )
        {
            return PeripheralType.WiredModemWithCable;
        }
        else if( modem != BlockCableModemVariant.None )
        {
            return PeripheralType.WiredModem;
        }
        else
        {
            return PeripheralType.Cable;
        }
    }

    @Override
    protected TileGeneric createTile( IBlockState state )
    {
        return new TileCable();
    }

    @Override
    protected TileGeneric createTile( int damage )
    {
        return new TileCable();
    }

    @Nonnull
    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos )
    {
        return CableBounds.getBounds( state.getActualState( source, pos ) );
    }

    @Override
    @Deprecated
    public void addCollisionBoxToList( IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB bigBox, @Nonnull List<AxisAlignedBB> list, Entity entity, boolean isActualState )
    {
        if( !isActualState ) state = state.getActualState( world, pos );

        // Get collision bounds
        List<AxisAlignedBB> collision = new ArrayList<>( 1 );
        CableBounds.getBounds( state, collision );
        for( AxisAlignedBB localBounds : collision ) addCollisionBoxToList( pos, bigBox, list, localBounds );
    }

    @Nullable
    @Override
    @Deprecated
    public RayTraceResult collisionRayTrace( IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end )
    {
        double distance = Double.POSITIVE_INFINITY;
        RayTraceResult result = null;

        List<AxisAlignedBB> bounds = new ArrayList<>( 7 );
        CableBounds.getBounds( state.getActualState( world, pos ), bounds );

        Vec3d startOff = start.subtract( pos.getX(), pos.getY(), pos.getZ() );
        Vec3d endOff = end.subtract( pos.getX(), pos.getY(), pos.getZ() );

        for( AxisAlignedBB bb : bounds )
        {
            RayTraceResult hit = bb.calculateIntercept( startOff, endOff );
            if( hit != null )
            {
                double newDistance = hit.hitVec.squareDistanceTo( startOff );
                if( newDistance <= distance )
                {
                    distance = newDistance;
                    result = hit;
                }
            }
        }

        return result == null ? null : new RayTraceResult( result.hitVec.addVector( pos.getX(), pos.getY(), pos.getZ() ), result.sideHit, pos );
    }

    @Override
    public boolean removedByPlayer( @Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest )
    {
        PeripheralType type = getPeripheralType( state );
        if( type == PeripheralType.WiredModemWithCable )
        {
            RayTraceResult hit = state.collisionRayTrace( world, pos, WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ) );
            if( hit != null )
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileCable && tile.hasWorld() )
                {
                    TileCable cable = (TileCable) tile;

                    ItemStack item;
                    IBlockState newState;

                    AxisAlignedBB bb = CableBounds.getModemBounds( state );
                    if( WorldUtil.isVecInsideInclusive( bb, hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        newState = state.withProperty( MODEM, BlockCableModemVariant.None );
                        item = PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 );
                    }
                    else
                    {
                        newState = state.withProperty( CABLE, false );
                        item = PeripheralItemFactory.create( PeripheralType.Cable, null, 1 );
                    }

                    world.setBlockState( pos, newState, 3 );

                    cable.modemChanged();
                    cable.connectionsChanged();
                    if( !world.isRemote && !player.capabilities.isCreativeMode )
                    {
                        Block.spawnAsEntity( world, pos, item );
                    }

                    return false;
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }

    @Override
    public void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        PeripheralType type = getPeripheralType( state );
        switch( type )
        {
            case Cable:
            case WiredModem:
                drops.add( PeripheralItemFactory.create( type, null, 1 ) );
                break;
            case WiredModemWithCable:
                drops.add( PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 ) );
                drops.add( PeripheralItemFactory.create( PeripheralType.Cable, null, 1 ) );
                break;
        }
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult hit, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        PeripheralType type = getPeripheralType( state );
        if( type == PeripheralType.WiredModemWithCable )
        {
            type = hit == null || WorldUtil.isVecInsideInclusive( CableBounds.getModemBounds( state ), hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) )
                ? PeripheralType.WiredModem : PeripheralType.Cable;
        }

        return PeripheralItemFactory.create( type, null, 1 );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            if( cable.getPeripheralType() != PeripheralType.WiredModem )
            {
                cable.connectionsChanged();
            }
        }

        super.onBlockPlacedBy( world, pos, state, placer, stack );
    }

    @Override
    @Deprecated
    public final boolean isOpaqueCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @Deprecated
    public boolean hasCustomBreakingProgress( IBlockState state )
    {
        return true;
    }
}
