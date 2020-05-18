/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.ModemBounds;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class BlockPeripheral extends BlockGeneric
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<BlockPeripheralVariant> VARIANT = PropertyEnum.create( "variant", BlockPeripheralVariant.class );

    public BlockPeripheral()
    {
        super( Material.ROCK );
        setHardness( 2.0f );
        setUnlocalizedName( "computercraft:peripheral" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( VARIANT, BlockPeripheralVariant.DiskDriveEmpty )
        );
    }

    @Override
    @Nonnull
    @SideOnly( Side.CLIENT )
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }
    
    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, VARIANT );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        IBlockState state = getDefaultState();
        if( meta >= 2 && meta <= 5 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.DiskDriveEmpty );
            state = state.withProperty( FACING, EnumFacing.getFront( meta ) );
        }
        else if( meta <= 9 )
        {
            if( meta == 0 )
            {
                state = state.withProperty( VARIANT, BlockPeripheralVariant.WirelessModemDownOff );
                state = state.withProperty( FACING, EnumFacing.NORTH );
            }
            else if( meta == 1 )
            {
                state = state.withProperty( VARIANT, BlockPeripheralVariant.WirelessModemUpOff );
                state = state.withProperty( FACING, EnumFacing.NORTH );
            }
            else
            {
                state = state.withProperty( VARIANT, BlockPeripheralVariant.WirelessModemOff );
                state = state.withProperty( FACING, EnumFacing.getFront( meta - 4 ) );
            }
        }
        else if( meta == 10 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.Monitor );
        }
        else if( meta == 11 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
        }
        else if( meta == 12 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.AdvancedMonitor );
        }
        else if( meta == 13 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.Speaker );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = 0;
        BlockPeripheralVariant variant = state.getValue( VARIANT );
        switch( variant.getPeripheralType() )
        {
            case DiskDrive:
            {
                EnumFacing dir = state.getValue( FACING );
                if( dir.getAxis() == EnumFacing.Axis.Y )
                {
                    dir = EnumFacing.NORTH;
                }
                meta = dir.getIndex();
                break;
            }
            case WirelessModem:
                switch( variant )
                {
                    case WirelessModemDownOff:
                    case WirelessModemDownOn:
                        meta = 0;
                        break;
                    case WirelessModemUpOff:
                    case WirelessModemUpOn:
                        meta = 1;
                        break;
                    default:
                    {
                        EnumFacing dir = state.getValue( FACING );
                        meta = dir.getIndex() + 4;
                        break;
                    }
                }
                break;
            case Monitor:
                meta = 10;
                break;
            case Printer:
                meta = 11;
                break;
            case AdvancedMonitor:
                meta = 12;
                break;
            case Speaker:
                meta = 13;
                break;
        }
        return meta;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity tile = world.getTileEntity( pos );
        PeripheralType type = getPeripheralType( state );
        switch( type )
        {
            case DiskDrive:
            {
                if( !(tile instanceof TileDiskDrive) ) return state;

                TileDiskDrive drive = (TileDiskDrive) tile;
                state = state.withProperty( FACING, drive.getDirection() );
                switch( drive.getAnim() )
                {
                    default:
                    case 0:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.DiskDriveEmpty );
                    case 1:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.DiskDriveInvalid );
                    case 2:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.DiskDriveFull );
                }
            }
            case Printer:
            {
                if( !(tile instanceof TilePrinter) ) return state;

                TilePrinter printer = (TilePrinter) tile;
                state = state.withProperty( FACING, printer.getDirection() );
                switch( printer.getAnim() )
                {
                    default:
                    case 0:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
                    case 1:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.PrinterTopFull );
                    case 2:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.PrinterBottomFull );
                    case 3:
                        return state.withProperty( VARIANT, BlockPeripheralVariant.PrinterBothFull );
                }
            }
            case WirelessModem:
            {
                if( !(tile instanceof TileWirelessModem) ) return state;

                TileWirelessModem modem = (TileWirelessModem) tile;
                EnumFacing direction = modem.getDirection();
                switch( direction )
                {
                    case UP:
                        return state
                            .withProperty( FACING, EnumFacing.NORTH )
                            .withProperty( VARIANT,
                                modem.isOn() ? BlockPeripheralVariant.WirelessModemUpOn : BlockPeripheralVariant.WirelessModemUpOff );
                    case DOWN:
                        return state
                            .withProperty( FACING, EnumFacing.NORTH )
                            .withProperty( VARIANT,
                                modem.isOn() ? BlockPeripheralVariant.WirelessModemDownOn : BlockPeripheralVariant.WirelessModemDownOff );
                    default:
                    {
                        return state
                            .withProperty( FACING, direction )
                            .withProperty( VARIANT,
                                modem.isOn() ? BlockPeripheralVariant.WirelessModemOn : BlockPeripheralVariant.WirelessModemOff );
                    }
                }
            }
            case Speaker:
            {
                if( !(tile instanceof TileSpeaker) ) return state;
                return state.withProperty( FACING, ((TileSpeaker) tile).getDirection() );
            }
            case Monitor:
            case AdvancedMonitor:
            {
                if( !(tile instanceof TileMonitor) ) return state;

                TileMonitor monitor = (TileMonitor) tile;
                EnumFacing dir = monitor.getDirection();
                EnumFacing front = monitor.getFront();
                int xIndex = monitor.getXIndex();
                int yIndex = monitor.getYIndex();
                int width = monitor.getWidth();
                int height = monitor.getHeight();

                BlockPeripheralVariant baseVariant;
                if( front == EnumFacing.UP )
                {
                    baseVariant = type == PeripheralType.AdvancedMonitor ?
                        BlockPeripheralVariant.AdvancedMonitorUp :
                        BlockPeripheralVariant.MonitorUp;
                }
                else if( front == EnumFacing.DOWN )
                {
                    baseVariant = type == PeripheralType.AdvancedMonitor ?
                        BlockPeripheralVariant.AdvancedMonitorDown :
                        BlockPeripheralVariant.MonitorDown;
                }
                else
                {
                    baseVariant = type == PeripheralType.AdvancedMonitor ?
                        BlockPeripheralVariant.AdvancedMonitor :
                        BlockPeripheralVariant.Monitor;
                }

                int subType;
                if( width == 1 && height == 1 )
                {
                    subType = 0;
                }
                else if( height == 1 )
                {
                    if( xIndex == 0 )
                    {
                        subType = 1;
                    }
                    else if( xIndex == width - 1 )
                    {
                        subType = 3;
                    }
                    else
                    {
                        subType = 2;
                    }
                }
                else if( width == 1 )
                {
                    if( yIndex == 0 )
                    {
                        subType = 6;
                    }
                    else if( yIndex == height - 1 )
                    {
                        subType = 4;
                    }
                    else
                    {
                        subType = 5;
                    }
                }
                else
                {
                    if( xIndex == 0 )
                    {
                        subType = 7;
                    }
                    else if( xIndex == width - 1 )
                    {
                        subType = 9;
                    }
                    else
                    {
                        subType = 8;
                    }
                    if( yIndex == 0 )
                    {
                        subType += 6;
                    }
                    else if( yIndex < height - 1 )
                    {
                        subType += 3;
                    }
                }

                return state
                    .withProperty( FACING, dir )
                    .withProperty( VARIANT, BlockPeripheralVariant.values()[baseVariant.ordinal() + subType] );
            }
            default:
                return state;
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public final IBlockState getStateForPlacement( World world, BlockPos pos, EnumFacing placedSide, float hitX, float hitY, float hitZ, int damage, EntityLivingBase placer )
    {
        switch( getPeripheralType( damage ) )
        {
            case DiskDrive:
            default:
                return getDefaultState()
                    .withProperty( VARIANT, BlockPeripheralVariant.DiskDriveEmpty )
                    .withProperty( FACING, placedSide.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : placedSide );
            case WirelessModem:
            {
                EnumFacing dir = placedSide.getOpposite();
                if( dir == EnumFacing.DOWN )
                {
                    return getDefaultState()
                        .withProperty( VARIANT, BlockPeripheralVariant.WirelessModemDownOff )
                        .withProperty( FACING, EnumFacing.NORTH );
                }
                else if( dir == EnumFacing.UP )
                {
                    return getDefaultState()
                        .withProperty( VARIANT, BlockPeripheralVariant.WirelessModemUpOff )
                        .withProperty( FACING, EnumFacing.NORTH );
                }
                else
                {
                    return getDefaultState()
                        .withProperty( VARIANT, BlockPeripheralVariant.WirelessModemOff )
                        .withProperty( FACING, dir );
                }
            }
            case Monitor:
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.Monitor );
            case Printer:
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
            case AdvancedMonitor:
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.AdvancedMonitor );
            case Speaker:
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.Speaker );
        }
    }

    public PeripheralType getPeripheralType( int damage )
    {
        return ComputerCraft.Items.peripheral.getPeripheralType( damage );
    }

    public static PeripheralType getPeripheralType( IBlockState state )
    {
        return state.getValue( VARIANT ).getPeripheralType();
    }

    private TileGeneric createTile( PeripheralType type )
    {
        switch( type )
        {
            case DiskDrive:
            default:
                return new TileDiskDrive();
            case WirelessModem:
                return new TileWirelessModem();
            case Monitor:
            case AdvancedMonitor:
                return new TileMonitor();
            case Printer:
                return new TilePrinter();
            case Speaker:
                return new TileSpeaker();
        }
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        TileEntity tile = world.getTileEntity( pos );
        switch( getPeripheralType( state ) )
        {
            case Speaker:
            case DiskDrive:
            case Printer:
                if( tile instanceof TilePeripheralBase )
                {
                    TilePeripheralBase peripheral = (TilePeripheralBase) tile;
                    peripheral.setDirection( DirectionUtil.fromEntityRot( player ) );
                    if( stack.hasDisplayName() ) peripheral.setLabel( stack.getDisplayName() );
                }
                break;
            case Monitor:
            case AdvancedMonitor:
                if( tile instanceof TileMonitor )
                {
                    int direction = DirectionUtil.fromEntityRot( player ).getIndex();
                    if( player.rotationPitch > 66.5F )
                    {
                        direction += 12;
                    }
                    else if( player.rotationPitch < -66.5F )
                    {
                        direction += 6;
                    }

                    TileMonitor monitor = (TileMonitor) tile;
                    if( world.isRemote )
                    {
                        monitor.setDir( direction );
                    }
                    else
                    {
                        monitor.contractNeighbours();
                        monitor.setDir( direction );
                        monitor.contract();
                        monitor.expand();
                    }
                }
                break;
        }
    }

    @Override
    @Deprecated
    public final boolean isOpaqueCube( IBlockState state )
    {
        PeripheralType type = getPeripheralType( state );
        return type == PeripheralType.DiskDrive || type == PeripheralType.Printer
            || type == PeripheralType.Monitor || type == PeripheralType.AdvancedMonitor
            || type == PeripheralType.Speaker;
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return isOpaqueCube( state );
    }

    @Override
    @Deprecated
    public boolean isFullBlock( IBlockState state )
    {
        return isOpaqueCube( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return isOpaqueCube( state ) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @Deprecated
    public boolean causesSuffocation( IBlockState state )
    {
        // This normally uses the default state
    	    	
        return blockMaterial.blocksMovement() && state.isOpaqueCube();
    }

    @Override
    @Deprecated
    public int getLightOpacity( IBlockState state )
    {
        // This normally uses the default state
        return isOpaqueCube( state ) ? 255 : 0;
    }

    @Override
    @Deprecated
    @Nonnull
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess world, BlockPos pos )
    {
        if( getPeripheralType( state ) == PeripheralType.WirelessModem )
        {
            TileEntity tile = world.getTileEntity( pos );
            if( tile instanceof TileWirelessModem )
            {
                return ModemBounds.getBounds( ((TileWirelessModem) tile).getDirection() );
            }
        }

        return super.getBoundingBox( state, world, pos );
    }

    @Override
    public final boolean canPlaceBlockOnSide( @Nonnull World world, @Nonnull BlockPos pos, EnumFacing side )
    {
        return true; // ItemPeripheralBase handles this
    }

    @Override
    public final TileGeneric createTile( IBlockState state )
    {
        return createTile( getPeripheralType( state ) );
    }

    @Override
    public final TileGeneric createTile( int damage )
    {
        return createTile( getPeripheralType( damage ) );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile instanceof ITilePeripheral
            ? PeripheralItemFactory.create( (ITilePeripheral) tile )
            : super.getPickBlock( state, target, world, pos, player );
    }

    @Override
    public void getDrops( @Nonnull NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, @Nonnull IBlockState state, int fortune )
    {
        drops.add( PeripheralItemFactory.create( getPeripheralType( state ), null, 1 ) );
    }
}
