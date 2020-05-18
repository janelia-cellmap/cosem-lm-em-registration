package org.janelia.saalfeldlab.view;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Util;

public class N5S3View
{

	public static void main( String[] args ) throws IOException
	{
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

        String bucketName = "https://janelia-cosem-datasets-dev.s3.amazonaws.com/jrc_fly-fsb-1/jrc_fly-fsb-1.n5/";
        N5AmazonS3Reader n5 = new N5AmazonS3Reader(s3, bucketName );

        RandomAccessibleInterval<?> img = N5Utils.open( n5, "/fibsem/aligned" );

        System.out.println( "img: " + img );
        System.out.println( " nd: " + img.numDimensions());
        System.out.println( " sz: " + Util.printInterval( img ));

	}

}
