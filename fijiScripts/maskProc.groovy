#@ ImagePlus (label="img") imgPlus

import ij.*;
import ij.plugin.*;

def cropAndMask( img )
{
	roi = img.getRoi();
	bbox = roi.getBounds(); // bounding box in pixel units

	ipDup = new Duplicator().run(img);
	ipDup.getCalibration().xOrigin = bbox.getX();
	ipDup.getCalibration().yOrigin = bbox.getY();
	ipDup.getCalibration().zOrigin = 0.0;

	ipDup.show();
	

	maskProc = img.createRoiMask();
	println( maskProc );

	// make a stack from the mask
	stack = new ImageStack();
	(0..<ipDup.getNSlices()).collect{ stack.addSlice( maskProc ) }
	maskIp = new ImagePlus( img.getTitle() + "_mask", stack );
	maskIp.show();

	// crop it according to the roi
	maskIp.setRoi( roi );
	mask = new Duplicator().run( maskIp );
	mask.show();
	// and set metadata
	mask.getCalibration().pixelWidth = ipDup.getCalibration().pixelWidth;
	mask.getCalibration().pixelHeight = ipDup.getCalibration().pixelHeight;
	mask.getCalibration().pixelDepth = ipDup.getCalibration().pixelDepth;
	
	mask.getCalibration().xOrigin = bbox.getX();
	mask.getCalibration().yOrigin = bbox.getY();
	mask.getCalibration().zOrigin = 0.0;

	// close old mask
	maskIp.close();
	
}

cropAndMask( imgPlus );
