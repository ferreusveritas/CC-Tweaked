/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.BlockComputerBase;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockTurtle extends BlockComputerBase
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockTurtle()
    {
        super( Material.IRON );
        setHardness( 2.5f );
        setUnlocalizedName( "computercraft:turtle" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
        );
    }

    @Nonnull
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType( IBlockState state )
    {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public boolean isFullCube( IBlockState state )
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

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        return state.withProperty( FACING, getDirection( world, pos ) );
    }

    @Override
    protected IBlockState getDefaultBlockState( ComputerFamily family, EnumFacing placedSide )
    {
        return getDefaultState();
    }

    @Nonnull
    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        Vec3d offset = tile instanceof TileTurtle ? ((TileTurtle) tile).getRenderOffset( 1.0f ) : Vec3d.ZERO;
        return new AxisAlignedBB(
            offset.x + 0.125, offset.y + 0.125, offset.z + 0.125,
            offset.x + 0.875, offset.y + 0.875, offset.z + 0.875
        );
    }

    private ComputerFamily getFamily()
    {
        if( this == ComputerCraft.Blocks.turtleAdvanced )
        {
            return ComputerFamily.Advanced;
        }
        else
        {
            return ComputerFamily.Normal;
        }
    }

    @Override
    public ComputerFamily getFamily( int damage )
    {
        return getFamily();
    }

    @Override
    public ComputerFamily getFamily( IBlockState state )
    {
        return getFamily();
    }

    @Override
    protected TileComputerBase createTile( ComputerFamily family )
    {
        if( this == ComputerCraft.Blocks.turtleAdvanced )
        {
            return new TileTurtleAdvanced();
        }
        else if( this == ComputerCraft.Blocks.turtleExpanded )
        {
            return new TileTurtleExpanded();
        }
        else
        {
            return new TileTurtle();
        }
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        super.onBlockPlacedBy( world, pos, state, player, stack );

        TileEntity tile = world.getTileEntity( pos );
        if( !world.isRemote && tile instanceof TileTurtle )
        {
            TileTurtle turtle = (TileTurtle) tile;

            if( player instanceof EntityPlayer )
            {
                ((TileTurtle) tile).setOwningPlayer( ((EntityPlayer) player).getGameProfile() );
            }

            if( stack.getItem() instanceof ITurtleItem )
            {
                ITurtleItem item = (ITurtleItem) stack.getItem();

                // Set Upgrades
                for( TurtleSide side : TurtleSide.values() )
                {
                    turtle.getAccess().setUpgrade( side, item.getUpgrade( stack, side ) );
                }

                turtle.getAccess().setFuelLevel( item.getFuelLevel( stack ) );

                // Set colour
                int colour = item.getColour( stack );
                if( colour != -1 ) turtle.getAccess().setColour( colour );

                // Set overlay
                ResourceLocation overlay = item.getOverlay( stack );
                if( overlay != null ) ((TurtleBrain) turtle.getAccess()).setOverlay( overlay );
            }
        }

        // Set direction
        EnumFacing dir = DirectionUtil.fromEntityRot( player );
        setDirection( world, pos, dir.getOpposite() );
    }

    @Override
    @Deprecated
    public float getExplosionResistance( Entity exploder )
    {
        if( getFamily() == ComputerFamily.Advanced || exploder instanceof EntityLivingBase || exploder instanceof EntityFireball )
        {
            return 2000;
        }

        return super.getExplosionResistance( exploder );
    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileTurtle ? TurtleItemFactory.create( (TileTurtle) tile ) : ItemStack.EMPTY;
    }
}
