#@ File(label="base folder", style="directory") baseDir
#@ String(label="dir pattern 1") dirPattern1
#@ String(label="dir pattern 2") dirPattern2
#@ String(label="file pattern") filePatternIn

/* 
 * Opens all images matching the given directory patterns,
 * where patterns are of the form:
 * 	  baseDir/dirPattern1/dirPattern2/filePatternIn
 */

// exp*
// result_*
// *nrrd

import ij.IJ;
import java.io.*;

import org.apache.commons.io.filefilter.WildcardFileFilter;

dirFilter1 = new WildcardFileFilter( dirPattern1 );
dirFilter2 = new WildcardFileFilter( dirPattern2 );
fileFilter = new WildcardFileFilter( filePatternIn );

fileList = []
for (d in baseDir.listFiles( dirFilter1 as java.io.FilenameFilter ))
{
	if( d.isDirectory() )
	{
		println( d );
		
        for ( resdir in d.listFiles( dirFilter2 as java.io.FilenameFilter ))
        {
            if( resdir.isDirectory())
             {
                ff = resdir.list( fileFilter as java.io.FilenameFilter );
                def prefix = ( resdir.getCanonicalPath() =~ /(exp\d+)_/ )[0][0] as String

                println ( ff )
                if( ff.length == 0 ){ continue; }
                
                totalFile = new File( resdir, ff[0]);
                fileList += totalFile;

                println( totalFile );
                println( prefix );
                
                imp = IJ.openImage( totalFile.getCanonicalPath() );
                imp.setTitle( prefix + imp.getTitle())
                imp.show();
            }
        }
	}
}

