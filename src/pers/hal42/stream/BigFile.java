package pers.hal42.util;


import java.io.*;

// +++ build a listener system that can get notified of progress

public class BigFile {
  private ErrorLogStream dbg = new ErrorLogStream(BigFile.class.getName(), ErrorLogStream.VERBOSE);
	private String path = "";
	private long size = 0;

	/*
	  path - the base path to the file to split or join.
	  size - the size in kb to split the file into.
	*/

	public BigFile(String path, int size) {
		this.path = path;
		this.size = size;
	}

  // constructor for combining
	public BigFile(String path) {
		this.path = path;
	}


	void 	splitfile() {
		File infile = new File( path );

		if( ! infile.exists() ){
			dbg.ERROR("No such file:\n" + path);
			return;
		} else if( ! infile.canRead() ){
			dbg.ERROR("Cannot read file:\n" + path);
			return;
		}

		long filelength = infile.length();

		FileInputStream is = null;
		try{
			is = new FileInputStream( infile );
		}catch(Exception ex){
			dbg.ERROR("File not found:\n" + path);
			return;
		}

		if( filelength < size ){
			dbg.VERBOSE("Splitting unnecessary.\nFile is less than\n" + (size/1024) + " Kbytes long");
			return;
		}else if( filelength == size ){
			dbg.VERBOSE("Splitting unnecessary.\nFile is " + (size/1024) + " Kbytes long");
			return;
		}

		long tot = 0;
		long p = 0;
		while( tot < filelength ) {
      dbg.VERBOSE(statDisp(tot, filelength));
  		long sz = ( ( filelength - tot ) < size ) ? (int)(filelength - tot) : size;
			try{
  			FileOutputStream os = new FileOutputStream( path + "." + p );
        long rd = Streamer.swapStreams(is, os, 100000, sz);
        if(rd < sz) {
          dbg.ERROR("Only " + rd + " bytes transferred instead of the " + sz + " expected!");
        }
  			tot += rd;
				os.close();
			}catch(Exception ex){
				dbg.Caught(ex);
				if( is instanceof FileInputStream ) {
					try{ is.close(); }catch(Exception s){}
				}
				return;
			}
			p++;
		}
    dbg.VERBOSE(statDisp(tot, filelength) + ", " + tot + " bytes, " + p + " files.");
		if( is instanceof FileInputStream ) {
			try{ is.close(); }catch(Exception s){}
		}
		dbg.VERBOSE("Done... split into " + p + " files.\nTotal " + tot + " bytes" );
	}

  private static final String statDisp(long total, long filelength) {
    return "" + Math.round((100 * (total / (double)filelength ))) + " %";
  }

  // +++ make this stream like splitfile() !!!
	void joinfile() {
		String name = "";
		int p = 0;
		int rd = 0;
		int dotindex = 0;
		byte temp[] = null;
		long inner = 0;
		long filelen = 0;
		long oldfilelen = 0;
		long total = 0;
		FileOutputStream os = null;
		FileInputStream is = null;
		File infile = null;
		File test = null;

		dotindex = path.lastIndexOf(".");

		if( dotindex == -1 ){
			dbg.ERROR("Not a valid JavaSplit file.");
			return;
		}

		name = path.substring( 0 , dotindex );

		try{
			Integer.parseInt( path.substring( dotindex + 1, path.length() ));
		}catch(Exception ex){
			dbg.ERROR("Not a valid JavaSplit file.");
			return;
		}

		test = new File( path );

		if( ! test.exists() ){
			dbg.ERROR("No such file:\n" + path );
			return;
		}else if( ! test.canRead() ){
			dbg.ERROR("Cannot read file:\n" + path );
			return;
		}

		test = new File( name );

		if( test.exists() ) {
			dbg.WARNING("The target file already exists.\nPlease move the file " + name + "\nTo another directory." );
			return;
		}

		try{
			os = new FileOutputStream( name );
		}catch(Exception ex){
  		dbg.Caught(ex);
			return;
		}

		while( true ){
			try{
				infile = new File( name + "." + p );
				if( is instanceof FileInputStream ){
					try{ is.close(); }catch(Exception s){}
				}

				is = new FileInputStream( infile );

				p++;
			}catch(Exception ex){
				//setMessage(  "Done... joined " + p + " files" );
				dbg.VERBOSE("Done... joined " + p + " files.\nTotal " + total + " bytes" );
				break;
			}

			filelen = infile.length();

			if( filelen > oldfilelen || temp == null ) {
				temp = new byte[ (int)filelen ];
			}
			oldfilelen = filelen;
			try{
				inner = 0;
				while( inner < filelen ) {
				  	rd = is.read( temp );
				  	total += rd;
					inner += rd;
					os.write( temp , 0 , rd );
					//setMessage(  "Reading file " + p  );
					temp = null;
				}
			}catch(Exception ex){
				dbg.Caught(ex);
				break;
			}
		}

		if( os instanceof FileOutputStream ){
			try{ os.close(); }catch(Exception s){}
		}
	}

  public static final void main(String args[]) {
    ErrorLogStream.cpf.myLevel.setLevel(ErrorLogStream.VERBOSE);
    switch(args.length) {
      default:
      case 0: {
        System.out.println("BigFile filename [size]");
      } break;
      case 1: {
        //only filename means to join
        BigFile fa = new BigFile(args[0]);
        fa.joinfile();
      } break;
      case 2: {
        // filesize means to split
        BigFile fa = new BigFile(args[0], Integer.parseInt(args[1]));
        fa.splitfile();
      } break;
    }
  }

}
