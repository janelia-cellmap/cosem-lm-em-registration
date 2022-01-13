
// edit the two lists below
n5Roots = [
	"A:\\some\\path\\to\\your.n5",
	"G:\\yet\\another.n5",
	"A:\\fastest\\storage\\in\\the\\west.n5",
]

n5Datasets = [ 
	"/volumes/raw",
]

// be fancy about adding to your list when things are repetative
n5Datasets += ["/nucleus"] * 2


// ignore everything below this
 
import java.util.*;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.bdv.*;
import org.janelia.saalfeldlab.n5.ij.*;
import org.janelia.saalfeldlab.n5.metadata.*;
import org.janelia.saalfeldlab.n5.metadata.canonical.*;
import org.janelia.saalfeldlab.n5.ui.*;
import org.janelia.saalfeldlab.n5.imglib2.*;
 
import bdv.BigDataViewer;
import bdv.cache.CacheControl;
import bdv.export.ProgressWriterConsole;
import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.*;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util

 
 
 
n5s = n5Roots.collect { new N5Factory().openReader(it)}
 
metadataList = []
i = 0;
for ( d in n5Datasets )
{
	metadataList += [ parseMetadata( n5s[i], d )]
	i++;
}

 		
def parseMetadata( n5, dataset )
{
	discoverer = new N5DatasetDiscoverer( n5,
		Executors.newFixedThreadPool( ij.Prefs.getThreads() ),
		Arrays.asList(N5Importer.PARSERS),
		Arrays.asList(N5Importer.GROUP_PARSERS)  );
 
 
	m = discoverer.parse( dataset );
	if( m.getMetadata() != null )
		return m.getMetadata();
	
 
	m = discoverer.discoverAndParseRecursive( dataset );
	if( m.getMetadata() != null )
		return m.getMetadata();
}
 
def addSourceToListsARGBType(
		final Source source,
		final int setupId,
		final List converterSetups,
		final List sources )
{
	final TransformedSource ts = new TransformedSource( source );
	final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );
	final SourceAndConverter soc = new SourceAndConverter( ts, converter );
 
	final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );
 
	converterSetups.add( setup );
	sources.add( soc );
}
 
def addSourceToListsVolatileARGBType(
		final Source source,
		final int setupId,
		final List converterSetups,
		final List sources )
{
	final TransformedSource ts = new TransformedSource( source );
	final ScaledARGBConverter.VolatileARGB converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
	final SourceAndConverter soc = new SourceAndConverter( ts, converter );
 
	final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );
 
	converterSetups.add( setup );
	sources.add( soc );
}
 
def addSourceToListsRealType(
		final Source source,
		final int setupId,
		final List converterSetups,
		final List sources )
{
	type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
	
	final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
	final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
	final RealARGBColorConverter converter = RealARGBColorConverter.create( source.getType(), typeMin, typeMax );
 
	c = new ARGBType( ARGBType.rgba(1.0,1.0,1.0,1.0) );
 
	//converter.setColor( c );
 
	final TransformedSource ts = new TransformedSource( source );
	final SourceAndConverter soc = new SourceAndConverter( ts, converter );
	final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );
 
	converterSetups.add( setup );
	sources.add( soc );
}
 
def addSourceToListsGenericType(
		final Source source,
		final int setupId,
		final int numTimepoints,
		final Object type,
		final List converterSetups,
		final List sources )
{
	if ( type instanceof RealType )
		addSourceToListsRealType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
	else if ( type instanceof ARGBType )
		addSourceToListsARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
	else if ( type instanceof VolatileARGBType )
		addSourceToListsVolatileARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
	else
		throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
}
 
def exec( n5s, metadataList )
{
		int numSources = n5Datasets.size();
		int numTimepoints = 1;
		Prefs.showScaleBar( true );
 
		final SharedQueue sharedQueue = new SharedQueue( (int)Math.ceil( Runtime.getRuntime().availableProcessors() / 2 ) );
 
		final ArrayList converterSetups = new ArrayList();
		final ArrayList sourcesAndConverters = new ArrayList();
 
		final List sources = new ArrayList();
		final List volatileSources = new ArrayList();
		final List additionalSources = new ArrayList();
 
		N5Reader n5;
		for ( int i = 0; i < numSources; ++i )
		{
			n5 = n5s[i]
			
			String[] datasetsToOpen = []
			AffineTransform3D[] transforms = []
 
			N5Metadata metadata = metadataList.get(i);
			if (metadata instanceof N5SingleScaleMetadata) {
				final N5SingleScaleMetadata singleScaleDataset = (N5SingleScaleMetadata) metadata;
				String[] tmpDatasets= new String[]{ singleScaleDataset.getPath() };
				AffineTransform3D[] tmpTransforms = new AffineTransform3D[]{ singleScaleDataset.spatialTransform3d() };

				MultiscaleDatasets msd = MultiscaleDatasets.sort( tmpDatasets, tmpTransforms );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			} else if (metadata instanceof N5MultiScaleMetadata) {
				final N5MultiScaleMetadata multiScaleDataset = (N5MultiScaleMetadata) metadata;
				datasetsToOpen = multiScaleDataset.getPaths();
				transforms = multiScaleDataset.spatialTransforms3d();
			} else if (metadata instanceof N5CosemMetadata ) {
				final N5CosemMetadata singleScaleCosemDataset = (N5CosemMetadata) metadata;
				datasetsToOpen = new String[]{ singleScaleCosemDataset.getPath() };
				transforms = new AffineTransform3D[]{ singleScaleCosemDataset.spatialTransform3d() };
			} else if (metadata instanceof CanonicalSpatialMetadata ) {
				final CanonicalSpatialMetadata canonicalDataset = (CanonicalSpatialMetadata) metadata;
				datasetsToOpen = new String[]{ canonicalDataset.getPath() };
				transforms = new AffineTransform3D[]{ canonicalDataset.getSpatialTransform().spatialTransform3d() };
			} else if (metadata instanceof N5CosemMultiScaleMetadata ) {
				final N5CosemMultiScaleMetadata multiScaleDataset = (N5CosemMultiScaleMetadata) metadata;
				MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			} else if (metadata instanceof CanonicalMultiscaleMetadata ) {
				final CanonicalMultiscaleMetadata multiScaleDataset = (CanonicalMultiscaleMetadata) metadata;
				MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			}
			else if( metadata instanceof N5DatasetMetadata ) {
				final List addTheseSources = MetadataSource.buildMetadataSources(n5, (N5DatasetMetadata)metadata);
				if( addTheseSources != null )
					additionalSources.addAll(addTheseSources);
			}
			else {
				datasetsToOpen = new String[]{ metadata.getPath() };
				transforms = new AffineTransform3D[] { new AffineTransform3D() };
			}


			if( datasetsToOpen == null || datasetsToOpen.length == 0 )
				continue;

			// is2D should be true at the end of this loop if all sources are 2D
			is2D = true;

			RandomAccessibleInterval[] images = new RandomAccessibleInterval[datasetsToOpen.length];
			for ( int s = 0; s < images.length; ++s )
			{
				CachedCellImg vimg = N5Utils.openVolatile( n5, datasetsToOpen[s] );
				if( vimg.numDimensions() == 2 )
				{
					images[ s ] = Views.addDimension(vimg, 0, 0);
					is2D = is2D && true;
				}
				else
				{
					images[ s ] = vimg;
					is2D = is2D && false;
				}
			}

			N5Source source = new N5Source(
					Util.getTypeFromInterval(images[0]),
					"source " + (i + 1),
					images,
					transforms);

			N5VolatileSource volatileSource = source.asVolatile(sharedQueue);

			sources.add(source);
			volatileSources.add(volatileSource);

			addSourceToListsGenericType( volatileSource, i + 1, numTimepoints, volatileSource.getType(), converterSetups, sourcesAndConverters );
		}

		for( MetadataSource src : additionalSources ) {
			if( src.numTimePoints() > numTimepoints )
				numTimepoints = src.numTimePoints();

			addSourceToListsGenericType( src, i + 1, src.numTimePoints(), src.getType(), converterSetups, sourcesAndConverters );
		}
 
		BigDataViewer bdv = new BigDataViewer(
				converterSetups,
				sourcesAndConverters,
				null,
				numTimepoints,
				new CacheControl.CacheControls(),
				"N5 Viewer",
				new ProgressWriterConsole(),
				BdvOptions.options().values.getViewerOptions()
			);
 
		InitializeViewerState.initTransform( bdv.getViewer() );
 
		bdv.getViewer().setDisplayMode( DisplayMode.FUSED );
		bdv.getViewerFrame().setVisible( true );
 
}
 
exec( n5s, metadataList )

