#@ ImagePlus (label="img") imgPlus
#@ File (label="working directory",  style="directory") workingDirectory
#@ String (label="name") name
#@ Boolean (label="write") doWrite


import ij.*;
import ij.plugin.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

def writeTranslation2D( file, tx, ty, rx, ry )
{
	s = String.format( "1.0 0.0 0.0 %f\n0.0 1.0 0.0 %f\n0.0 0.0 1.0 0.0\n0.0 0.0 0.0 1.0", rx*tx, ry*ty );
	println( s );
    Files.write( Paths.get( file ), 
        s.getBytes(),
        StandardOpenOption.CREATE )
}

def cropAndMask( img, baseDir, doWrite )
{
	roi = img.getRoi();
	bbox = roi.getBounds(); // bounding box in pixel units

	ipDup = new Duplicator().run(img);
	ipDup.getCalibration().xOrigin = bbox.getX();
	ipDup.getCalibration().yOrigin = bbox.getY();
	ipDup.getCalibration().zOrigin = 0.0;

	ipDup.setTitle( imgPlus.getShortTitle() + "_" + name + ".nrrd" );
	ipDup.show();

	maskProc = img.createRoiMask();
	println( maskProc );

	// make a stack from the mask
	stack = new ImageStack();
	(0..<ipDup.getNSlices()).collect{ stack.addSlice( maskProc ) }
	maskIp = new ImagePlus( ipDup.getShortTitle() + "_mask.nrrd", stack );
	maskIp.show();


	// crop it according to the roi
	maskIp.setRoi( roi );
	mask = new Duplicator().run( maskIp );

    rx = ipDup.getCalibration().pixelWidth;
    ry = ipDup.getCalibration().pixelHeight;
	
	// and set metadata
	mask.getCalibration().pixelWidth = rx;
	mask.getCalibration().pixelHeight = ry;
	mask.getCalibration().pixelDepth = ipDup.getCalibration().pixelDepth;
	
	mask.getCalibration().xOrigin = bbox.getX();
	mask.getCalibration().yOrigin = bbox.getY();
	mask.getCalibration().zOrigin = 0.0;

    IJ.run( mask, "Divide...", "value=255 stack");

    ipDupMasked = new ImageCalculator().run("Multiply create stack", ipDup, mask )
	ipDupMasked.getCalibration().pixelWidth = rx;
	ipDupMasked.getCalibration().pixelHeight = ry;
	ipDupMasked.getCalibration().pixelDepth = ipDup.getCalibration().pixelDepth;

	ipDupMasked.getCalibration().xOrigin = bbox.getX();
	ipDupMasked.getCalibration().yOrigin = bbox.getY();
	ipDupMasked.getCalibration().zOrigin = 0.0;

	mask.show();

    // multiply dup image by mask

	

	if( doWrite )
	{
		println("writing to files");
		
		outputImgF = baseDir.getAbsolutePath() + File.separator + ipDup.getTitle();
		maskImgF = baseDir.getAbsolutePath() + File.separator + mask.getTitle();
		translationF = baseDir.getAbsolutePath() + File.separator + mask.getTitle() + "_xlationMaskToOrigPhysical.mat";

		// TODO consider checking for overwriting files here
		println( outputImgF );
		println( maskImgF );
		println( translationF );

		writeTranslation2D( translationF, bbox.getX(), bbox.getY(), rx, ry);

		IJ.run( ipDupMasked, "Nrrd ... ", "nrrd="+outputImgF);
		IJ.run(  mask, "Nrrd ... ", "nrrd="+maskImgF);
	}

	// close old mask
	maskIp.close();
}

def createAndCheckOutputDir( File workingDirectory, String name )
{
	if( workingDirectory.isDirectory() )
	{
		File outdir = new File( workingDirectory, name );

		if( outdir.exists() && outdir.isDirectory() )
		{
			println( "the folder exists: " + outdir );
			files = outdir.list();
			if( files.length > 0 )
			{
				println( "   but it has contents " );
				// TODO consider warning more strongly here.
				return outdir;
			}
			else
			{
				println( "   and it's empty " );
				return outdir;
			}
		}
		else
		{
			println( "making the folder: " + outdir );
			outdir.mkdir();
			return outdir;
		}
	}
	return null;
}

dir = createAndCheckOutputDir( workingDirectory, name );
println( dir );
if( dir == null )
{
	println( "The name and working directory already exist and may have old results.  Skipping." )
	return;
}
println( "cropAndMask" );
cropAndMask( imgPlus, dir, doWrite ); 
