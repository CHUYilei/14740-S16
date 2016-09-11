

import java.io.*;

public class CgiHandler {
	String url ="";
	static String partpath="";
	public CgiHandler(String partpath, String url){
		this.partpath = partpath;
		this.url = url;
		
		
	}
	public String respond(){
		
		if(url.contains("?")){
			System.err.println("[INFO] enters CGI with ?");
			String[] urlParsed = url.split("[?]"); 
			processurl(urlParsed[0], urlParsed[1]); 
		}else{
			System.err.println("[INFO] enters CGI without ?");
			processurl(url, "");
		}
		return "CGI has finished";
	}
	

	static void processurl(String url, String values){
		System.err.println("[INFO] server enters processUrl");
		String filepath = url.split("[.]")[0]; 
		String[] commandArgs = null; 
		String fileName = null;
		
        String[] args = values.split("&");
        for(int i=0;i<args.length;i++){
			args[i]=args[i].split("=")[1];
		}

		if(url.split("[.]")[1].equals("cgi")){
			fileName = filepath+".class";
			int lastindex = filepath.lastIndexOf('/');
			String newpath = partpath+filepath.substring(0,lastindex);
			String mainclass = filepath.substring(lastindex+1);
			System.err.println("[INFO] newpath: "+newpath);
			System.err.println("[INFO] mainclass: "+mainclass);
			commandArgs = new String[4+args.length];
			commandArgs[0] = "java";
			commandArgs[1] = "-cp";
			commandArgs[2] = newpath;
			commandArgs[3] = mainclass;
			System.arraycopy(args,0,commandArgs,4,args.length);

		}

		File execfile = new File(partpath,fileName);

		if(execfile.exists()){
			System.err.println("[INFO] Path found: "+partpath+fileName+" ,start sum calculation");
			try{
				Process process = Runtime.getRuntime().exec(commandArgs);
				System.err.println("[INFO] process starts calculation");
				for(String arg:commandArgs){
					System.err.println("[INFO] arg: "+arg);
				}

				BufferedReader stdInput = new BufferedReader(new
						InputStreamReader(process.getInputStream()));

				BufferedReader stdError = new BufferedReader(new
						InputStreamReader(process.getErrorStream()));

				// read the output from the command
				System.out.println("Here is the standard output of the command:\n");
				String s = null;
				while ((s = stdInput.readLine()) != null) {
					System.out.println(s);
				}

				// read any errors from the attempted command
				System.out.println("Here is the standard error of the command (if any):\n");
				while ((s = stdError.readLine()) != null) {
					System.out.println(s);
				}

//				BufferedReader in = new BufferedReader( new InputStreamReader( process.getInputStream()));
//
//				String line = in.readLine();
//				if(line == null){
//					System.err.println("[INFO] line is null!!!!");
//				}else{
//					System.err.println("line: "+line);
//				}


//				while ((line = in.readLine()) != null){
//					System.out.println(line);
//				}
				
			}catch (Exception e){ 
				e.printStackTrace(); 
			}
		}else{
			System.err.println("[INFO] Path not found: "+partpath+fileName);
		}
	}

}
