#@ File (label="N5 root",  style="directory") n5Root
#@ String (label="dataset") dataset

/**
 * Visualize a multiscale n5 dataset with bigdataviewer 
 */

import bdv.util.*;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.*;

import net.imglib2.util.*;
import net.imglib2.realtransform.*;

import mpicbg.spim.data.sequence.*;

mipmaps = N5Utils.openMipmaps(new N5FSReader(n5Root.getAbsolutePath()), dataset, true );
type = Util.getTypeFromInterval( mipmaps.getA()[0] );

src = new RandomAccessibleIntervalMipmapSource( mipmaps.getA(), type, mipmaps.getB(),
	new FinalVoxelDimensions("pix", 1, 1, 1 ),
	new AffineTransform3D(), dataset );

bdv = BdvFunctions.show( src );
