/*****************************************************************************
 * Copyright (c) Minh Duc Cao, Monash Uni & UQ, All rights reserved.         *
 *                                                                           *
 * Redistribution and use in source and binary forms, with or without        *
 * modification, are permitted provided that the following conditions        *
 * are met:                                                                  * 
 *                                                                           *
 * 1. Redistributions of source code must retain the above copyright notice, *
 *    this list of conditions and the following disclaimer.                  *
 * 2. Redistributions in binary form must reproduce the above copyright      *
 *    notice, this list of conditions and the following disclaimer in the    *
 *    documentation and/or other materials provided with the distribution.   *
 * 3. Neither the names of the institutions nor the names of the contributors*
 *    may be used to endorse or promote products derived from this software  *
 *    without specific prior written permission.                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
 ****************************************************************************/

/*****************************************************************************
 *                           Revision History                                
 * 7 Aug 2015 - Minh Duc Cao: Created                                        
 * 
 ****************************************************************************/
package japsa.tools.bio.np;

import java.io.IOException;

import japsa.bio.np.RealtimeMLST;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;

/**
 * @author minhduc
 *
 */
@Deployable(
	scriptName = "jsa.np.rtMLST", 
	scriptDesc = "Realtime Multi-Locus Strain Typing using Nanopore Sequencing data"
	)
public class RealtimeMLSTCmd extends CommandLine{	
	public RealtimeMLSTCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());

		
		
		addString("mlstScheme", null, "Path to mlst scheme",true);
		addString("bamFile", null,  "The bam file");
		addInt("top", 10,  "The number of top strains");		
		addString("msa", "kalign",
				"Name of the msa method, support poa, kalign, muscle and clustalo");
		addString("tmp", "tmp/t",  "Temporary folder");
		addString("hours", null,  "The file containging hours against yields, if set will output acording to tiime");

		//////////////////////////////////////////////////////////////////////////
		
		addDouble("qual", 0,  "Minimum alignment quality");
		addBoolean("twodonly", false,  "Use only two dimentional reads");		
		addInt("read", 50,  "Minimum number of reads between analyses");		
		addInt("time", 30,   "Minimum number of seconds between analyses");
		
		addStdHelp();		
	} 

	public static void main(String[] args) throws IOException, InterruptedException{
		CommandLine cmdLine = new RealtimeMLSTCmd();
		args = cmdLine.stdParseLine(args);			
		/**********************************************************************/

		
		String mlstDir = cmdLine.getStringVal("mlstScheme");
		String bamFile = cmdLine.getStringVal("bamFile");		
		String msa = cmdLine.getStringVal("msa");
		String tmp = cmdLine.getStringVal("tmp");		
		
				
		int read       = cmdLine.getIntVal("read");
		int time       = cmdLine.getIntVal("time");		
		double qual      = cmdLine.getDoubleVal("qual");		
		boolean twodonly = cmdLine.getBooleanVal("twodonly");
				
		
		int top = 10;
		
		
		RealtimeMLST paTyping = new RealtimeMLST(mlstDir);
		paTyping.setTwoDOnly(twodonly);
		paTyping.setMinQual(qual);
		
		paTyping.msa = msa;
		paTyping.prefix = tmp;		
		
		paTyping.readNumber = read;		


		if (paTyping.readNumber < 1)
			paTyping.readNumber = 1;
		
		paTyping.typing(bamFile,  top);

	}
}
