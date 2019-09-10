package org.janelia.saalfeldlab.process;

import java.io.File;
import java.util.concurrent.Callable;

import ij.ImagePlus;
import io.IOHelper;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;

import net.imglib2.util.*;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command( version = "0.0.1-SNAPSHOT" )
public class PreprocessN5ForRegistration implements Callable<Void>
{

	@Option( names = { "-o", "--output" }, required = true, description = "Output file" )
	private File outputFile;
	
	@Option( names = { "-n", "--n5-root" }, required = true, description = "N5 root directory" )
	private File n5Base;

	@Option( names = { "-i", "--input" }, required = true, description = "Input dataset." )
	private String inDataset;

	@Option( names = { "-m", "--mask" }, required = false, description = "Dataset to use as mask." )
	private String maskDataset;

	@Option( names = { "--mask-threshold" }, required = false, description = "Threshold for the mask" )
	private double maskThreshold = 50;

	@Option( names = { "-r", "--intensityrange" }, required = false, description = "Intensity range for input." )
	private double[] inIntensityRange = new double[]{ 120, 134 };

	
	private double min;
	private double max;
	private double slope;

	public static void main( String[] args )
	{
		CommandLine.call( new PreprocessN5ForRegistration(), args );
		System.exit(0);
	}

	@SuppressWarnings( "unchecked" )
	public <T extends RealType<T> & NativeType<T>, S extends RealType<S>> Void run() throws Exception
	{
		N5FSReader n5 = new N5FSReader( n5Base.getAbsolutePath() );
		RandomAccessibleInterval<T> img = (RandomAccessibleInterval<T>)N5Utils.open( n5, inDataset );
		RandomAccessibleInterval<S> mask = (RandomAccessibleInterval<S>)N5Utils.open( n5, maskDataset );
		
		// make the mask
		Converter< S, UnsignedByteType > conv = binaryThresholdConverter( Util.getTypeFromInterval( mask ), new UnsignedByteType(), maskThreshold );
		RandomAccessibleInterval< UnsignedByteType > maskConv = Converters.convert( mask, conv, new UnsignedByteType() );
		
		// scale intensities
		RandomAccessibleInterval< FloatType > imgScaled = Converters.convert( 
				img, linearIntensityRangeConverter( Util.getTypeFromInterval( img ) ), new FloatType());

		// allocate output
		ImagePlusImgFactory< FloatType > factory = new ImagePlusImgFactory<>( new FloatType() );
		ImagePlusImg< FloatType, ? > outimg = factory.create( img );
		ImagePlus ip = outimg.getImagePlus();
		
		// create output
		System.out.println( "" );
		LoopBuilder.setImages( outimg, imgScaled, maskConv )
			.forEachPixel( ( o, i, m ) -> { o.setReal( m.getRealDouble() * i.getRealDouble() ); } );

		IOHelper.write( ip, outputFile );

		return null;
	}

	public void setup()
	{
		min = inIntensityRange[0];
		max = inIntensityRange[1];
		slope = (max - min) / min;
	}

	@Override
	public Void call() throws Exception
	{
		setup();
		run();
		return null;
	}

	public <T extends RealType<T>> Converter<T,FloatType> linearIntensityRangeConverter( final T t )
	{
		return new Converter< T, FloatType >()
		{
			@Override
			public void convert( T input, FloatType output )
			{
				double val = input.getRealDouble();
				if( val <= min )
					output.setZero();
				else if ( val >= max )
					output.setOne();
				else
				{
					output.setReal( ( val - min ) * slope );
				}
			}
		};
	}

	public static <T extends RealType<T>, S extends RealType<S>> Converter<S,T> binaryThresholdConverter( 
			final S s, final T t, final double threshold )
	{
		return new Converter< S, T >()
		{
			@Override
			public void convert( S input, T output )
			{
				if( input.getRealDouble() < threshold )
					output.setOne();
				else
					output.setZero();
			}
		};
	}
}
