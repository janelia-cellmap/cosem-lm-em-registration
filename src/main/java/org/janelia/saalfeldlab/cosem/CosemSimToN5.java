package org.janelia.saalfeldlab.cosem;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class CosemSimToN5 implements Callable<Void>
{

	@Option( names = { "-i", "--image" }, required = true, description = "Input image." )
	private String inputImage;

	@Option( names = { "-n", "--n5" }, required = true, description = "N5 base folder." )
	private String n5Base;

	@Option( names = { "-d", "--dataset" }, required = false, description = "Base dataset for raw data." )
	private String datasetBase = "/volumes/raw";

	@Option( names = { "-b", "--block-size" }, required = false, description = "Block size", split = "," )
	private int[] blockSize = new int[]{ 64, 64, 64 };

	@Option( names = { "--skip-iso" }, required = false, description = "Skip isotropic"  )
	private boolean skipIso = false;

	@Option( names = { "-s", "--iso-dataset" }, required = false, description = "Base dataset for isotropic data." )
	private String isoDatasetBase = "/volumes/proc";

	public static void main(String[] args)
	{
		CommandLine.call( new CosemSimToN5(), args );	
	}

	@Override
	public Void call() throws Exception
	{
		run();
		return null;
	}

	public <T extends RealType<T> & NativeType<T>> void run() throws Exception
	{

		ImagePlus imp = IJ.openImage( inputImage );
		// get calibration and image size
		double pw = imp.getCalibration().pixelWidth;
		double ph = imp.getCalibration().pixelHeight;
		double pd = imp.getCalibration().pixelDepth;
		String punit = imp.getCalibration().getUnit();
		if ( punit == null || punit.isEmpty() )
			punit = "px";
		else if ( punit.startsWith("micron") || punit.equals("um" ))
		{
			pw *= 1000;
			ph *= 1000;
			pd *= 1000;
			punit = "nm";
		}
		
		CosemResolution res = new CosemResolution( new double[]{ pw, ph, pd }, punit );

		@SuppressWarnings("unchecked")
		Img<T> img = (Img<T>)ImageJFunctions.wrap( imp );

		N5Writer n5 = new N5FSWriter( n5Base );
		GzipCompression compression = new GzipCompression();
		
		// channels in the 3rd dimension
		for( int i = 0; i < img.dimension( 2 ); i++ )
		{
			IntervalView<T> channelImg = Views.hyperSlice( img, 2, i );

			String dataset = datasetBase + "/" + String.format( "c%d", i );
			N5Utils.save( channelImg, n5, dataset, blockSize, compression );
			n5.setAttribute( dataset, CosemResolution.key, res );
			
			if( !skipIso )
			{
				double[] newres = isoRes( res.dimension );
				CosemResolution isoRes = new CosemResolution( newres, punit );
				RandomAccessibleInterval<T> isoImg = toIsotropic( channelImg, res.dimension, newres );

				String isoDataset = isoDatasetBase + "/" + String.format( "c%d", i );
				N5Utils.save( isoImg, n5, isoDataset, blockSize, compression );
				n5.setAttribute( isoDataset, CosemResolution.key, isoRes );
			}
		}
		System.out.println("finished");
	}

	public static double[] isoRes( final double[] res )
	{
		double bestRes = Double.MAX_VALUE;
		for( int i = 0; i < res.length; i++ )
		{
			if( res[ i ] < bestRes )
				bestRes = res[ i ];
		}
		double[] isores = new double[ res.length ];
		Arrays.fill( isores, bestRes );
		return isores;
	}

	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> toIsotropic(
			final RandomAccessibleInterval<T> img,
			final double[] oldres,
			final double[] newres )
	{
		double[] factors = new double[ img.numDimensions() ];
		long[] newSz = new long[ img.numDimensions() ];
		for( int i = 0; i < oldres.length; i++ )
		{
			factors[ i ] = oldres[ i ] / newres[ i ];
			newSz[ i ] = (long)Math.ceil( ((double)img.dimension( i )) * factors[ i ] );
		}
		Scale3D scale = new Scale3D( factors );

		return Views.interval( Views.raster( 
					RealViews.affine(
						Views.interpolate( Views.extendZero( img ), new NLinearInterpolatorFactory<>()),
						scale )),
					new FinalInterval( newSz ));
	}

	
	public static class CosemResolution 
	{
		public static String key = "pixelResolution";
		public final double[] dimension;
		public final String unit;
		
		public CosemResolution( final double[] dimension, final String unit )
		{
			this.dimension = dimension;
			this.unit = unit;
		}
	}
	
}
