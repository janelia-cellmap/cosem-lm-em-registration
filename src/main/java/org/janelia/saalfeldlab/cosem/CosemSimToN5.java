package org.janelia.saalfeldlab.cosem;

import java.util.concurrent.Callable;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
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

	@Option( names = { "-b", "--block-size" }, required = false, description = "Block size", split = "," )
	private int[] blockSize = new int[]{ 64, 64, 64 };

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

	public <T extends NativeType<T>> void run() throws Exception
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

		Img<T> img = (Img<T>)ImageJFunctions.wrap( imp );

		N5Writer n5 = new N5FSWriter( n5Base );
		GzipCompression compression = new GzipCompression();
		
		// channels in the 3rd dimension
		for( int i = 0; i < img.dimension( 2 ); i++ )
		{
			IntervalView<T> channelImg = Views.hyperSlice( img, 2, i );

			System.out.println( "sz: " + Util.printInterval( channelImg ));

			String dataset = String.format("c%d", i);
			System.out.println( "dataset: " + dataset );
			
			N5Utils.save( channelImg, n5, dataset, blockSize, compression );
			n5.setAttribute( dataset, CosemResolution.key, res );
		}
		System.out.println("finished");
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
