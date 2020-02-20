#@ File (label="N5 root",  style="directory") n5Root
#@ String (label="dataset") dataset
#@ boolean (label="duplicate") duplicate

import  bdv.util.*;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;

import net.imglib2.util.*;
import net.imglib2.realtransform.*;

import mpicbg.spim.data.sequence.*;

import ij.plugin.Duplicator;

import net.imglib2.img.display.imagej.ImageJFunctions;

n5 = new N5FSReader( n5Root.getAbsolutePath());

// assume we're getting a pyramid level
// TODO relax this assumption

downFactors = n5.getAttribute( dataset, "downsamplingFactors", double[].class );

datasetS0 = dataset.replaceAll( "s[0-9]+\$", "s0" );


baseResolution = n5.getAttribute( datasetS0, "resolution", double[].class );
println( baseResolution );

if( baseResolution != null )
{
	println( downFactors );
	toMicrons = [0.001, 0.001, 0.001]
	
	resolutionNm = [downFactors, baseResolution].transpose().collect{ it[0] * it[1]}
	println( resolutionNm );
	resolution = [toMicrons, resolutionNm].transpose().collect{ it[0] * it[1]}
	println( resolution );
}

img = N5Utils.open( n5 , dataset );
ip = ImageJFunctions.show( img, dataset );


if( duplicate )
{
	ipDup = new Duplicator().run(ip);
	ip.close();

	if( baseResolution != null )
	{
	    ipDup.getCalibration().pixelWidth = resolution[ 0 ];
	    ipDup.getCalibration().pixelHeight = resolution[ 1 ];
	    ipDup.getCalibration().pixelDepth = resolution[ 2 ];
	    ipDup.getCalibration().setUnit( "um" );
	}
	ipDup.setDimensions( 1, (int)img.dimension(2), 1 );
	ipDup.show();
}
else
{
	if( baseResolution != null )
	{
	    ip.getCalibration().pixelWidth = resolution[ 0 ];
	    ip.getCalibration().pixelHeight = resolution[ 1 ];
	    ip.getCalibration().pixelDepth = resolution[ 2 ];
	}
	ip.show();
}
